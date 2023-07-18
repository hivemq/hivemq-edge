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
import com.hivemq.edge.modules.adapters.impl.IsolatedModuleClassloader;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.loader.ClassServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class ModuleLoader {
    private static final Logger log = LoggerFactory.getLogger(ModuleLoader.class);

    private final @NotNull SystemInformation systemInformation;

    private final @NotNull Set<EdgeModule> modules = new HashSet<>();
    private final @NotNull ClassServiceLoader classServiceLoader = new ClassServiceLoader();

    @Inject
    public ModuleLoader(final @NotNull SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }

    @Inject
    void loadModules() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        loadFromModulesDirectory(contextClassLoader);
        //TODO - need to load third party deps
//        loadFromWorkspace(contextClassLoader);
    }

    private void loadFromWorkspace(final ClassLoader parentClassloader){
        File userDir = new File(System.getProperty("user.dir"));
        if(userDir.getName().equals("hivemq-edge")){
            discoverWorkspaceModule(new File(userDir, "modules"), parentClassloader);
        } else if(userDir.getName().equals("hivemq-edge-composite")){
            discoverWorkspaceModule(new File(userDir, "../hivemq-edge/modules"), parentClassloader);
        }
    }

    private void discoverWorkspaceModule(final File dir, final ClassLoader parentClassloader) {
        if(dir.exists()){
            File[] files = dir.listFiles(pathname -> pathname.isDirectory() && pathname.canRead()
                     && new File(pathname,"build").exists());
            for (int i = 0; i < files.length; i++){
                log.debug("Found module workspace directory {}.", files[i].getAbsolutePath());
                try {
                    final IsolatedModuleClassloader isolatedClassloader =
                            new IsolatedModuleClassloader(new URL[]{
                                    new File(files[i], "build/classes/java/main").toURI().toURL(),
                                    new File(files[i], "build/resources/main").toURI().toURL()}, parentClassloader);
                    modules.add(new EdgeModule(files[i], isolatedClassloader));
                } catch (final IOException ioException) {
                    log.warn("Exception with reason {} while reading module file {}",
                            ioException.getMessage(),
                            files[i].getAbsolutePath());
                    log.debug("Original exception", ioException);
                }
            }
        }
    }

    private void loadFromModulesDirectory(final ClassLoader parentClassloader){
        final File modulesFolder = systemInformation.getModulesFolder();
        final File[] libs = modulesFolder.listFiles();
        if (libs != null) {
            for (final File lib : libs) {
                try {
                    if (lib.getName().endsWith(".jar")) {
                        log.debug("Found module jar in modules lib {}.", lib.getAbsolutePath());
                        final IsolatedModuleClassloader isolatedClassloader =
                                new IsolatedModuleClassloader(new URL[]{lib.toURI().toURL()}, parentClassloader);
                        modules.add(new EdgeModule(lib, isolatedClassloader));
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
        for (EdgeModule module : modules) {
            try {
                final Iterable<Class<? extends T>> loaded = classServiceLoader.load(serviceClazz, module.classloader);
                for (Class<? extends T> foundClass : loaded) {
                    classes.add(foundClass);
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

    public static class EdgeModule {

        private File root;
        private final @NotNull ClassLoader classloader;

        public EdgeModule(final @NotNull File root, final @NotNull ClassLoader classloader) {
            this.classloader = classloader;
            this.root = root;
        }

        public @NotNull ClassLoader getClassloader() {
            return classloader;
        }

        public File getRoot(){
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
