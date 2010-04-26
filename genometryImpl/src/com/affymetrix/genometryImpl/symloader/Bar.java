package com.affymetrix.genometryImpl.symloader;

import java.io.*;
import java.util.*;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Bar extends SymLoader {

	private final String featureName;
	private final AnnotatedSeqGroup group;
	private File f = null;

	public Bar(URI uri, String featureName, AnnotatedSeqGroup group) {
		super(uri);
		this.featureName = featureName;
		this.group = group;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		super.init();
		f = LocalUrlCacher.convertURIToFile(uri);
	}

	@Override
	public List<GraphSym> getGenome() {
		FileInputStream fis = null;
		try {
			init();
			fis = new FileInputStream(f);
			return BarParser.parse(fis, GenometryModel.getGenometryModel(), group, featureName, true);
		} catch (Exception ex) {
			Logger.getLogger(Bar.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(fis);
		}
		return null;
	}
}
