package com.affymetrix.genometry.util;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.FileTypeHolder;
import com.affymetrix.genometry.parsers.FileTypehandlerRegistry;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symloader.SymLoaderInstNC;
import java.net.URI;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerUtils {

    private static final FileTypeHolder fileTypeHolder = FileTypehandlerRegistry.getFileTypeHolder();
    private static final Logger logger = LoggerFactory.getLogger(ServerUtils.class);

    /**
     * Determine the appropriate loader.
     *
     * @return the SymLoader requested
     */
    public static SymLoader determineLoader(String extension, URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        FileTypeHandler fileTypeHandler = fileTypeHolder.getFileTypeHandler(extension);
        SymLoader symLoader;
        if (fileTypeHandler == null) {
            logger.warn("Couldn't find any Symloader for {} format. Opening whole file.", extension);
            symLoader = new SymLoaderInstNC(uri, indexUri, featureName, genomeVersion);
        } else {
            symLoader = fileTypeHandler.createSymLoader(uri, indexUri, featureName, genomeVersion);
        }
        return symLoader;
    }

    public static boolean isResidueFile(String format) {
        return (format.equalsIgnoreCase("bnib") || format.equalsIgnoreCase("fa")
                || format.equalsIgnoreCase("2bit"));
    }
}
