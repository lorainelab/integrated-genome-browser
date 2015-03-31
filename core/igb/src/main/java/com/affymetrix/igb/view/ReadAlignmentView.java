package com.affymetrix.igb.view;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.event.ContextualPopupListener;
import com.affymetrix.genometry.symloader.BAM;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.igb.action.ViewReadAlignmentAction;
import com.affymetrix.igb.swing.JRPMenuItem;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 * @author hiralv
 */
public class ReadAlignmentView implements ContextualPopupListener {

    public static final String COMPONENT_NAME = "ReadAlignmentView";
    private static final int MENU_WEIGHT = 25;

    @Override
    public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_items, SeqSymmetry primary_sym) {
        if (!selected_items.isEmpty() && selected_items.get(0) instanceof SymWithProps) {
            SymWithProps swp = (SymWithProps) selected_items.get(0);
            Object prop = swp.getProperty(BAM.SHOWMASK);
            if (prop != null) {
                if (selected_items.size() == 1) {
                    if (Boolean.parseBoolean(prop.toString())) {
                        JRPMenuItem menu = new JRPMenuItem("", ViewReadAlignmentAction.getReadRestoreAction(selected_items), MENU_WEIGHT);
                        BioSeq seq = GenometryModel.getInstance().getSelectedSeq();
                        SeqSpan span = selected_items.get(0).getSpan(seq);
                        menu.setEnabled(seq.isComplete(span.getMin(), span.getMax()));
                        popup.add(menu);
                    } else {
                        popup.add(new JMenuItem(ViewReadAlignmentAction.getMismatchAligmentAction(selected_items)));
                    }
                } else {
                    popup.add(new JMenuItem(ViewReadAlignmentAction.getReadRestoreAction(selected_items)));
                    popup.add(new JMenuItem(ViewReadAlignmentAction.getMismatchAligmentAction(selected_items)));
                }
            }
        }
    }

}
