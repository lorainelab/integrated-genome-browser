/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.feedback;

import org.osgi.service.component.annotations.Component;
import com.affymetrix.genometry.util.GeneralUtils;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import org.lorainelab.igb.menu.api.model.MenuBarParentMenu;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lorainelab.igb.menu.api.MenuBarEntryProvider;

/**
 *
 * @author dcnorris
 */
@Component(name = FeedbackMenuProvider.COMPONENT_NAME, immediate = true, service = MenuBarEntryProvider.class)
public class FeedbackMenuProvider implements MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(FeedbackMenuProvider.class);
    public static final String COMPONENT_NAME = "FeedbackMenuProvider";
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private static final int MENU_WEIGHT = 20;
    private static final String BIOVIZ_HELP_PAGE = "https://bioviz.org/help.html";

    @Override
    public Optional<List<MenuItem>> getMenuItems() {
        MenuItem menuItem = new MenuItem(BUNDLE.getString("menu.name"), (Void t) -> {
            GeneralUtils.openWebpage(BIOVIZ_HELP_PAGE);
            return t;
        });
        try (InputStream resourceAsStream = FeedbackMenuProvider.class.getClassLoader().getResourceAsStream(FEEDBACK_MENU_ICON)) {
            menuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        menuItem.setWeight(MENU_WEIGHT);
        return Optional.of(Arrays.asList(menuItem));
    }
    private static final String FEEDBACK_MENU_ICON = "help.png";

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.HELP;
    }

}
