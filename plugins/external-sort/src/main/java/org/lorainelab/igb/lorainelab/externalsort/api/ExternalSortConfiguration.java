package org.lorainelab.igb.externalsort.api;

import com.google.common.io.Files;
import java.io.File;
import java.nio.charset.Charset;

/**
 *
 * @author jeckstei
 */
public class ExternalSortConfiguration {

    private int maxTmpFiles;
    private long maxMemoryInBytes;
    private Charset charset;
    private File tmpDir;
    private boolean isDistinctValues;
    private int numHeaderRows;
    private boolean useGzipOnTmpFiles;

    public ExternalSortConfiguration() {
        numHeaderRows = 0;
        charset = Charset.defaultCharset();
        tmpDir = Files.createTempDir();
        isDistinctValues = false;
        useGzipOnTmpFiles = false;
        maxTmpFiles = 1024;
        maxMemoryInBytes = getEstimatedAvailableMemoryInBytes();
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

    public long getMaxMemoryInBytes() {
        return maxMemoryInBytes;
    }

    public void setMaxMemoryInBytes(long maxMemoryInBytes) {
        this.maxMemoryInBytes = maxMemoryInBytes;
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

    private long getEstimatedAvailableMemoryInBytes() {
        System.gc();
        return Runtime.getRuntime().freeMemory();
    }

}
