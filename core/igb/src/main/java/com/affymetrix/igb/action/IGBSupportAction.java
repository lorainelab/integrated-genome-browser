/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import static com.affymetrix.common.CommonUtils.APP_NAME;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.GeneralUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;

/**
 *
 * @author tkanapar
 */
public class IGBSupportAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final IGBSupportAction ACTION = new IGBSupportAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static IGBSupportAction getAction() {
        return ACTION;
    }

    private IGBSupportAction() {
        super(MessageFormat.format(BUNDLE.getString("igbSupport"), APP_NAME),
                null, "16x16/actions/help.png",
                "22x22/actions/help.png",
                KeyEvent.VK_UNDEFINED, null, false);
        this.ordinal = 120;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        GeneralUtils.browse("http://bioviz.org/igb/contact.html");
    }
}
