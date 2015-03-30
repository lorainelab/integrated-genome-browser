package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.BugOrFeatureRequestForm;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;

/**
 *
 * @author sgblanch
 * @version $Id: ReportBugAction.java 9589 2011-12-20 15:54:10Z lfrohman $
 */
public class ReportBugActionNew extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final ReportBugActionNew ACTION = new ReportBugActionNew();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ReportBugActionNew getAction() {
        return ACTION;
    }

    private ReportBugActionNew() {
        super("Report a Bug", null,
                "16x16/actions/report_bug.png",
                "22x22/actions/report_bug.png",
                KeyEvent.VK_R, null, true);
        this.ordinal = 130;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame frame = new BugOrFeatureRequestForm(false);
        frame.setLocationRelativeTo(IGB.getInstance().getFrame());
        frame.setVisible(true);
    }
}
