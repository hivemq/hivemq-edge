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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModuleLoader {
    private static final Logger log = LoggerFactory.getLogger(ModuleLoader.class);

    private final @NotNull SystemInformation systemInformation;
    protected final @NotNull Set<EdgeModule> modules = new HashSet<>();
    protected final @NotNull Comparator<File> fileComparator = (o1, o2) -> {
        final long delta = o2.lastModified() - o1.lastModified();
        // we cna easily get an overflow within months, so we can not use the delta directly by casting it to integer!
        if (delta == 0) {
            return 0;
        } else if (delta < 0) {
            return -1;
        } else {
            return 1;
        }
    };

    private final @NotNull ClassServiceLoader classServiceLoader = new ClassServiceLoader();
    private final AtomicBoolean loaded = new AtomicBoolean();

    @Inject
    public ModuleLoader(final @NotNull SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }

    public void loadModules() {
        if (loaded.get()) {
            // avoid duplicate loads
            return;
        }
        loaded.set(true);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (Boolean.getBoolean(HiveMQEdgeConstants.DEVELOPMENT_MODE)) {
            log.info(String.format("Welcome '%s' is starting...", "48 69 76 65 4D 51  45 64 67 65"));
            log.warn(
                    "\n################################################################################################################\n" +
                            "# You are running HiveMQ Edge in Development Mode and Modules will be loaded from your workspace NOT your      #\n" +
                            "# HIVEMQ_HOME/modules directory. To load runtime modules from your HOME directory please remove                #\n" +
                            "#  '-Dhivemq.edge.workspace.modules=true' from your startup script                                             #\n" +
                            "################################################################################################################");
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

    private void loadCommercialModuleLoaderFromWorkSpace(final @NotNull ClassLoader contextClassLoader) {
        final File userDir = new File(System.getProperty("user.dir"));
        final File commercialModulesRepoRootFolder = new File(userDir, "../hivemq-edge-commercial-modules");
        if (!commercialModulesRepoRootFolder.exists()) {
            log.error("Can not load commercial modules from workspace as the assumed root folder '{}' does not exist.",
                    commercialModulesRepoRootFolder.getAbsolutePath());
            return;
        }


        final File commercialModuleLoaderLibFolder =
                new File(commercialModulesRepoRootFolder, "hivemq-edge-commercial-modules-loader/build/libs");
        if (!commercialModuleLoaderLibFolder.exists()) {
            log.error("Could not load commercial module loader as the assumed lib folder '{}' does not exist.",
                    commercialModuleLoaderLibFolder.getAbsolutePath());
            return;
        }

        final File[] tmp = commercialModuleLoaderLibFolder.listFiles(file -> file.getName().endsWith("proguarded.jar"));

        if (tmp == null || tmp.length == 0) {
            log.info("No commercial module loader jar was discovered in libs folder '{}'",
                    commercialModuleLoaderLibFolder);
            return;
        }

        final List<File> potentialCommercialModuleJars =
                new ArrayList<>(Arrays.stream(tmp).sorted(fileComparator).toList());

        final String absolutePathJar = potentialCommercialModuleJars.get(0).getAbsolutePath();
        if (potentialCommercialModuleJars.size() > 1) {
            log.debug(
                    "More than one commercial module loader jar was discovered in libs folder '{}'. Clean unwanted jars to avoid loading the wrong version. The first found jar '{}' will be loaded.",
                    commercialModuleLoaderLibFolder,
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
        final IsolatedModuleClassloader isolatedClassloader =
                new IsolatedModuleClassloader(urls.toArray(new URL[0]), parentClassloader);
        modules.add(new ModuleLoader.EdgeModule(jarFile, isolatedClassloader, false));
    }

    protected void loadFromWorkspace(final @NotNull ClassLoader parentClassloader) {
        log.debug("Loading modules from development workspace.");
        final File userDir = new File(System.getProperty("user.dir"));
        loadFromWorkspace(parentClassloader, userDir);
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
    protected void loadFromWorkspace(final @NotNull ClassLoader parentClassloader, final @NotNull File currentDir) {
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

    protected void discoverWorkspaceModule(final File dir, final @NotNull ClassLoader parentClassloader) {
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
                    final IsolatedModuleClassloader isolatedClassloader =
                            new IsolatedModuleClassloader(urls.toArray(new URL[0]), parentClassloader);
                    modules.add(new EdgeModule(file, isolatedClassloader, true));
                } catch (final IOException ioException) {
                    log.warn("Exception with reason {} while reading module file {}",
                            ioException.getMessage(),
                            file.getAbsolutePath());
                    log.debug("Original exception", ioException);
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
                    log.warn("Exception with reason {} while reading module file {}",
                            ioException.getMessage(),
                            lib.getAbsolutePath());
                    log.debug("Original exception", ioException);
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
        return modules;
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
