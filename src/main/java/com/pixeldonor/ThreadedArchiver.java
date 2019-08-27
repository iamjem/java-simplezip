package com.pixeldonor;



import java.io.IOException;
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
    private Path root = null;

    class Callable implements java.util.concurrent.Callable<Integer> {
        private Path file;

        Callable(Path file) {
            super();
            this.file = file;
        }

        @Override
        public Integer call() throws Exception {
            Path path = zipfs.getPath(root.relativize(file).toString());
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.copy(file, path, StandardCopyOption.REPLACE_EXISTING);

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
        env.put("encoding", "UTF-8");
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
            root = fs.getPath(this.getInputDir());
            Files.walkFileTree(root, visitor);
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
