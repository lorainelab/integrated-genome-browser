package org.lorainelab.igb.snp.convert.action;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.lorainelab.igb.menu.api.model.MenuBarParentMenu;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.snp.convert.ui.SnpConverterFrame;
import org.lorainelab.igb.menu.api.MenuBarEntryProvider;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true)
public class SnpConverterAction implements MenuBarEntryProvider {

    private static final int MENU_WEIGHT = 35;
    private SnpConverterFrame snpConverterFrame;

    @Reference
    public void setSnpConverterFrame(SnpConverterFrame snpConverterFrame) {
        this.snpConverterFrame = snpConverterFrame;
    }

    @Override
    public Optional<List<MenuItem>> getMenuItems() {
        MenuItem menuItem = new MenuItem("23andMe SNP Converter", (Void t) -> {
            snpConverterFrame.setVisible(true);
            return t;
        });
        menuItem.setWeight(MENU_WEIGHT);
        return Optional.of(Arrays.asList(menuItem));
    }

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.TOOLS;
    }
}
