package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

/**
 *
 * @author sgblanch
 * @version $Id: ExitAction.java 11358 2012-05-02 13:28:22Z anuj4159 $
 */
public class ExitAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final ExitAction ACTION = new ExitAction();
    private final int TOOLBAR_INDEX = 7;

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ExitAction getAction() {
        return ACTION;
    }

    private ExitAction() {
        super(BUNDLE.getString("exit"), BUNDLE.getString("exitTooltip"),
                "16x16/actions/process-stop.png",
                "22x22/actions/process-stop.png",
                KeyEvent.VK_X);
        this.ordinal = -9009000;
        setKeyStrokeBinding("ctrl Q");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                new WindowEvent(
                        IGB.getInstance().getFrame(),
                        WindowEvent.WINDOW_CLOSING));
    }
    
    @Override
    public boolean isToolbarDefault() {
        return true; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getToolbarIndex() {
        return TOOLBAR_INDEX; //To change body of generated methods, choose Tools | Templates.
    }
}
