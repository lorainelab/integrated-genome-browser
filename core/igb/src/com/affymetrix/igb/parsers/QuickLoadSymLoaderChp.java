package com.affymetrix.igb.parsers;

import java.io.File;
import java.net.URI;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.io.IOException;

/**
 * This is an extension of the QuickLoadSymLoader class, specifically for chp files.
 * If this is not a chp file (extension ends with "chp") an Exception is thrown
 */
public class QuickLoadSymLoaderChp extends QuickLoadSymLoader {

	public QuickLoadSymLoaderChp(URI uri, String featureName, GenericVersion version, SymLoader symL) {
		super(uri, featureName, version, symL);
		if (!extension.endsWith("chp")) {
			throw new IllegalStateException("wrong QuickLoad for chp file");
		}
	}
	
	protected List<? extends SeqSymmetry> loadSymmetriesThread(final GenericFeature feature, final SeqSpan overlapSpan)
		throws OutOfMemoryError, Exception {
		// special-case chp files, due to their LazyChpSym DAS/2 loading
		addMethodsToFeature(feature, QuickLoadSymLoaderChp.this.getGenome());
		return QuickLoadSymLoaderChp.this.getGenome();
	}

	protected void addAllSymmetries(final GenericFeature feature, List<? extends SeqSymmetry> results)
			throws OutOfMemoryError {
		// special-case chp files, due to their LazyChpSym DAS/2 loading
		addMethodsToFeature(feature, results);
	}

	//Only used for "chp"
	private static void addMethodsToFeature(GenericFeature feature, List<? extends SeqSymmetry> results) {
		if(results == null)
			return;
		
		String method;
		for (SeqSymmetry sym : results) {
			method = BioSeq.determineMethod(sym);
			if (method != null) {
				feature.addMethod(method);
			}
		}
	}
	
	/**
	 * Only used for non-symloader files.
	 */
	@Override
	public List<? extends SeqSymmetry> getGenome() {
		try {
			// special-case CHP files. ChpParser only has
			//    a parse() method that takes the file name
			// (ChpParser uses Affymetrix Fusion SDK for actual file parsing)
			File f = LocalUrlCacher.convertURIToFile(this.uri);
			return ChpParser.parse(f.getAbsolutePath(), true);
		} catch(IOException ex){
			throw new RuntimeException(ex);
		}catch (Exception ex) {
			logException(ex);
			throw new RuntimeException(ex);
		}
	}
}
