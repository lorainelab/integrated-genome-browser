package com.affymetrix.igb.action;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.parsers.AnnotationWriter;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.IgbServiceImpl;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import java.awt.event.KeyEvent;
import java.io.DataOutputStream;
import java.util.List;

public class ExportSelectedAnnotationFileAction extends AbstractExportFileAction {

    private static final long serialVersionUID = 1L;
    private static final ExportSelectedAnnotationFileAction ACTION = new ExportSelectedAnnotationFileAction();

    static {
//		GenericActionHolder.getInstance().addGenericAction(ACTION);
        ACTION.setEnabled(false);
        GenometryModel.getInstance().addSymSelectionListener(ACTION);
    }

    public static ExportSelectedAnnotationFileAction getAction() {
        return ACTION;
    }

    private ExportSelectedAnnotationFileAction() {
        super(BUNDLE.getString("saveSelectedAnnotationAction"), null,
                "16x16/actions/save.png",
                "22x22/actions/save.png",
                KeyEvent.VK_UNDEFINED, null, true);
        putValue(SHORT_DESCRIPTION, BUNDLE.getString("saveSelectedAnnotationActionToolTip"));
    }

    @Override
    protected void exportFile(AnnotationWriter annotationWriter, DataOutputStream dos, BioSeq aseq, TierGlyph atier) throws java.io.IOException {
        annotationWriter.writeAnnotations(atier.getSelected(), aseq, "", dos);
    }

    @Override
    public void symSelectionChanged(SymSelectionEvent evt) {
        List<TierGlyph> answer = IgbServiceImpl.getInstance().getSelectedTierGlyphs();
        ExportSelectedAnnotationFileAction.getAction().setEnabled((1 == answer.size())
                && !(answer.get(0).getSelected().isEmpty())
                && isExportable(answer.get(0).getFileTypeCategory()));
    }
}
