package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.ParameteredAction;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JOptionPane;

public class RenameTierAction extends SeqMapViewActionA implements ParameteredAction {

    private static final long serialVersionUID = 1L;
    private static final RenameTierAction ACTION = new RenameTierAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static RenameTierAction getAction() {
        return ACTION;
    }

    private RenameTierAction() {
        super(BUNDLE.getString("renameAction"), "16x16/actions/blank_placeholder.png", null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        List<TierGlyph> current_tiers = getTierManager().getSelectedTiers();
        if (current_tiers.size() != 1) {
            ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
        }
        if (current_tiers.get(0) == null) {
            return;
        }

        ITrackStyleExtended style = current_tiers.get(0).getAnnotStyle();
        setNewName(style, JOptionPane.showInputDialog(BUNDLE.getString("label") + ": ", style.getTrackName()));

        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    public void performAction(Object... parameters) {
        if (parameters.length < 2
                || parameters[1].getClass() != String.class
                || !(parameters[0] instanceof ITrackStyleExtended)) {
            return;
        }

        setNewName((ITrackStyleExtended) parameters[0], (String) parameters[1]);
    }

    private void setNewName(ITrackStyleExtended style, String new_label) {
        boolean renamed = renameTier(style, new_label);
        if (renamed) {
            update();
        }
    }

    private boolean renameTier(ITrackStyleExtended style, String new_label) {
        if (new_label != null && new_label.length() > 0) {
            style.setTrackName(new_label);
            return true;
        }
        return false;
    }

    private void update() {
        getTierMap().getLabelMap().updateWidget();
    }
}
