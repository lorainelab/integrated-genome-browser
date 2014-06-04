/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.util.BugOrFeatureRequestForm;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;

/**
 *
 * @author auser
 */
public class RequestFeatureActionNew extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final RequestFeatureActionNew ACTION = new RequestFeatureActionNew();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static RequestFeatureActionNew getAction() {
        return ACTION;
    }

    private RequestFeatureActionNew() {
        super(BUNDLE.getString("requestAFeature"), null,
                "16x16/actions/mail-forward.png",
                "22x22/actions/mail-forward.png",
                KeyEvent.VK_R, null, true);
        this.ordinal = 140;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame frame = new BugOrFeatureRequestForm(true);
        frame.setLocationRelativeTo(IGB.getSingleton().getFrame());
        frame.setVisible(true);
    }
}
