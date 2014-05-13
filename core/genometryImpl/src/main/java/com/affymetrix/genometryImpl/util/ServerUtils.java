package com.affymetrix.genometryImpl.util;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderInstNC;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerUtils {

	private static final List<ServerTypeI> DEFAULT_SERVER_TYPES = new ArrayList<ServerTypeI>();
	static {
		DEFAULT_SERVER_TYPES.add(ServerTypeI.DAS2);
		DEFAULT_SERVER_TYPES.add(ServerTypeI.DAS);
		DEFAULT_SERVER_TYPES.add(ServerTypeI.QuickLoad);
		DEFAULT_SERVER_TYPES.add(ServerTypeI.LocalFiles);
	}
	
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
	
	public static boolean isResidueFile(String format){
		return (format.equalsIgnoreCase("bnib") || format.equalsIgnoreCase("fa") ||
				format.equalsIgnoreCase("2bit"));
	}
}
