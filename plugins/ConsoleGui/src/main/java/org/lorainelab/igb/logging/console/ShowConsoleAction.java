package org.lorainelab.igb.logging.console;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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

@Component(name = ShowConsoleAction.COMPONENT_NAME, immediate = true, provide = {GenericAction.class, MenuBarEntryProvider.class})
public class ShowConsoleAction extends GenericAction implements MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ShowConsoleAction.class);
    public static final String COMPONENT_NAME = "ShowConsoleAction";
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private static final int CONSOLE_MENU_ITEM_WEIGHT = 10;
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
    public Optional<List<MenuItem>> getMenuItems() {
        MenuItem showConsoleActionMenuItem = new MenuItem(BUNDLE.getString("showConsole"), (Void t) -> {
            consoleGui.showConsole();
            return t;
        });
        try (InputStream resourceAsStream = ShowConsoleAction.class.getClassLoader().getResourceAsStream(SHOW_CONSOLE_ICON)) {
            showConsoleActionMenuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        showConsoleActionMenuItem.setWeight(CONSOLE_MENU_ITEM_WEIGHT);
        return Optional.of(Arrays.asList(showConsoleActionMenuItem));
    }
    private static final String SHOW_CONSOLE_ICON = "console.png";

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.HELP;
    }
}
