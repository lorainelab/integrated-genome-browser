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
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ServerUtils {

    private static final List<ServerTypeI> DEFAULT_SERVER_TYPES = ImmutableList.of(DasServerType.getInstance(), QuickloadServerType.getInstance(), LocalFilesServerType.getInstance());

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
        return serverTypes;
    }

    public static Set<ServerTypeI> getServerTypesSupportingUserInstances() {
        return getServerTypes().stream().filter(serverType -> serverType.supportsUserAddedInstances()).collect(Collectors.toSet());
    }

    public static boolean isResidueFile(String format) {
        return (format.equalsIgnoreCase("bnib") || format.equalsIgnoreCase("fa")
                || format.equalsIgnoreCase("2bit"));
    }
}
