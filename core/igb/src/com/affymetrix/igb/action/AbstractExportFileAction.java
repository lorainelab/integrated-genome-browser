package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.parsers.AnnotationWriter;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symloader.Fasta;
import com.affymetrix.genometryImpl.symloader.BedGraph;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.shared.TierGlyph;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.util.logging.Level;

public abstract class AbstractExportFileAction
extends GenericAction implements SymSelectionListener {
	private static final long serialVersionUID = 1l;
	private static final String MIME_TYPE_PREFIX = "text/";
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();

	private static final Map<FileTypeCategory, Class<? extends AnnotationWriter>> annotationWriters
			= new HashMap<FileTypeCategory, Class<? extends AnnotationWriter>>();
	static {
		annotationWriters.put(FileTypeCategory.Annotation, BedParser.class);
		annotationWriters.put(FileTypeCategory.Graph, BedGraph.class);
		annotationWriters.put(FileTypeCategory.Sequence, Fasta.class);
	}
	
	protected AbstractExportFileAction(
			String text,
			String tooltip,
			String iconPath, String largeIconPath,
			int mnemonic,
			Object extraInfo,
			boolean popup) {
		super(text, tooltip, iconPath, largeIconPath, mnemonic, extraInfo, popup);
	}

	/**
	 * Override to enable or disable self based on tracks selected.
	 * Note that this must match {@link #actionPerformed(java.awt.event.ActionEvent} 
	 * which only works when one track is selected.
	 */
	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		List<Glyph> answer = IGBServiceImpl.getInstance().getSelectedTierGlyphs();
		setEnabled(1 == answer.size());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		List<Glyph> current_tiers = IGBServiceImpl.getInstance().getSelectedTierGlyphs();
		if (current_tiers.size() > 1) {
			ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
		}
		else if (current_tiers.isEmpty()) {
			ErrorHandler.errorPanel(BUNDLE.getString("noTrackError"));
		}
		else {
			TierGlyph current_tier = (TierGlyph)current_tiers.get(0);
			saveAsFile(current_tier);
		}
	}

	private void saveAsFile(TierGlyph atier) {
		RootSeqSymmetry rootSym = (RootSeqSymmetry)atier.getViewModeGlyph().getInfo();
		AnnotationWriter annotationWriter;
		try {
			annotationWriter = annotationWriters.get(rootSym.getCategory()).newInstance();
		}
		catch (Exception x) {
			ErrorHandler.errorPanel("not supported yet", "cannot export files of type "
					+ rootSym.getCategory().name(), Level.WARNING);
			return;
		}
		if (annotationWriter == null) {		
			ErrorHandler.errorPanel("not supported yet", "cannot export files of type "
					+ rootSym.getCategory().name(), Level.WARNING);
			return;
		}
		String mimeType = annotationWriter.getMimeType();
		if (!mimeType.startsWith(MIME_TYPE_PREFIX)) {
			throw new UnsupportedOperationException("Export file can't handle mime type "
					+ mimeType);
		}
		String extension = mimeType.substring(MIME_TYPE_PREFIX.length());
		FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandler(extension);
		JFileChooser chooser = UniFileChooser.getFileChooser(fth.getName(), extension);
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
		
		int option = chooser.showSaveDialog(null);
		if (option == JFileChooser.APPROVE_OPTION) {
			FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
			BioSeq aseq = gmodel.getSelectedSeq();
			DataOutputStream dos = null;
			try {
				File fil = chooser.getSelectedFile();
				dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fil)));
				exportFile(annotationWriter, dos, aseq, atier);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Problem saving file", ex, Level.SEVERE);
			} finally {
				GeneralUtils.safeClose(dos);
			}
		}
	}

	protected abstract void exportFile(
			AnnotationWriter annotationWriter,
			DataOutputStream dos,
			BioSeq aseq,
			TierGlyph atier
			) throws java.io.IOException;
	
}
