package com.lorainelab.externalsort.api;

import java.io.File;
import java.nio.charset.Charset;

/**
 *
 * @author jeckstei
 */
public class ExternalSortConfiguration {
    private int maxTmpFiles;
    private long maxMemory;
    private Charset charset;
    private File tmpDir;
    private boolean isDistinctValues;
    private int numHeaderRows;
    private boolean useGzipOnTmpFiles;
    private int[] columns;

    public ExternalSortConfiguration() {
        charset = Charset.defaultCharset();
        columns = new int[]{0};
    }

    public int[] getColumns() {
        return columns;
    }

    public void setColumns(int[] columns) {
        this.columns = columns;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }


    public int getMaxTmpFiles() {
        return maxTmpFiles;
    }

    public void setMaxTmpFiles(int maxTmpFiles) {
        this.maxTmpFiles = maxTmpFiles;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(long maxMemory) {
        this.maxMemory = maxMemory;
    }


    public File getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    public boolean isIsDistinctValues() {
        return isDistinctValues;
    }

    public void setIsDistinctValues(boolean isDistinctValues) {
        this.isDistinctValues = isDistinctValues;
    }

    public int getNumHeaderRows() {
        return numHeaderRows;
    }

    public void setNumHeaderRows(int numHeaderRows) {
        this.numHeaderRows = numHeaderRows;
    }

    public boolean isUseGzipOnTmpFiles() {
        return useGzipOnTmpFiles;
    }

    public void setUseGzipOnTmpFiles(boolean useGzipOnTmpFiles) {
        this.useGzipOnTmpFiles = useGzipOnTmpFiles;
    }

}
