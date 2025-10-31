/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.modules;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.modules.adapters.impl.IsolatedModuleClassloader;
import com.hivemq.extensions.loader.ClassServiceLoader;
import com.hivemq.http.handlers.AlternativeClassloadingStaticFileHandler;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModuleLoader {
    protected static final @NotNull Comparator<File> fileComparator = (o1, o2) -> {
        final long delta = o2.lastModified() - o1.lastModified();
        return delta == 0 ? 0 : delta < 0 ? -1 : 1;
    };
    private static final @NotNull Logger log = LoggerFactory.getLogger(ModuleLoader.class);
    protected final @NotNull Set<EdgeModule> modules;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull ClassServiceLoader classServiceLoader;
    private final @NotNull AtomicBoolean loaded;

    @Inject
    public ModuleLoader(final @NotNull SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
        this.classServiceLoader = new ClassServiceLoader();
        this.loaded = new AtomicBoolean(false);
        this.modules = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    private static void logException(final @NotNull File file, final @NotNull IOException ioException) {
        log.warn("Exception with reason {} while reading module file {}",
                ioException.getMessage(),
                file.getAbsolutePath());
        log.debug("Original exception", ioException);
    }

    public void loadModules() {
        if (loaded.compareAndSet(false, true)) {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (Boolean.getBoolean(HiveMQEdgeConstants.DEVELOPMENT_MODE)) {
                log.info(String.format("Welcome '%s' is starting...", "48 69 76 65 4D 51  45 64 67 65"));
                log.warn("""
                        
                        ################################################################################################################
                        # You are running HiveMQ Edge in Development Mode and Modules will be loaded from your workspace NOT your      #
                        # HIVEMQ_HOME/modules directory. To load runtime modules from your HOME directory please remove                #
                        #  '-Dhivemq.edge.workspace.modules=true' from your startup script                                             #
                        ################################################################################################################""");
                loadFromWorkspace(contextClassLoader);
                // load the commercial module loader from the workspace folder
                // the loadFromWorkspace() will not find it.
                log.info("Loading the commercial module loader from workspace.");
                loadCommercialModuleLoaderFromWorkSpace(contextClassLoader);
            } else {
                // the commercial module loader will be found here in case of a "normal" running hivemq edge
                loadFromModulesDirectory(contextClassLoader);
            }
        }
    }

    private void loadCommercialModuleLoaderFromWorkSpace(final @NotNull ClassLoader contextClassLoader) {
        final File userDir = new File(System.getProperty("user.dir"));
        final File commercialModulesRepoRootFolder = new File(userDir, "../hivemq-edge-commercial-modules");
        if (!commercialModulesRepoRootFolder.exists()) {
            log.error("Can not load commercial modules from workspace as the assumed root folder '{}' does not exist.",
                    commercialModulesRepoRootFolder.getAbsolutePath());
            return;
        }

        final File libs = new File(commercialModulesRepoRootFolder, "hivemq-edge-commercial-modules-loader/build/libs");
        if (!libs.exists()) {
            log.error("Could not load commercial module loader as the assumed lib folder '{}' does not exist.",
                    libs.getAbsolutePath());
            return;
        }
        final File[] tmp = libs.listFiles(file -> file.getName().endsWith("proguarded.jar"));
        if (tmp == null || tmp.length == 0) {
            log.info("No commercial module loader jar was discovered in libs folder '{}'", libs);
            return;
        }

        final List<File> jars = new ArrayList<>(Arrays.stream(tmp).sorted(fileComparator).toList());
        final String absolutePathJar = jars.get(0).getAbsolutePath();
        if (jars.size() > 1) {
            log.debug(
                    "More than one commercial module loader jar was discovered in libs folder '{}'. Clean unwanted jars to avoid loading the wrong version. The first found jar '{}' will be loaded.",
                    libs,
                    absolutePathJar);
        } else {
            log.info("Commercial Module jar '{}' was discovered.", absolutePathJar);
        }
        loadCommercialModulesLoaderJar(new File(absolutePathJar), contextClassLoader);
    }

    private void loadCommercialModulesLoaderJar(final File jarFile, final @NotNull ClassLoader parentClassloader) {
        final List<URL> urls = new ArrayList<>();
        try {
            urls.add(jarFile.toURI().toURL());
        } catch (final MalformedURLException e) {
            log.error("", e);
        }
        log.info("Loading commercial module loader from {}", jarFile.getAbsoluteFile());
        modules.add(new ModuleLoader.EdgeModule(jarFile,
                new IsolatedModuleClassloader(urls.toArray(new URL[0]), parentClassloader),
                false));
    }

    protected void loadFromWorkspace(final @NotNull ClassLoader parentClassloader) {
        log.debug("Loading modules from development workspace.");
        loadFromWorkspace(parentClassloader, new File(System.getProperty("user.dir")));
    }

    /**
     * Load from workspace recursively from the current dir and its parent dir by looking for
     * folder name matching 'hivemq-edge' or 'hivemq-edge-composite'.
     * <p>
     * This allows modules to be loaded from any subprojects when dev mode is turned on.
     *
     * @param parentClassloader the parent classloader
     * @param currentDir        the current dir
     */
    private void loadFromWorkspace(final @NotNull ClassLoader parentClassloader, final @NotNull File currentDir) {
        if (currentDir.exists() && currentDir.isDirectory()) {
            if (currentDir.getName().equals("hivemq-edge")) {
                discoverWorkspaceModule(new File(currentDir, "modules"), parentClassloader);
            } else if (currentDir.getName().equals("hivemq-edge-composite")) {
                discoverWorkspaceModule(new File(currentDir, "../hivemq-edge/modules"), parentClassloader);
            } else {
                final @Nullable File parentFile = currentDir.getParentFile();
                if (parentFile != null) {
                    loadFromWorkspace(parentClassloader, parentFile);
                }
            }
        }
    }

    protected void discoverWorkspaceModule(final @NotNull File dir, final @NotNull ClassLoader parentClassloader) {
        if (dir.exists()) {
            final File[] files = dir.listFiles(pathname -> pathname.isDirectory() &&
                    pathname.canRead() &&
                    new File(pathname, "build").exists());
            if (files == null) {
                log.warn("No potential files discovered in dir '{}'", dir);
                return;
            }

            for (final File file : files) {
                log.info("Found module workspace directory {}.", file.getAbsolutePath());
                try {
                    final List<URL> urls = new ArrayList<>();
                    urls.add(new File(file, "build/classes/java/main").toURI().toURL());
                    urls.add(new File(file, "build/resources/main").toURI().toURL());
                    final File deps = new File(file, "build/deps/libs");
                    if (deps.exists()) {
                        final File[] jars = deps.listFiles(pathname -> pathname.getName().endsWith(".jar"));
                        for (final File jar : jars) {
                            urls.add(jar.toURI().toURL());
                        }
                    }
                    modules.add(new EdgeModule(file,
                            new IsolatedModuleClassloader(urls.toArray(new URL[0]), parentClassloader),
                            true));
                } catch (final IOException ioException) {
                    logException(file, ioException);
                }
            }
        }
    }

    protected void loadFromModulesDirectory(final @NotNull ClassLoader parentClassloader) {
        log.debug("Loading modules from HiveMQ Home.");
        final File modulesFolder = systemInformation.getModulesFolder();
        final File[] libs = modulesFolder.listFiles();
        if (libs != null) {
            for (final File lib : libs) {
                try {
                    if (lib.getName().endsWith(".jar")) {
                        log.debug("Found module jar in modules lib {}.", lib.getAbsolutePath());
                        final IsolatedModuleClassloader isolatedClassloader =
                                new IsolatedModuleClassloader(new URL[]{lib.toURI().toURL()}, parentClassloader);
                        modules.add(new EdgeModule(lib, isolatedClassloader, true));
                    } else {
                        log.debug("Ignoring non jar file in module folder {}.", lib.getAbsolutePath());
                    }
                } catch (final IOException ioException) {
                    logException(lib, ioException);
                }
            }
        }
    }

    public <T> @NotNull List<Class<? extends T>> findImplementations(final @NotNull Class<T> serviceClazz) {
        final ArrayList<Class<? extends T>> classes = new ArrayList<>();
        for (final EdgeModule module : modules) {
            log.trace("Looking for implementations of class '{}' in module '{}'", serviceClazz, module.root);
            try {
                log.trace("Trying to load class '{}' from file '{}'.", serviceClazz, module.root.getAbsoluteFile());
                final Iterable<Class<? extends T>> loaded = classServiceLoader.load(serviceClazz, module.classloader);
                for (final Class<? extends T> foundClass : loaded) {
                    classes.add(foundClass);
                    log.trace("Found implementation '{}' of class '{}' in module '{}'",
                            foundClass,
                            serviceClazz,
                            module.root);
                }
            } catch (final IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return classes;
    }

    public @NotNull Set<EdgeModule> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    public void clear() {
        modules.clear();
    }

    public static class EdgeModule {
        private final @NotNull File root;
        private final @NotNull ClassLoader classloader;

        public EdgeModule(
                final @NotNull File root,
                final @NotNull ClassLoader classloader,
                final boolean registerStaticResources) {
            this.classloader = classloader;
            this.root = root;
            if (registerStaticResources) {
                AlternativeClassloadingStaticFileHandler.addClassLoader(classloader);
            }
        }

        public @NotNull ClassLoader getClassloader() {
            return classloader;
        }

        public @NotNull File getRoot() {
            return root;
        }

        @Override
        public @NotNull String toString() {
            return "EdgeModule{" + "root=" + root + '}';
        }
    }
}
