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
package com.hivemq.extensions.classloader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.classloader.ClassLoaderTestClass;
import com.hivemq.extension.sdk.api.services.Services;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.OnTheFlyCompilationUtil;

public class IsolatedExtensionClassloaderTest {

    @TempDir
    public File temporaryFolder;

    private @NotNull File folder;

    @BeforeEach
    public void setUp() throws Exception {
        folder = new File(temporaryFolder, "newFolder");
        folder.mkdir();
    }

    /**
     * This test contains pure magic. It does the following:
     * 1. Finds out the Java source file
     * 2. Copy the Java source file
     * 3. Modify the Java source file
     * 4. Compile the Java source file
     * 5. Load the just compiled Java source file
     */
    @Test
    public void test_modified_class_loaded() throws Exception {
        final File javaSrcFile = getJavaSrcFileForClassFile(ClassLoadedClass.class);
        final File file = new File(temporaryFolder, ClassLoadedClass.class.getSimpleName() + ".java");
        file.createNewFile();
        FileUtils.copyFile(javaSrcFile, file);

        replaceFileContent(file, "original", "modified");

        // actually compile the file
        OnTheFlyCompilationUtil.compileJavaFile(file, folder);

        final Class<?> aClass;
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(
                new URL[] {folder.toURI().toURL()}, getClass().getClassLoader())) {
            aClass = loader.loadClass(ClassLoadedClass.class.getCanonicalName());
        }

        // we can't cast to ClassLoadedClass because the parent classloader already has the "original" class loaded
        // casting would result in a ClassCastException!
        final Object classLoadedClass = aClass.getDeclaredConstructor().newInstance();
        final String output = (String) aClass.getDeclaredMethod("get").invoke(classLoadedClass);

        assertEquals("modified", output);

        // now let's check that the original class is not affected.
        // we're loading from the parent classloader
        final ClassLoadedClass originalClassloadedClass = new ClassLoadedClass();
        assertNotEquals(originalClassloadedClass.get(), output);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_original_class_loaded_delegate() throws Exception {
        final IsolatedExtensionClassloader loader =
                new IsolatedExtensionClassloader(getClass().getClassLoader(), null);

        final Class<?> aClass = loader.loadClass(ClassLoadedClass.class.getCanonicalName());

        final String output = (String) aClass.getDeclaredMethod("get").invoke(new ClassLoadedClass());

        assertEquals("original", output);
        loader.close();
    }

    @Test
    public void test_restricted_class_loaded_from_parent() throws Exception {
        final File javaSrcFile = getJavaSrcFileForClassFile(ClassLoaderTestClass.class);
        final File file = new File(temporaryFolder, ClassLoaderTestClass.class.getSimpleName() + ".java");
        file.createNewFile();
        FileUtils.copyFile(javaSrcFile, file);

        replaceFileContent(file, "original", "modified");

        // actually compile the file
        OnTheFlyCompilationUtil.compileJavaFile(file, folder);

        final Class<?> aClass;
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(
                new URL[] {folder.toURI().toURL()}, getClass().getClassLoader())) {
            aClass = loader.loadClass(ClassLoaderTestClass.class.getCanonicalName());
        }

        // we can't cast to ClassLoadedClass because the parent classloader already has the "original" class loaded
        // (casting would result in a ClassCastException)
        final Object classLoadedClass = aClass.getDeclaredConstructor().newInstance();
        final String output = (String) aClass.getDeclaredMethod("get").invoke(classLoadedClass);

        assertEquals("original", output);

