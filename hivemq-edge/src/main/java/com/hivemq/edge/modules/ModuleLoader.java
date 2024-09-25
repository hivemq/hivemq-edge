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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.loader.ClassServiceLoader;
import com.hivemq.http.handlers.AlternativeClassloadingStaticFileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleLoader {
    private static final Logger log = LoggerFactory.getLogger(ModuleLoader.class);

    private final @NotNull SystemInformation systemInformation;
    protected final @NotNull Set<EdgeModule> modules = new HashSet<>();
    private final @NotNull ClassServiceLoader classServiceLoader = new ClassServiceLoader();

    public ModuleLoader(final @NotNull SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }

    @Inject
    public void loadModules() {
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

    private void loadCommercialModuleLoaderFromWorkSpace(ClassLoader contextClassLoader) {
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

        final File[] potentialCommercialModuleJars =
                commercialModuleLoaderLibFolder.listFiles(file -> file.getName().endsWith("proguarded.jar"));
        if (potentialCommercialModuleJars == null || potentialCommercialModuleJars.length == 0) {
            log.info("No commercial module loader jar was discovered in libs folder '{}'",
                    commercialModuleLoaderLibFolder);
        } else if (potentialCommercialModuleJars.length > 1) {
            final String absolutePathJar = potentialCommercialModuleJars[0].getAbsolutePath();
            log.warn(
                    "More than one commercial module loader jar was discovered in libs folder '{}'. Clean unwanted jars to avoid loading the wrong version. The first found jar '{}' will be loaded.",
                    commercialModuleLoaderLibFolder,
                    absolutePathJar);
            loadCommercialModulesLoaderJar(new File(absolutePathJar), contextClassLoader);
        } else {
            final String absolutePathJar = potentialCommercialModuleJars[0].getAbsolutePath();
            log.info("Commercial Module jar '{}' was discovered.", absolutePathJar);
            loadCommercialModulesLoaderJar(new File(absolutePathJar), contextClassLoader);
        }
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

    protected void loadFromWorkspace(final ClassLoader parentClassloader) {
        log.debug("Loading modules from development workspace.");
        File userDir = new File(System.getProperty("user.dir"));
        if (userDir.getName().equals("hivemq-edge")) {
            discoverWorkspaceModule(new File(userDir, "modules"), parentClassloader);
        } else if (userDir.getName().equals("hivemq-edge-composite")) {
            discoverWorkspaceModule(new File(userDir, "../hivemq-edge/modules"), parentClassloader);
        }
    }

    protected void discoverWorkspaceModule(final File dir, final ClassLoader parentClassloader) {
        if (dir.exists()) {
            File[] files = dir.listFiles(pathname -> pathname.isDirectory() &&
                    pathname.canRead() &&
                    new File(pathname, "build").exists());
            for (int i = 0; i < files.length; i++) {
                log.info("Found module workspace directory {}.", files[i].getAbsolutePath());
                try {
                    List<URL> urls = new ArrayList<>();
                    urls.add(new File(files[i], "build/classes/java/main").toURI().toURL());
                    urls.add(new File(files[i], "build/resources/main").toURI().toURL());
                    File deps = new File(files[i], "build/deps/libs");
                    if (deps.exists()) {
                        File[] jars = deps.listFiles(pathname -> pathname.getName().endsWith(".jar"));
                        for (File jar : jars) {
                            urls.add(jar.toURI().toURL());
                        }
                    }
                    final IsolatedModuleClassloader isolatedClassloader =
                            new IsolatedModuleClassloader(urls.toArray(new URL[0]), parentClassloader);
                    modules.add(new EdgeModule(files[i], isolatedClassloader, true));
                } catch (final IOException ioException) {
                    log.warn("Exception with reason {} while reading module file {}",
                            ioException.getMessage(),
                            files[i].getAbsolutePath());
                    log.debug("Original exception", ioException);
                }
            }
        }
    }

    protected void loadFromModulesDirectory(final ClassLoader parentClassloader) {
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
            } catch (IOException | ClassNotFoundException e) {
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

        private File root;
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

        public File getRoot() {
            return root;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("EdgeModule{");
            sb.append("root=").append(root);
            sb.append('}');
            return sb.toString();
        }
    }
}
