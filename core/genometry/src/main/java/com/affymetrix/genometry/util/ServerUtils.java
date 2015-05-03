package com.affymetrix.genometry.util;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.FileTypeHolder;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symloader.SymLoaderInstNC;
import com.google.common.base.Stopwatch;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerUtils {

    private static final FileTypeHolder fileTypeHolder = FileTypeHolder.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(ServerUtils.class);

    /**
     * Determine the appropriate loader.
     *
     * @return the SymLoader requested
     */
    public static SymLoader determineLoader(String extension, URI uri, String featureName, GenomeVersion genomeVersion) {
        FileTypeHandler fileTypeHandler = fileTypeHolder.getFileTypeHandler(extension);
        SymLoader symLoader;
        if (fileTypeHandler == null) {
            logger.warn("Couldn't find any Symloader for {0} format. Opening whole file.", new Object[]{extension});
            symLoader = new SymLoaderInstNC(uri, featureName, genomeVersion);
        } else {
            final Stopwatch stopwatch = Stopwatch.createStarted();
            symLoader = fileTypeHandler.createSymLoader(uri, featureName, genomeVersion);
            stopwatch.stop();
            logger.info("STOPWATCH METRICS for createSymLoader {}", stopwatch);
        }
        return symLoader;
    }

    public static boolean isResidueFile(String format) {
        return (format.equalsIgnoreCase("bnib") || format.equalsIgnoreCase("fa")
                || format.equalsIgnoreCase("2bit"));
    }
}
