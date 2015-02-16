package com.lorainelab.igb.preferences.weblink.action;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.lorainelab.igb.preferences.weblink.view.WebLinkDisplayProvider;
import com.lorainelab.igb.service.api.IgbMenuItemProvider;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.Action;
import javax.swing.JMenuItem;

/**
 * A panel for viewing and editing weblinks.
 */
@Component(name = WebLinksAction.COMPONENT_NAME, immediate = true, provide = {IgbMenuItemProvider.class, GenericAction.class})
public class WebLinksAction extends GenericAction implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "WebLinksAction";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private WebLinkDisplayProvider webLinkDisplayProvider;
    private JMenuItem webLinksMenuEntry;

    public WebLinksAction() {
        super(BUNDLE.getString("configureWebLinks"),
                "16x16/categories/applications-internet.png",
                "22x22/categories/applications-internet.png");
        putValue(Action.SHORT_DESCRIPTION, "Manage Web Links");
        webLinksMenuEntry = new JMenuItem(this);
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
    public String getParentMenuName() {
        return "tools";
    }

    @Override
    public JMenuItem getMenuItem() {
        return webLinksMenuEntry;
    }

    @Override
    public int getMenuItemPosition() {
        return -1;
    }

}
