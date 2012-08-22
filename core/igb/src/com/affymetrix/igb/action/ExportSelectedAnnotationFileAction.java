package com.affymetrix.igb.action;

import java.awt.event.KeyEvent;
import java.io.DataOutputStream;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.parsers.AnnotationWriter;
import com.affymetrix.igb.shared.TierGlyph;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class ExportSelectedAnnotationFileAction extends AbstractExportFileAction{
	private static final long serialVersionUID = 1L;
	private static final ExportSelectedAnnotationFileAction ACTION = new ExportSelectedAnnotationFileAction();
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ExportSelectedAnnotationFileAction getAction() {
		return ACTION;
	}
				
	private ExportSelectedAnnotationFileAction() {
		super(BUNDLE.getString("saveSelectedAnnotationAction"), null, null, null, KeyEvent.VK_UNDEFINED, null, true);
	}

	@Override
	protected void exportFile(AnnotationWriter annotationWriter, DataOutputStream dos, BioSeq aseq, TierGlyph atier) throws java.io.IOException{
		annotationWriter.writeAnnotations(atier.getSelected(), aseq, "", dos);
	}
}
