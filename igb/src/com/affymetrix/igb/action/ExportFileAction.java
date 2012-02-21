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
import com.affymetrix.genometryImpl.parsers.AnnotationFileExporter;
import com.affymetrix.genometryImpl.parsers.FileExporterI;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.parsers.GraphFileExporter;
import com.affymetrix.genometryImpl.parsers.MismatchFileExporter;
import com.affymetrix.genometryImpl.parsers.SequenceFileExporter;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TierGlyphViewMode;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class ExportFileAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final ExportFileAction ACTION = new ExportFileAction();
	private static final Map<FileTypeCategory, FileExporterI> fileExporters = new HashMap<FileTypeCategory, FileExporterI>();
	static {
		fileExporters.put(FileTypeCategory.Annotation, new AnnotationFileExporter());
		fileExporters.put(FileTypeCategory.Alignment, new AnnotationFileExporter());
		fileExporters.put(FileTypeCategory.Graph, new GraphFileExporter());
		fileExporters.put(FileTypeCategory.Sequence, new SequenceFileExporter());
//		fileExporters.put(FileTypeCategory.Variant, null);
		fileExporters.put(FileTypeCategory.Mismatch, new MismatchFileExporter());
	}
	public static ExportFileAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		List<Glyph> current_tiers = IGBServiceImpl.getInstance().getSelectedTierGlyphs();
		if (current_tiers.size() > 1) {
			ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
		}
		TierGlyph current_tier = (TierGlyph)current_tiers.get(0);
		saveAsFile(current_tier);
	}

	private void saveAsFile(TierGlyph atier) {
		if (!(atier instanceof TierGlyphViewMode)) {
			return;
		}
		RootSeqSymmetry rootSym = (RootSeqSymmetry)((TierGlyphViewMode)atier).getViewModeGlyph().getInfo();
		FileExporterI fileExporter = fileExporters.get(rootSym.getCategory());
		String extension = fileExporter.getFileExtension();
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
				fileExporter.exportFile(dos, (SeqSymmetry)rootSym, aseq);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Problem saving file", ex);
			} finally {
				GeneralUtils.safeClose(dos);
			}
		}
	}

	@Override
	public String getText() {
		return BUNDLE.getString("saveTrackAction");
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
