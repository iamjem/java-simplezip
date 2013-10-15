package com.pixeldonor;


import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SimpleArchiver extends Archiver {
    private ZipOutputStream zos;

    SimpleArchiver(String inputDir, String outputFile) {
        super(inputDir, outputFile);
    }

    private void zipFile(File file) throws IOException {
        ZipEntry ze = new ZipEntry(file.getName());
        zos.putNextEntry(ze);
        FileInputStream fis = new FileInputStream(file);
        IOUtils.copy(fis, zos);
        fis.close();
        incrementCount();
    }

    @Override
    public void run() {
        try {
            // create output file if it doesn't exist
            File out = new File(getOutputFile());
            if (!out.exists()) {
                out.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(getOutputFile());
            zos = new ZipOutputStream(fos);

            // iterate through input directory files
            // and copy anything that's a file
            File[] fileList = new File(getInputDir()).listFiles();
            for (File file : fileList) {
                if (!file.isDirectory()) {
                    zipFile(file);
                }
            }

            zos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
