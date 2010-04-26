/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.useq.ArchiveInfo;
import com.affymetrix.genometryImpl.parsers.useq.USeqGraphParser;
import com.affymetrix.genometryImpl.parsers.useq.USeqRegionParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

/**
 *
 * @author jnicol
 */
public class USeq extends SymLoader {

	private final AnnotatedSeqGroup group;
	private ArchiveInfo archiveInfo = null;
	private final String featureName;
	private File f = null;

	public USeq(URI uri, String featureName, AnnotatedSeqGroup group) {
		super(uri);
		this.featureName = featureName;
		this.group = group;
	}

	@Override
	public String[] getLoadChoices() {
		String[] choices = {LoadStrategy.NO_LOAD.toString(), LoadStrategy.GENOME.toString()};
		return choices;
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
	public List<? extends SeqSymmetry> getGenome() {
		init();
		ZipInputStream zis = null;
		try {
			zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(f)));
			zis.getNextEntry();
			archiveInfo = new ArchiveInfo(zis, false);
			if (archiveInfo.getDataType().equals(ArchiveInfo.DATA_TYPE_VALUE_GRAPH)) {
				USeqGraphParser gp = new USeqGraphParser();
				return gp.parseGraphSyms(zis, GenometryModel.getGenometryModel(), featureName, archiveInfo);
			} else {
				USeqRegionParser rp = new USeqRegionParser();
				return rp.parse(zis, group, featureName, false, archiveInfo);
			}
		} catch (Exception ex) {
			Logger.getLogger(USeq.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(zis);
		}
		return Collections.<SeqSymmetry>emptyList();
	}
}
