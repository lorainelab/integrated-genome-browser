package com.lorainelab.logging.console;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

@Component(name = ShowConsoleAction.COMPONENT_NAME, immediate = true, provide = {GenericAction.class, IgbMenuItemProvider.class})
public class ShowConsoleAction extends GenericAction implements IgbMenuItemProvider {
    
    public static final String COMPONENT_NAME = "ShowConsoleAction";
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private static final String PARENT_MENU_NAME = "help";
    private static final int CONSOLE_MENU_ITEM_WEIGHT = 5;
    private static final long serialVersionUID = 1l;
    private ConsoleLogger consoleGui;
    
    public ShowConsoleAction() {
        super(BUNDLE.getString("showConsole"), null,
                "16x16/actions/console.png",
                "22x22/actions/console.png",
                KeyEvent.VK_C, null, false);
        
        setKeyStrokeBinding("ctrl shift C");
    }
    
    @Activate
    public void activate() {
        this.ordinal = 150;
    }
    
    @Reference
    public void setConsoleGui(ConsoleLogger consoleGui) {
        this.consoleGui = consoleGui;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        consoleGui.showConsole();
    }
    
    @Override
    public String getParentMenuName() {
        return PARENT_MENU_NAME;
    }
    
    @Override
    public JRPMenuItem getMenuItem() {
        JRPMenuItem consoleMenuItem = new JRPMenuItem("showConsole", this, 5);
        return consoleMenuItem;
    }
    
    @Override
    public int getMenuItemWeight() {
        return CONSOLE_MENU_ITEM_WEIGHT;
    }
}
