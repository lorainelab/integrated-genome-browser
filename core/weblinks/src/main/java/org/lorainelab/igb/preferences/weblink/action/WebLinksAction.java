package org.lorainelab.igb.preferences.weblink.action;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.swing.Action;
import javax.swing.JMenuItem;
import org.lorainelab.igb.menu.api.model.MenuBarParentMenu;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.preferences.weblink.view.WebLinkDisplayProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lorainelab.igb.menu.api.MenuBarEntryProvider;

/**
 * A panel for viewing and editing weblinks.
 */
@Component(name = WebLinksAction.COMPONENT_NAME, immediate = true, provide = {MenuBarEntryProvider.class, GenericAction.class})
public class WebLinksAction extends GenericAction implements MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(WebLinksAction.class);
    private static final String SEARCH_WEB_ICONPATH = "searchweb.png";
    public static final String COMPONENT_NAME = "WebLinksAction";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private WebLinkDisplayProvider webLinkDisplayProvider;
    private JMenuItem webLinksMenuEntry;
    private static final int MENU_ITEM_WEIGHT = 35;

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
    public Optional<List<MenuItem>> getMenuItems() {
        MenuItem menuItem = new MenuItem(BUNDLE.getString("configureWebLinks"), (Void t) -> {
            actionPerformed(null);
            return t;
        });
        try (InputStream resourceAsStream = WebLinksAction.class.getClassLoader().getResourceAsStream(SEARCH_WEB_ICONPATH)) {
            menuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        menuItem.setWeight(MENU_ITEM_WEIGHT);
        return Optional.of(Arrays.asList(menuItem));
    }

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.TOOLS;
    }

}
