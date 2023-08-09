package com.hivemq.tools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Simon L Johnson
 */
public class CreateAdapterBlueprint {

    private final Map<String, String> replacements;
    private final File zipFile;
    private final File outputDirectory;

    public CreateAdapterBlueprint(
            final Map<String, String> replacements,
            final File zipFile,
            final File outputDirectory) throws IOException {
        this.replacements = replacements;
        this.zipFile = zipFile;
        if(zipFile == null){
            throw new NullPointerException("specify a non-null zipFile");
        }

        if(!zipFile.exists()){
            throw new FileNotFoundException(zipFile.getAbsolutePath());
        }

        this.outputDirectory = outputDirectory;
    }

    void run() throws IOException {

        if(!outputDirectory.exists()){
            outputDirectory.mkdirs();
        }

        ZipFile zipFile = new ZipFile(this.zipFile);
        for (Enumeration e = zipFile.entries(); e.hasMoreElements();) {
            ZipEntry entryIn = (ZipEntry) e.nextElement();
            File newFile = newFile(outputDirectory, entryIn, replacements);
            if (entryIn.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                try(InputStream is = zipFile.getInputStream(entryIn)){
                    byte[] arr = readZipEntry(is);
                    String outputName = entryIn.getName();
                    String resultName = PropertyReplacer.replaceProperties(outputName, replacements);
                    String result = PropertyReplacer.replaceProperties(new String(arr, StandardCharsets.UTF_8), replacements);
                    File file = new File(outputDirectory, resultName);
                    Files.write(file.toPath(), result.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry, Map<String, String> replacements) throws IOException {
        String outputName = zipEntry.getName();
        String resultName = PropertyReplacer.replaceProperties(outputName, replacements);
        File destFile = new File(destinationDir, resultName);
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    public static byte[] readZipEntry(InputStream zis) throws IOException {
        try(InputStream is = new BufferedInputStream(zis)){
            ByteArrayOutputStream baos
                    = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int length;
            while ((length = is.read(buf)) != -1) {
                baos.write(buf, 0, length);
            }
            return baos.toByteArray();
        }
    }

    public static void main(String[] args) throws IOException {

        String templateFile = null;
        String moduleDirectory = null;
        String adapterName = null;

        try (Scanner input = new Scanner(System.in)) {
            PrintStream output = System.out;
            templateFile = captureMandatoryFilePath(input, output, "Please specify path to template zip file (must exist)");
            moduleDirectory = captureMandatoryFilePath(input, output, "Please specify path to output module directory (must exist)");
            adapterName = captureMandatoryString(input, output, "Please specify the name of your adapter (alpha-numeric)");
        } catch (Exception e) {
            System.err.println("A fatal error was encountered: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        if(!adapterName.matches("^[a-zA-Z0-9]*$")){
            System.err.println("Adapter must contain only alpha numeric values " + adapterName);
            System.exit(1);
        }

        Map<String, String> map = new HashMap<>();
        map.put("nameUC", upperCaseFirst(adapterName.toLowerCase()));
        map.put("nameLC", adapterName.toLowerCase());
        File zipFile = new File(templateFile);
        File outputDirectory = new File(moduleDirectory);
        CreateAdapterBlueprint blueprint = new CreateAdapterBlueprint(map, zipFile, outputDirectory);
        blueprint.run();
        System.out.println("New module generated " + outputDirectory.getAbsolutePath());
    }

    protected static String captureMandatoryString(Scanner input, PrintStream output, String question){
        String value = null;
        while(value == null){
            output.print(String.format("%s : ", question));
            value = input.nextLine();
            value = value.trim();
            if(value.length() == 0) {
                value = null;
            }
        }
        return value;
    }

    protected static String captureMandatoryFilePath(Scanner input, PrintStream output, String question){
        String value = null;
        while(value == null){
            output.print(String.format("%s : ", question));
            value = input.nextLine();
            value = value.trim();
            if(value.length() == 0 || value == null) {
                value = null;
                continue;
            }
            File f = new File(value);
            if(!f.exists()){
                value = null;
            }
        }
        return value;
    }

    public static String upperCaseFirst(String str){
        if(str == null) return null;
        char[] chars = str.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }
}
