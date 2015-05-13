package com.gene.findannotations;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.lorainelab.igb.services.IgbService;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import javax.swing.JTextField;

public class TrackFromHitsAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final String TYPE = FindAnnotationsView.BUNDLE.getString("findannotationsTrackFromHits");
    private final IgbService igbService;
    private final JTextField textField;
    private final AnnotationsTableModel tableModel;

    public TrackFromHitsAction(IgbService igbService, JTextField textField, AnnotationsTableModel tableModel) {
        super(null, null, null);
        this.igbService = igbService;
        this.textField = textField;
        this.tableModel = tableModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        String type = MessageFormat.format(TYPE, textField.getText());
        TypeContainerAnnot containerSym = new TypeContainerAnnot(type);
        final GenometryModel gmodel = GenometryModel.getInstance();
        BioSeq seq = gmodel.getSelectedSeq().orElse(null);
        // copy children
        for (SeqSymmetry sym : tableModel.getResults()) {
            if (sym.getSpan(seq) != null) {
                containerSym.addChild(sym);
                containerSym.addSpan(sym.getSpan(0));
            }
        }
        if (containerSym.getChildCount() == 0) {
            igbService.setStatus(MessageFormat.format(FindAnnotationsView.BUNDLE.getString("findannotationsNoData"), seq.toString()));
        } else {
            igbService.addTrack(containerSym, type);
        }
    }
}
