package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class ViewReadAlignmentAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1l;

    private static final String RESTOREREAD = BUNDLE.getString("restoreAlignment");
    private static final String SHOWMISMATCH = BUNDLE.getString("showMismatch");

    private static final ViewReadAlignmentAction restoreRead = new ViewReadAlignmentAction(RESTOREREAD);
    private static final ViewReadAlignmentAction showMismatch = new ViewReadAlignmentAction(SHOWMISMATCH);

    private final List<SeqSymmetry> syms = new ArrayList<>();

    private ViewReadAlignmentAction(String text) {
        super(text, null, null);
    }

    public static ViewReadAlignmentAction getReadRestoreAction(List<SeqSymmetry> syms) {
        return getAction(restoreRead, syms);
    }

    public static ViewReadAlignmentAction getMismatchAligmentAction(List<SeqSymmetry> syms) {
        return getAction(showMismatch, syms);
    }

    private static ViewReadAlignmentAction getAction(ViewReadAlignmentAction action, List<SeqSymmetry> syms) {
        action.syms.clear();
        action.syms.addAll(syms);
        return action;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);

        boolean set = true;

        if (RESTOREREAD.equals(e.getActionCommand())) {
            set = false;
        }

        for (SeqSymmetry sym : syms) {
            if (sym instanceof SymWithProps) {
                SymWithProps swp = (SymWithProps) sym;
                if (swp.getProperty(BAM.SHOWMASK) != null) {
                    swp.setProperty(BAM.SHOWMASK, set);
                }
            }
        }

        getSeqMapView().getSeqMap().repaint();
    }
}
