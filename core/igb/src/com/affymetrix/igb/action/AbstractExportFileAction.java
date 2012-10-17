package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.parsers.AnnotationWriter;
import com.affymetrix.genometryImpl.parsers.BedDetailWriter;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symloader.Fasta;
import com.affymetrix.genometryImpl.symloader.BedGraph;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GFileChooser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.shared.TierGlyph;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public abstract class AbstractExportFileAction
extends GenericAction implements SymSelectionListener {
	private static final long serialVersionUID = 1l;
	private static final String MIME_TYPE_PREFIX = "text/";
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();

	private static final Map<FileTypeCategory, List<Class<? extends AnnotationWriter>>> annotationWriters
			= new HashMap<FileTypeCategory, List<Class<? extends AnnotationWriter>>>();
	static {
		List<Class<? extends AnnotationWriter>> annotationList = new ArrayList<Class<? extends AnnotationWriter>>();
		annotationList.add(BedDetailWriter.class);
		annotationList.add(BedParser.class);
		annotationWriters.put(FileTypeCategory.Annotation, annotationList);
		
		annotationWriters.put(FileTypeCategory.Alignment, annotationList);
				
		List<Class<? extends AnnotationWriter>> graphList = new ArrayList<Class<? extends AnnotationWriter>>();
		graphList.add(BedGraph.class);
		annotationWriters.put(FileTypeCategory.Graph, graphList);
		
		List<Class<? extends AnnotationWriter>> sequenceList = new ArrayList<Class<? extends AnnotationWriter>>();
		sequenceList.add(Fasta.class);
		annotationWriters.put(FileTypeCategory.Sequence, sequenceList);
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
	 * Note that this must match {@link #actionPerformed(ActionEvent)} 
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
		RootSeqSymmetry rootSym = (RootSeqSymmetry)atier.getInfo();
		List<Class<? extends AnnotationWriter>> availableWriters = annotationWriters.get(rootSym.getCategory());
		if (availableWriters == null || availableWriters.size() == 0) {		
			ErrorHandler.errorPanel("not supported yet", "cannot export files of type "
					+ rootSym.getCategory().name(), Level.WARNING);
			return;
		}
		
		JFileChooser chooser = new GFileChooser();
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setMultiSelectionEnabled(false);
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
		
		Map<UniFileFilter, AnnotationWriter> filter2writers = new HashMap<UniFileFilter, AnnotationWriter>();
		for (Class<? extends AnnotationWriter> clazz : availableWriters) {
			try {
				AnnotationWriter writer = clazz.newInstance();
				String mimeType = writer.getMimeType();
				if (mimeType.startsWith(MIME_TYPE_PREFIX)) {
					String extension = mimeType.substring(MIME_TYPE_PREFIX.length());
					FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandler(extension);
					
					// Hack to display as bed detail
					String name = clazz == BedDetailWriter.class ? "BED Detail" : fth.getName();
					
					UniFileFilter filter = new UniFileFilter(extension, name);
					chooser.addChoosableFileFilter(filter);
					filter2writers.put(filter, writer);
				}else{
					Logger.getLogger(AbstractExportFileAction.class.getName()).log(
						Level.WARNING, "Export file can't handle mime type {0}"
							 ,mimeType);
				}
			} catch (Exception ex) {
				Logger.getLogger(AbstractExportFileAction.class.getName()).log(
						Level.WARNING, "Couldn't not initiate class for {0} due to exception {1}",
						new Object[]{clazz, ex.getMessage()});
			}
		}
		
		if (filter2writers.size() == 0) {		
			ErrorHandler.errorPanel("not supported yet", "cannot export files of type "
					+ rootSym.getCategory().name(), Level.WARNING);
			return;
		}
		
		
		int option = chooser.showSaveDialog(null);
		if (option == JFileChooser.APPROVE_OPTION) {
			FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
			BioSeq aseq = gmodel.getSelectedSeq();
			DataOutputStream dos = null;
			try {
				File fil = chooser.getSelectedFile();
				dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fil)));
				exportFile(filter2writers.get(chooser.getFileFilter()), dos, aseq, atier);
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
