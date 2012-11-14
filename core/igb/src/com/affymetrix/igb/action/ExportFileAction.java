package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.parsers.AnnotationWriter;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.SeqUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.TierGlyph;
import java.awt.event.KeyEvent;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExportFileAction
extends AbstractExportFileAction {
	private static final long serialVersionUID = 1L;
	private static final ExportFileAction ACTION = new ExportFileAction();
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		ACTION.setEnabled(false);
		GenometryModel.getGenometryModel().addSymSelectionListener(ACTION);
	}
	
	public static ExportFileAction getAction() {
		return ACTION;
	}

	private ExportFileAction() {
		super(BUNDLE.getString("saveTrackAction"), null,
				"16x16/actions/save.png",
				"22x22/actions/save.png",
				KeyEvent.VK_UNDEFINED, null, true);
		this.ordinal = -9007100;
	}

	@Override
	protected void exportFile(AnnotationWriter annotationWriter, DataOutputStream dos, BioSeq aseq, TierGlyph atier) throws java.io.IOException{
		RootSeqSymmetry rootSym = (RootSeqSymmetry)atier.getInfo();
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		if (rootSym.getCategory().isContainer()) {
			collectSyms(rootSym, syms, atier.getAnnotStyle().getGlyphDepth());
		}
		else {
			syms.add(rootSym);
		}
		annotationWriter.writeAnnotations(syms, aseq, "", dos);
	}
	
	protected static void collectSyms(SeqSymmetry sym, List<SeqSymmetry> syms, int desired_leaf_depth){
		int depth = SeqUtils.getDepthFor(sym);
		if (depth > desired_leaf_depth || sym instanceof TypeContainerAnnot) {
			int childCount = sym.getChildCount();
			for (int i = 0; i < childCount; i++) {
				collectSyms(sym.getChild(i), syms, desired_leaf_depth);
			}
		} else {  // depth == desired_leaf_depth
			syms.add(sym);
		}
	}
}
