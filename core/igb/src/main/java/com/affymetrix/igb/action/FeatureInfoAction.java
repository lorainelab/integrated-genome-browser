package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.GeneralUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public class FeatureInfoAction extends GenericAction {

    private static final long serialVersionUID = 1L;

    final String url;

    public FeatureInfoAction(String url) {
        super(BUNDLE.getString("trackInfo"), "16x16/actions/info.png", null);
        this.url = url;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        GeneralUtils.browse(url);
    }
}