        // now let's check that the original class is not affected (we're loading from the parent classloader)
        final ClassLoaderTestClass originalClassloadedClass = new ClassLoaderTestClass();
        assertEquals("original", originalClassloadedClass.get());
    }

    @Test
    public void test_restricted_class_loaded_from_parent_not_found_fallback_to_child() throws Exception {
        final File javaSrcFile = getJavaSrcFileForClassFile(ClassLoadedClass.class);
        final File file = new File(temporaryFolder, ClassLoadedClass.class.getSimpleName() + ".java");
        file.createNewFile();
        FileUtils.copyFile(javaSrcFile, file);

        replaceFileContent(
                file, "package com.hivemq.extensions.classloader;", "package com.hivemq.extensions.api.test;");
        replaceFileContent(file, "original", "modified");

        // actually compile the file
        OnTheFlyCompilationUtil.compileJavaFile(file, folder);

        final Class<?> aClass;
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(
                new URL[] {folder.toURI().toURL()}, getClass().getClassLoader())) {
            aClass = loader.loadClass("com.hivemq.extensions.api.test.ClassLoadedClass");
        }

        // the parent classloader should not know this class
        try {
            getClass().getClassLoader().loadClass("com.hivemq.extensions.api.test.ClassLoadedClass");
            fail();
        } catch (final ClassNotFoundException e) {
            // expected, no-op
        }

        // invoke the get() method
        final Object classLoadedClass = aClass.getDeclaredConstructor().newInstance();
        final String output = (String) aClass.getDeclaredMethod("get").invoke(classLoadedClass);

        assertEquals("modified", output);
    }

    @Test
    public void test_static_context_is_always_loaded_from_child() throws Exception {
        final Class<?> servicesClassParent = getClass().getClassLoader().loadClass(Services.class.getCanonicalName());

        final Field servicesFieldParent = servicesClassParent.getDeclaredField("services");
        servicesFieldParent.setAccessible(true);
        final ImmutableMap<String, Object> dependenciesParent = ImmutableMap.of("key", "original");
        servicesFieldParent.set(null, dependenciesParent);

        final URL serviceUrl = Services.class.getResource("Services.class");
        assertNotNull(serviceUrl);
        final String path = serviceUrl.toExternalForm();
        final URL folder =
                new URL(path.replace(Services.class.getCanonicalName().replace(".", File.separator) + ".class", ""));

        final Class<?> servicesClassIsolated;
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(
                new URL[] {folder.toURI().toURL()}, getClass().getClassLoader())) {
            servicesClassIsolated = loader.loadClass(Services.class.getCanonicalName());
        }

        final Field servicesFieldIsolated = servicesClassIsolated.getDeclaredField("services");
        servicesFieldIsolated.setAccessible(true);
        final ImmutableMap<String, Object> dependencies = ImmutableMap.of("key", "modified");
        servicesFieldIsolated.set(null, dependencies);

        // now let's check that the original class is not affected
        // (we're loading from the parent classloader and from the isolated classloader)
        //noinspection unchecked
        assertEquals("original", ((Map<String, Object>) servicesFieldParent.get(null)).get("key"));
        //noinspection unchecked
        assertEquals("modified", ((Map<String, Object>) servicesFieldIsolated.get(null)).get("key"));
    }

    @Test
    public void test_get_resource() throws Exception {
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(
                new URL[] {folder.toURI().toURL()}, getClass().getClassLoader())) {
            final URL resource = loader.getResource("logback-test.xml");
            assertNotNull(resource);
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_get_resource_delegate() throws Exception {
        try (final IsolatedExtensionClassloader loader =
                new IsolatedExtensionClassloader(getClass().getClassLoader(), null)) {
            final URL resource = loader.getResource("logback-test.xml");
            assertNotNull(resource);
        }
    }

    @Test
    public void test_get_resources() throws Exception {
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(
                new URL[] {folder.toURI().toURL()}, getClass().getClassLoader())) {
            final Enumeration<URL> resource = loader.getResources("logback-test.xml");
            assertNotNull(resource);
            assertTrue(resource.hasMoreElements());
            assertNotNull(resource.nextElement());
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_get_resources_delegate() throws Exception {
        try (final IsolatedExtensionClassloader loader =
                new IsolatedExtensionClassloader(getClass().getClassLoader(), null)) {
            final Enumeration<URL> resource = loader.getResources("logback-test.xml");
            assertNotNull(resource);
            assertTrue(resource.hasMoreElements());
            assertNotNull(resource.nextElement());
        }
    }

    @Test
    public void test_get_resources_as_stream() throws Exception {
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(
                new URL[] {folder.toURI().toURL()}, getClass().getClassLoader())) {
            final InputStream resource = loader.getResourceAsStream("logback-test.xml");
            assertNotNull(resource);
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_get_resources_as_stream_delegate() throws Exception {
        try (final IsolatedExtensionClassloader loader =
                new IsolatedExtensionClassloader(getClass().getClassLoader(), null)) {
            final InputStream resource = loader.getResourceAsStream("logback-test.xml");
            assertNotNull(resource);
        }
    }

    private void replaceFileContent(
            final @NotNull File file, final @NotNull String original, final @NotNull String modified) throws Exception {
        String content = FileUtils.readFileToString(file, UTF_8);
        content = content.replaceAll(original, modified);
        FileUtils.writeStringToFile(file, content, UTF_8);
    }

    /**
     * The {@code .java} source of {@code clazz}, so the test can copy, modify and recompile it.
     * <p>
     * The module root is derived by removing the build system's fixed output suffix from the class location --
     * {@code build/classes/java/test} for Gradle, {@code out/test/classes} for IntelliJ. Both are constants of
     * the build system, so nothing here depends on what any directory in the path happens to be called.
     * <p>
     * The previous version searched instead: it walked up until a path {@code endsWith("out")}, meaning
     * IntelliJ's output folder. Under Gradle no such segment exists, so the walk was designed to run all the
     * way to {@code /}, whose parent is {@code null}, which makes {@code new File((File) null, "src")} a
     * RELATIVE path that Gradle then resolves against the module working directory. It worked, but only by
     * that accident, and {@code endsWith} matches a string rather than a directory name -- so a Jenkins
     * workspace derived from a branch called {@code .../something-timeout} ends with "out", the walk stopped
     * on the workspace itself, and every derived path was wrong. Three tests failed on every build of that
     * branch and passed after nothing but a rename (EDG-959).
     */
    private File getJavaSrcFileForClassFile(final Class<?> clazz) {
        final File classesRoot = new File(
                clazz.getProtectionDomain().getCodeSource().getLocation().getPath());
        final Path classes = classesRoot.toPath().toAbsolutePath().normalize();

        Path moduleRoot = null;
        for (final String suffix : List.of("build/classes/java/test", "out/test/classes")) {
            final Path relative = Paths.get(suffix);
            if (classes.endsWith(relative)) {
                moduleRoot =
                        classes.getRoot().resolve(classes.subpath(0, classes.getNameCount() - relative.getNameCount()));
                break;
            }
        }
        // Fail with the path we looked at. Deriving a wrong path silently is what made the branch-name bug
        // cost an evening: the failure named a file that never existed and gave no hint why.
        assertNotNull(
                moduleRoot,
                "Cannot derive the module root from the class location '" + classes
                        + "'. Expected it to end with 'build/classes/java/test' (Gradle) or 'out/test/classes'"
                        + " (IntelliJ). Add the new layout's suffix here.");

        Path testJava = moduleRoot.resolve("src/test/java");
        if (!Files.isDirectory(testJava)) {
            testJava = moduleRoot.resolve("src/core/test/java");
        }
        return testJava.resolve(clazz.getCanonicalName().replace(".", File.separator) + ".java")
                .toFile();
    }
}
