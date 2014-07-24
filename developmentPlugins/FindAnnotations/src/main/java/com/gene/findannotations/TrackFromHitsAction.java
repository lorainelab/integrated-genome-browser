package com.gene.findannotations;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.JTextField;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.igb.osgi.service.IGBService;

public class TrackFromHitsAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final String TYPE = FindAnnotationsView.BUNDLE.getString("findannotationsTrackFromHits");
    private final IGBService igbService;
    private final JTextField textField;
    private final AnnotationsTableModel tableModel;

    public TrackFromHitsAction(IGBService igbService, JTextField textField, AnnotationsTableModel tableModel) {
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
        BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
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
