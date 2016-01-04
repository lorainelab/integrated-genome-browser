package com.affymetrix.sequenceviewer;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.AxisPopupListener;
import com.affymetrix.genometry.event.ContextualPopupListener;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import org.lorainelab.igb.igb.services.window.menus.IgbMenuItemProvider;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 * @author hiralv
 */
@Component(name = PopupListener.COMPONENT_NAME, immediate = true, provide = {ContextualPopupListener.class, AxisPopupListener.class})
public class PopupListener implements ContextualPopupListener, AxisPopupListener {

    public static final String COMPONENT_NAME = "PopupListener";
    JMenuItem genomicSequenceViewer, readSequenceViewer;

    public PopupListener() {
    }

    @Override
    public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_syms, SeqSymmetry primary_sym) {
        if (!selected_syms.isEmpty() && !(selected_syms.get(0) instanceof GraphSym)) {
            popup.add(genomicSequenceViewer, 8);
            popup.add(readSequenceViewer, 10);
        }
    }

    @Override
    public void addPopup(JPopupMenu popup) {
        popup.add(genomicSequenceViewer);
    }

    @Reference(optional = false, target = "(&(component.name=ViewGenomicSequenceInSeqViewerAction))")
    public void setGenomicSequenceViewer(IgbMenuItemProvider genomicSequenceViewer) {
        this.genomicSequenceViewer = genomicSequenceViewer.getMenuItem();
    }

    @Reference(optional = false, target = "(&(component.name=ViewReadSequenceInSeqViewerAction))")
    public void setReadSequenceViewer(IgbMenuItemProvider readSequenceViewer) {
        this.readSequenceViewer = readSequenceViewer.getMenuItem();
    }

}
