package com.affymetrix.genometry.util;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.das.DasServerType;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.FileTypeHolder;
import com.affymetrix.genometry.quickload.QuickloadServerType;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symloader.SymLoaderInstNC;
import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerUtils {

    private static final List<ServerTypeI> DEFAULT_SERVER_TYPES = ImmutableList.of(DasServerType.getInstance(), QuickloadServerType.getInstance(), LocalFilesServerType.getInstance());

    /**
     * Format a URL based on the ServerType's requirements.
     *
     * @param url URL to format
     * @param type type of server the URL represents
     * @return formatted URL
     */
    public static String formatURL(String url, ServerTypeI type) {
        try {
            url = url.replace(" ", "");
            url = new URI(url).normalize().toASCIIString();
        } catch (URISyntaxException ex) {
            String message = "Unable to parse URL: '" + url + "'";
            Logger.getLogger(ServerUtils.class.getName()).log(Level.SEVERE, message, ex);
            throw new IllegalArgumentException(message, ex);
        }
        if (type == null) {
            return url;
        }
        return type.formatURL(url);
    }

    /**
     * Determine the appropriate loader.
     *
     * @return the SymLoader requested
     */
    public static SymLoader determineLoader(String extension, URI uri, String featureName, AnnotatedSeqGroup group) {
        FileTypeHandler fileTypeHandler = FileTypeHolder.getInstance().getFileTypeHandler(extension);
        SymLoader symLoader;
        if (fileTypeHandler == null) {
            Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING, "Couldn't find any Symloader for {0} format. Opening whole file.", new Object[]{extension});
            symLoader = new SymLoaderInstNC(uri, featureName, group);
        } else {
            symLoader = fileTypeHandler.createSymLoader(uri, featureName, group);
        }
        return symLoader;
    }

    public static List<ServerTypeI> getServerTypes() {
        List<ServerTypeI> serverTypes = ExtensionPointHandler.getExtensionPoint(ServerTypeI.class) == null ? DEFAULT_SERVER_TYPES : ExtensionPointHandler.getExtensionPoint(ServerTypeI.class).getExtensionPointImpls();
        Collections.sort(serverTypes);
        return serverTypes;
    }

    public static boolean isResidueFile(String format) {
        return (format.equalsIgnoreCase("bnib") || format.equalsIgnoreCase("fa")
                || format.equalsIgnoreCase("2bit"));
    }
}
