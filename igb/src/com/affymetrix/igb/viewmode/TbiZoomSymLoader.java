package com.affymetrix.igb.viewmode;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.tribble.readers.TabixReaderExt;


import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderTabix;
import com.affymetrix.genometryImpl.util.GeneralUtils;

public class TbiZoomSymLoader extends IndexZoomSymLoader {
	private TabixReaderExt tabixReaderExt;

	public TbiZoomSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
		super(uri, featureName, group);
	}

	private URI getFileURI(URI baiUri) throws Exception {
		String bamUriString = baiUri.toString().substring(0, baiUri.toString().length() - ".tbi".length());
		if (!bamUriString.startsWith("file:") && !bamUriString.startsWith("http:") && !bamUriString.startsWith("https:") && !bamUriString.startsWith("ftp:")) {
			bamUriString = GeneralUtils.getFileScheme() + bamUriString;
		}
		return new URI(bamUriString);
	}

	@Override
	protected SymLoader getDataFileSymLoader() throws Exception {
		URI fileUri = getFileURI(uri);
		return FileTypeHolder.getInstance().getFileTypeHandlerForURI(fileUri.toString()).createSymLoader(fileUri, featureName, group);
	}

    @Override
	public void init() throws Exception  {
		if (this.isInitialized){
			return;
		}
		try {
			String uriString = uri.toString();
			if (uriString.startsWith(FILE_PREFIX)) {
				uriString = GeneralUtils.fixFileName(uriString);
			}
			uriString = uriString.toString().substring(0, uriString.toString().length() - ".tbi".length());
			tabixReaderExt = new TabixReaderExt(uriString);
			tabixReaderExt.readIndex();
		}
		catch (Exception x) {
			Logger.getLogger(SymLoaderTabix.class.getName()).log(Level.SEVERE,
						"Could not read tabix for {0}.",
						new Object[]{featureName});
			return;
		}
		this.isInitialized = true;
	}

	@Override
	protected Iterator<Map<Integer, List<List<Long>>>> getBinIter(String seq) {
		return tabixReaderExt.getBinIter(getSynonymMap(), seq);
	}
}
