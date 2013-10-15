package com.pixeldonor;


import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadedArchiver extends Archiver {
    private FileSystem zipfs;
    private ExecutorService es = Executors.newFixedThreadPool(4);
    private Visitor visitor = new Visitor();

    class Callable implements java.util.concurrent.Callable<Integer> {
        private Path file;

        Callable(Path file) {
            super();
            this.file = file;
        }

        @Override
        public Integer call() throws Exception {
            // copy input file to ZipFileSystem
            FileInputStream in = new FileInputStream(file.toFile());
            OutputStream out = Files.newOutputStream(zipfs.getPath(file.getFileName().toString()));

            IOUtils.copy(in, out);

            in.close();
            out.close();

            return 0;
        }
    }

    class Visitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            // ignore directories and symbolic links
            if (attrs.isRegularFile()) {
                es.submit(new Callable(file));
                incrementCount();
            }
            return FileVisitResult.CONTINUE;
        }
    }

    ThreadedArchiver(String inputDir, String outputFile) {
        super(inputDir, outputFile);
    }

    private void createZipFileSystem() throws IOException {
        // setup ZipFileSystem
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI zipURI = URI.create(String.format("jar:file:%s", this.getOutputFile()));
        zipfs = FileSystems.newFileSystem(zipURI, env);
    }

    public void setEs(ExecutorService es) {
        this.es = es;
    }

    @Override
    public void run() {
        try {
            this.createZipFileSystem();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // walk input directory using our visitor class
        FileSystem fs = FileSystems.getDefault();
        try {
            Files.walkFileTree(fs.getPath(this.getInputDir()), visitor);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // shutdown ExecutorService and block till tasks are complete
        es.shutdown();
        try {
            es.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            zipfs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
