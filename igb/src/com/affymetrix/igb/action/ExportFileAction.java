package com.affymetrix.igb.action;

import java.io.DataOutputStream;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileExporterI;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.shared.TierGlyph;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class ExportFileAction extends AbstractExportFileAction{
	private static final long serialVersionUID = 1L;
	private static final ExportFileAction ACTION = new ExportFileAction();
	
	public static ExportFileAction getAction() {
		return ACTION;
	}
				
	@Override
	protected void exportFile(FileExporterI fileExporter, DataOutputStream dos, BioSeq aseq, TierGlyph atier) throws java.io.IOException{
		fileExporter.exportFile(dos, (SeqSymmetry)atier.getViewModeGlyph().getInfo(), aseq);
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
