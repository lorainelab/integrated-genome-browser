package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.parsers.AnnotationWriter;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.ExportFileModel;
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

    static {
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
        putValue(SHORT_DESCRIPTION, BUNDLE.getString("saveTrackActionTooltip"));
        this.ordinal = -9007100;
    }

  @Override
    protected void exportFile(AnnotationWriter annotationWriter, DataOutputStream dos, BioSeq aseq, TierGlyph atier) throws java.io.IOException {
        List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
        RootSeqSymmetry rootSeqSymmetry = (RootSeqSymmetry) atier.getInfo();
        if (rootSeqSymmetry.getCategory().isContainer()) {
            AnnotatedSeqGroup group = aseq.getSeqGroup();
            List<BioSeq> seql = group.getSeqList();
            for (BioSeq aseql : seql) {
                RootSeqSymmetry rootSym = aseql.getAnnotation(((TypeContainerAnnot)atier.getInfo()).getType());
                if (rootSym != null) {
                    syms = new ArrayList<SeqSymmetry>();
                    ExportFileModel.collectSyms(rootSym, syms, atier.getAnnotStyle().getGlyphDepth());
                    annotationWriter.writeAnnotations(syms, aseql, "", dos);
                }
            }
        } else {
            syms.add(rootSeqSymmetry);
            annotationWriter.writeAnnotations(syms, aseq, "", dos);

        }
    }
}
