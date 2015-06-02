package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.logging.Level;
import javax.swing.SwingWorker;

/**
 * note !!! - depending on the script, it may not be possible to cancel it
 */
@Component(name = CancelScriptAction.COMPONENT_NAME, immediate = true, provide = {IgbMenuItemProvider.class, GenericAction.class})
public class CancelScriptAction extends GenericAction implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "CancelScriptAction";
    private static final long serialVersionUID = 1L;

    public CancelScriptAction() {
        super(BUNDLE.getString("cancelScript"), null, "16x16/actions/cancel_script.png", "22x22/actions/cancel_script.png", KeyEvent.VK_X);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        final IGB igb = ((IGB) IGB.getInstance());
        synchronized (igb) {
            SwingWorker<Void, Void> igbScriptWorker = igb.getScriptWorker();
            if (igbScriptWorker == null) {
                ErrorHandler.errorPanel("script error", "no script is running", Level.SEVERE);
            } else {
                igbScriptWorker.cancel(true);
                igb.setScriptWorker(null);
            }
        }
    }

    @Override
    public JRPMenuItem getMenuItem() {
        return new JRPMenuItem("Scripts_CancelScript", this);
    }

    @Override
    public int getMenuItemWeight() {
        return 8;
    }

    @Override
    public String getParentMenuName() {
        return "tools";
    }

    @Override
    public Optional<String> getSubMenuName() {
        return Optional.of("scripts"); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
