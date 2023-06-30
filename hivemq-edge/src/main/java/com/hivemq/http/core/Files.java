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
package com.hivemq.http.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPOutputStream;

public class Files {

    public static final byte NEW_LINE_DECIMAL = 10;
    public static final String PERIOD = ".";

    public static String getFileName(String filePath){
        int idx = filePath.lastIndexOf(File.separator);
        if(idx > -1){
            filePath = filePath.substring(idx);
        }
        return filePath;
    }

    public static String getFileNameExcludingExtension(String filePath){
        String name = getFileName(filePath);
        if(name.contains(PERIOD)){
            name = name.substring(name.lastIndexOf(PERIOD) + 1);
        }
        return name;
    }

    public static String getFileExtension(String filePath){
        int idx = filePath.lastIndexOf(PERIOD);
        String ext = null;
        if(idx > -1){
            ext = filePath.substring(idx + 1);
        }
        return ext;
    }

    public static void copy(InputStream is, OutputStream os, int bufSize) throws IOException {
        byte[] buf = new byte[bufSize];
        int length;
        while ((length = is.read(buf)) != -1) {
            os.write(buf, 0, length);
        }
    }

    public static byte[] read(InputStream is, int bufSize) throws IOException {
        try(ByteArrayOutputStream baos
                    = new ByteArrayOutputStream()){
            byte[] buf = new byte[bufSize];
            int length;
            while ((length = is.read(buf)) != -1) {
                baos.write(buf, 0, length);
            }
            return baos.toByteArray();
        }
    }

    public static byte[] read(File f) throws IOException {
        try(InputStream is = new BufferedInputStream(
                new FileInputStream(f))){
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

    public static void writeWithLock(File file, byte[] bytes)
            throws IOException, OverlappingFileLockException {

        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileChannel channel = raf.getChannel();
        FileLock lock = null;
        try {
            lock = channel.tryLock();
            channel.truncate(bytes.length);
            channel.write(ByteBuffer.wrap(bytes));
        }
        finally {
            try {
                if(lock != null) lock.release();
            } finally {
                if(raf != null) raf.close();
                if(channel != null) channel.close();
            }
        }
    }

    public static void appendWithLock(File file, byte[] bytes, boolean newLine)
            throws IOException, OverlappingFileLockException {

        try (RandomAccessFile raf =
                     new RandomAccessFile(file, "rw")){
            raf.seek(raf.length());
            FileChannel channel = raf.getChannel();
            FileLock lock = null;
            try {
                lock = channel.tryLock();
                if(raf.getFilePointer() > 0 && newLine){
                    channel.write(ByteBuffer.wrap(
                            System.lineSeparator().
                                    getBytes(StandardCharsets.UTF_8)));
                }
                channel.write(ByteBuffer.wrap(bytes));
            } finally {
                if(lock != null) lock.release();
            }
        }
    }

    public static void append(File f, byte[] bytes)
            throws IOException {
        java.nio.file.Files.write(f.toPath(), bytes,
                StandardOpenOption.APPEND);
    }

    public static long directorySize(File dir) {
        if (!dir.exists()) {
            throw new IllegalArgumentException(dir + " does not exist");
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir + " is not a directory");
        }
        long size = 0;
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                size += directorySize(file);
            } else {
                size += file.length();
            }
        }
        return size;
    }

    public static void gzipFile(File input, File output, int bufferSize) throws IOException {
        try (GZIPOutputStream out = new GZIPOutputStream(
                new FileOutputStream(output))){
            try (FileInputStream in =
                         new FileInputStream(input)){
                byte[] buffer = new byte[bufferSize];
                int len;
                while( (len=in.read(buffer)) != -1)
                    out.write(buffer, 0, len);
            }
        }
    }

    public static synchronized void createRuntimeLockFile(File dir) throws IOException {
        File lockFile = new File(dir, ".lck");
        if(lockFile.exists()){
            throw new IOException("lock already in use");
        }
        lockFile.createNewFile();
        lockFile.deleteOnExit();
        Thread h = new Thread(lockFile::delete);
        Runtime.getRuntime().addShutdownHook(h);
    }

    /**
     * Count the lines in a given file (assumes the file is character data)
     */
    public static int countLines(File file) throws IOException{
        int lines = 0;
        try(BufferedReader reader =
                new BufferedReader(new FileReader(file))){
            while (reader.readLine() != null) lines++;
        }
        return lines;
    }

    /**
     * Truncate the given file to the size specified
     */
    public static void truncate(File file, int size) throws IOException{
        try (FileChannel outChan =
                     new FileOutputStream(file, true).getChannel()) {
            outChan.truncate(size);
        }
    }



    /**
     * Given a file of character data, consume from the head of the file
     * the number of lines specified. NOTE: when a line is consumed,
     * it is removed from the file.
     *
     * Important: the is no native mechanism to modified file storage in line,
     * we therefore must write a temporary file containing the pruned data
     * and switch this file in.
     *
     * NB: This should not be used on large files since it involves copying
     * large sections of the file.
     */
    public static byte[] consumeLinesFromStart(File file, int count)
            throws IOException {
        ByteArrayOutputStream baos = null;
        try (BufferedReader reader =
                     new BufferedReader(new FileReader(file))){
            File tmp = File.createTempFile(
                    getFileNameExcludingExtension(file.getName()),
                    PERIOD + getFileExtension(file.getName()));
            try(BufferedOutputStream fos =
                    new BufferedOutputStream(
                            new FileOutputStream(tmp))) {
                baos = new ByteArrayOutputStream(1024);
                String line;
                int progress = 0;
                while((line = reader.readLine()) != null) {
                    byte[] b = line.getBytes(StandardCharsets.UTF_8);
                    if(b.length > 0){
                        OutputStream os =
                                progress++ < count ?  baos : fos;
                        os.write(b);
                        os.write(System.lineSeparator().
                                getBytes(StandardCharsets.UTF_8));
                    }
                }
            }

            if(!tmp.renameTo(file)){
                throw new IOException("unable to rename temp file");
            }
        }
        return trimNewLines(baos.toByteArray());
    }

    private static byte[] trimNewLines(byte[] a){

        if(a.length == 0) return a;

        if(a[0] == NEW_LINE_DECIMAL){
            byte[] trim = new byte[a.length - 1];
            System.arraycopy(a, 1, trim, 0, a.length - 1);
            a = trim;
        }

        if(a[a.length - 1] == NEW_LINE_DECIMAL){
            byte[] trim = new byte[a.length - 1];
            System.arraycopy(a, 0, trim, 0, a.length - 1);
            a = trim;
        }

        return a;
    }

    public static void delete(File f)
            throws IOException {
        if(f.exists()) {
            if(f.isDirectory()) {
                File[] files = f.listFiles();
                for(int i = 0; files != null &&
                        i < files.length; i++) {
                    delete(files[i]);
                }
            }
            if(!f.delete()) {
                throw new IOException("unable to delete; " + f);
            }
        }
    }
}
