package com.lorainelab.igb.preferences.weblink.action;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.lorainelab.igb.preferences.weblink.view.WebLinkDisplayProvider;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import com.lorainelab.igb.services.window.menus.IgbToolBarParentMenu;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.Action;

/**
 * A panel for viewing and editing weblinks.
 */
@Component(name = WebLinksAction.COMPONENT_NAME, immediate = true, provide = {IgbMenuItemProvider.class, GenericAction.class})
public class WebLinksAction extends GenericAction implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "WebLinksAction";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private WebLinkDisplayProvider webLinkDisplayProvider;
    private JRPMenuItem webLinksMenuEntry;
    private static final int MENU_ITEM_WEIGHT = 7;

    public WebLinksAction() {
        super(BUNDLE.getString("configureWebLinks"),
                "16x16/categories/applications-internet.png",
                "22x22/categories/applications-internet.png");
        putValue(Action.SHORT_DESCRIPTION, "Manage Web Links");
        webLinksMenuEntry = new JRPMenuItem("IGB_PLUGIN" + COMPONENT_NAME, this, getMenuItemWeight());
    }

    @Reference(optional = false)
    public void setWebLinksViewGUI(WebLinkDisplayProvider webLinkDisplayProvider) {
        this.webLinkDisplayProvider = webLinkDisplayProvider;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        super.actionPerformed(evt);
        webLinkDisplayProvider.displayPanel();
    }

    @Override
    public IgbToolBarParentMenu getParentMenu() {
        return IgbToolBarParentMenu.TOOLS;
    }

    @Override
    public JRPMenuItem getMenuItem() {
        return webLinksMenuEntry;
    }

    @Override
    public int getMenuItemWeight() {
        return MENU_ITEM_WEIGHT;
    }
    

}
