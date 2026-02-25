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
package com.hivemq.tools;

import com.google.common.base.Preconditions;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public class CreateAdapterBlueprint {

    static final String ADAPTER_TEMPLATE = "ext/module-templates/polling-adapter-template.zip";
    static final String MODULES_DIR = "modules";

    private final Map<String, String> replacements;
    private final File zipFile;
    private final File outputDirectory;

    public CreateAdapterBlueprint(
            final Map<String, String> replacements, final File zipFile, final File outputDirectory) throws IOException {
        this.replacements = replacements;
        this.zipFile = zipFile;
        if (zipFile == null) {
            throw new NullPointerException("specify a non-null zipFile");
        }

        if (!zipFile.exists()) {
            throw new FileNotFoundException(zipFile.getAbsolutePath());
        }

        this.outputDirectory = outputDirectory;
    }

    void run() throws IOException {

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        try (final var zipFile = new ZipFile(this.zipFile)) {
            for (final Enumeration<?> e = zipFile.entries(); e.hasMoreElements(); ) {
                final var entryIn = (ZipEntry) e.nextElement();
                final var newFile = newFile(outputDirectory, entryIn, replacements);
                if (entryIn.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    try (final var is = zipFile.getInputStream(entryIn)) {
                        final var arr = readZipEntry(is);
                        final var outputName = entryIn.getName();
                        final var resultName = PropertyReplacer.replaceProperties(outputName, replacements);
                        final var file = new File(outputDirectory, resultName);
                        if (processFile(resultName)) {
                            final var result = PropertyReplacer.replaceProperties(
                                    new String(arr, StandardCharsets.UTF_8), replacements);
                            Files.writeString(file.toPath(), result);
                        } else {
                            // binary files
                            Files.write(file.toPath(), arr);
                        }
                    }
                }
            }
        }
    }

    protected boolean processFile(final @NotNull String fileName) {
        return !fileName.endsWith("png")
                && !fileName.endsWith("jpg")
                && !fileName.endsWith("gif")
                && !fileName.endsWith("bat");
    }

    public static File newFile(
            final @NotNull File destinationDir,
            final @NotNull ZipEntry zipEntry,
            final @NotNull Map<String, String> replacements)
            throws IOException {
        final var outputName = zipEntry.getName();
        final var resultName = PropertyReplacer.replaceProperties(outputName, replacements);
        final var destFile = new File(destinationDir, resultName);
        final var destDirPath = destinationDir.getCanonicalPath();
        final var destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + java.io.File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    public static byte[] readZipEntry(final @NotNull InputStream zis) throws IOException {
        try (final var is = new BufferedInputStream(zis)) {
            final var baos = new ByteArrayOutputStream();
            final var buf = new byte[1024];
            int length;
            while ((length = is.read(buf)) != -1) {
                baos.write(buf, 0, length);
            }
            return baos.toByteArray();
        }
    }

    public static void main(final @NotNull String[] args) throws IOException {

        String templateFile = null;
        String moduleDirectory = null;
        String adapterName = null;

        File edgeHome = detectEdgeHomeDir();

        try (final var input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            final var output = System.out;
            if (edgeHome == null) {
                edgeHome =
                        new File(captureMandatoryFilePath(input, output, "Please specify path to hivemq-edge project"));
            }

            if (edgeHome.exists()) {
                output.println("Detected your edge home as '" + edgeHome.getAbsolutePath()
                        + "', running relative to this location");
                moduleDirectory = new File(edgeHome, MODULES_DIR).getAbsolutePath();
                templateFile = new File(edgeHome, ADAPTER_TEMPLATE).getAbsolutePath();
            } else {
                throw new FileNotFoundException("Unable to locate HIVEMQ_EDGE development home");
            }
            adapterName =
                    captureMandatoryString(input, output, "Please specify the name of your adapter (alpha-numeric)");

        } catch (final Exception e) {
            System.err.println("A fatal error was encountered: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        if (!adapterName.matches("^[a-zA-Z0-9]*$")) {
            System.err.println("Adapter must contain only alpha numeric values " + adapterName);
            System.exit(1);
        }

        final var adapterModuleName = adapterName.toLowerCase();
        final Map<String, String> map = new HashMap<>();
        map.put("nameUC", upperCaseFirst(adapterName.toLowerCase()));
        map.put("nameLC", adapterModuleName);
        final var zipFile = new File(templateFile);
        final var outputDirectory = new File(moduleDirectory);
        final var blueprint = new CreateAdapterBlueprint(map, zipFile, outputDirectory);
        blueprint.run();

        // -- add gradle configuration
        insertTextAtLocation(
                new File(edgeHome, "build.gradle.kts"),
                String.format("\tedgeModule(\"com.hivemq:hivemq-edge-module-%s\")", adapterModuleName),
                86);
        insertTextAtLocation(
                new File(edgeHome, "settings.gradle.kts"),
                String.format("includeBuild(\"./modules/hivemq-edge-module-%s\")", adapterModuleName),
                7);
    }

    protected static File detectEdgeHomeDir() {
        File edgeHome = null;
        final var file = new File(System.getProperty("user.dir"));
        if (file.exists() && file.isDirectory()) {
            if ("hivemq-edge-composite".equals(file.getName())) {
                edgeHome =
                        new File(com.hivemq.util.Files.getFilePathExcludingFile(file.getAbsolutePath()), "hivemq-edge");
            } else if ("hivemq-edge".equals(file.getName())) {
                // -- check if we are at the hivemq-edge root or in the nested project
                if (new File(file, "hivemq-edge").exists()) {
                    edgeHome = file;
                } else {
                    File f = new File(com.hivemq.util.Files.getFilePathExcludingFile(file.getAbsolutePath()));
                    if (f.exists()) {
                        edgeHome = f;
                    }
                }
            }
        }
        return edgeHome;
    }

    protected static String captureMandatoryString(
            final @NotNull Scanner input, final @NotNull PrintStream output, final @NotNull String question) {
        String value = null;
        while (value == null) {
            output.printf("%s : ", question);
            value = input.nextLine();
            value = value.trim();
            if (value.isEmpty()) {
                value = null;
            }
        }
        return value;
    }

    protected static String captureMandatoryChoice(
            final @NotNull Scanner input,
            final @NotNull PrintStream output,
            final @NotNull String question,
            final @NotNull String[] choices) {
        String value = null;
        while (value == null) {
            output.printf("%s (Please select one) : %n", question);
            for (int i = 0; i < choices.length; i++) {
                output.print(String.format("%d : %s", i + 1, choices[i]));
            }
            value = input.nextLine();
            value = value.trim();
            if (value.isEmpty()) {
                value = null;
            }
        }
        return value;
    }

    protected static String captureMandatoryFilePath(
            final @NotNull Scanner input, final @NotNull PrintStream output, final @NotNull String question) {
        String value = null;
        while (value == null) {
            output.printf("%s : ", question);
            value = input.nextLine();
            value = value.trim();
            if (value.isEmpty()) {
                value = null;
                continue;
            }
            final var f = new File(value);
            if (!f.exists()) {
                value = null;
            }
        }
        return value;
    }

    public static String upperCaseFirst(final @NotNull String str) {
        final var chars = str.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    public static void insertTextAtLocation(
            final @NotNull File textFile, final @NotNull String text, final int lineNumber) throws IOException {
        Preconditions.checkNotNull(textFile);
        Preconditions.checkNotNull(text);
        if (!textFile.canRead()) {
            throw new FileNotFoundException("file not found " + textFile.getAbsolutePath());
        }
        final var tmpFile = new File(textFile.getParentFile(), textFile.getName() + ".tmp");
        try (final var reader = java.nio.file.Files.newBufferedReader(textFile.toPath(), StandardCharsets.UTF_8)) {
            try (final var writer = java.nio.file.Files.newBufferedWriter(tmpFile.toPath(), StandardCharsets.UTF_8)) {
                int line = 0;
                String textLine = reader.readLine();
                while (textLine != null) {
                    if (++line == lineNumber) {
                        writer.write(text);
                        writer.write(System.lineSeparator());
                    }
                    writer.write(textLine);
                    writer.write(System.lineSeparator());
                    textLine = reader.readLine();
                }
                writer.flush();
            }
        }
        tmpFile.renameTo(textFile);
    }
}
