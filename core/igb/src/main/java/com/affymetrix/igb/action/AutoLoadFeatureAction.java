package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;

/**
 *
 * @author hiralv
 */
public class AutoLoadFeatureAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final AutoLoadFeatureAction ACTION = new AutoLoadFeatureAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static AutoLoadFeatureAction getAction() {
        return ACTION;
    }

    private final JCheckBox autoloadCB;

    private AutoLoadFeatureAction() {
        super(null, null, null);
        autoloadCB = PreferenceUtils.createCheckBox(PreferenceUtils.AUTO_LOAD,
                PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load);
        autoloadCB.setToolTipText("Automatically load default features when available (e.g., cytoband and refseq)");
        autoloadCB.addActionListener(this);
    }

    public static JCheckBox getActionCB() {
        return ACTION.autoloadCB;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        GeneralLoadUtils.setFeatureAutoLoad(autoloadCB.isSelected());
    }
}
