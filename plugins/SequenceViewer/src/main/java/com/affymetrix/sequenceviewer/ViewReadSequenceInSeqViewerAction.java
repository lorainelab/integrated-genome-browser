package com.affymetrix.sequenceviewer;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.symmetry.SymWithResidues;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;

import com.affymetrix.genometryImpl.util.ErrorHandler;

import com.affymetrix.igb.osgi.service.IGBService;

public class ViewReadSequenceInSeqViewerAction extends GenericAction implements SymSelectionListener {

    private static final long serialVersionUID = 1l;
    private IGBService igbService;

    public ViewReadSequenceInSeqViewerAction(IGBService igbService) {
        super(AbstractSequenceViewer.BUNDLE.getString("ViewReadSequenceInSeqViewer"), null, "16x16/actions/Genome_Viewer_reads.png", "22x22/actions/Genome_Viewer_reads.png", KeyEvent.VK_UNDEFINED, null, false);
        this.setEnabled(false);
        GenometryModel.getInstance().addSymSelectionListener(this);
        this.igbService = igbService;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        try {
            ReadSequenceViewer sv = new ReadSequenceViewer(igbService);
            sv.startSequenceViewer();
        } catch (Exception ex) {
            ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex, Level.WARNING);
        }
    }

    public void symSelectionChanged(SymSelectionEvent evt) {
        if (!evt.getSelectedGraphSyms().isEmpty() && !(evt.getSelectedGraphSyms().get(0) instanceof GraphSym)) {
            if (evt.getSelectedGraphSyms().get(0) instanceof SymWithResidues) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        } else {
            setEnabled(false);
        }
    }
}
