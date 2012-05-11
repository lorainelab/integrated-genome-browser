package com.affymetrix.igb.action;

import java.awt.event.KeyEvent;
import java.io.DataOutputStream;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.parsers.FileExporterI;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.shared.TierGlyph;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class ExportFileAction extends AbstractExportFileAction{
	private static final long serialVersionUID = 1L;
	private static final ExportFileAction ACTION = new ExportFileAction();
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ExportFileAction getAction() {
		return ACTION;
	}

	private ExportFileAction() {
		super(BUNDLE.getString("saveTrackAction"), null, "16x16/actions/document-save.png", "22x22/actions/document-save.png", KeyEvent.VK_UNDEFINED, null, true);
	}

	@Override
	protected void exportFile(FileExporterI fileExporter, DataOutputStream dos, BioSeq aseq, TierGlyph atier) throws java.io.IOException{
		fileExporter.exportFile(dos, (SeqSymmetry)atier.getViewModeGlyph().getInfo(), aseq);
	}
}
