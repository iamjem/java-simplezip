package com.pixeldonor;


public abstract class Archiver extends Thread {
    private String inputDir;
    private String outputFile;
    private long count = 0;

    Archiver(String inputDir, String outputFile) {
        this.inputDir = inputDir;
        this.outputFile = outputFile;
    }

    public String getInputDir() {
        return inputDir;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void incrementCount() {
        count++;
    }

    public long getCount() {
        return count;
    }

    public abstract void run();
}
