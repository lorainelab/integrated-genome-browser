package com.affymetrix.sequenceviewer.actions;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.sequenceviewer.AbstractSequenceViewer;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class SelectAllInSeqViewerAction extends GenericAction {

    AbstractSequenceViewer sv = null;

    public SelectAllInSeqViewerAction(String text, AbstractSequenceViewer sv) {
        super(text, null, null, null, KeyEvent.VK_A);
        this.sv = sv;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (sv != null) {
            sv.selectAll();
        }
    }
}
