package org.lorainelab.igb.image.exporter;

import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.ErrorHandler;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.lorainelab.igb.image.exporter.service.ImageExportService;
import org.lorainelab.igb.menu.api.model.MenuBarParentMenu;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.services.IgbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lorainelab.igb.menu.api.MenuBarEntryProvider;

/**
 *
 * @author nick
 */
@aQute.bnd.annotation.component.Component(name = SaveImageAction.COMPONENT_NAME, immediate = true, provide = {MenuBarEntryProvider.class, GenericAction.class})
public class SaveImageAction extends GenericAction implements MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SaveImageAction.class);
    public static final String COMPONENT_NAME = "SaveImageAction";
    private static final Logger logger = LoggerFactory.getLogger(SaveImageAction.class);
    private static final long serialVersionUID = 1L;
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private ImageExportService imageExportService;
    private final int TOOLBAR_INDEX = 4;
    private static final int MENU_POSITION = 17;
    private IgbService igbService;

    public SaveImageAction() {
        super(BUNDLE.getString("saveImage"), BUNDLE.getString("saveImageTooltip"),
                "16x16/actions/camera_toolbar.png",
                "22x22/actions/camera_toolbar.png",
                KeyEvent.VK_UNDEFINED, null, true);
        this.ordinal = -9002000;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        try {
            Map<String, Optional<Component>> compoToExport = new LinkedHashMap<>();
            compoToExport.put("Whole Frame", Optional.of(igbService.getApplicationFrame()));
            if (!igbService.getAllTierGlyphs().isEmpty()) {
                compoToExport.put("Main View", Optional.of(igbService.getMainViewComponent()));
            } else {
                compoToExport.put("Main View", Optional.empty());
            }
            if (!igbService.getAllTierGlyphs().isEmpty()) {
                compoToExport.put("Main View (with Labels)", Optional.of(igbService.getMainViewComponentWithLabels()));
            } else {
                compoToExport.put("Main View (with Labels)", Optional.empty());
            }
            if (!igbService.getSeqMapView().getSelectedSyms().isEmpty()) {
                compoToExport.put("Sliced View (with Labels)", Optional.of(igbService.getSpliceViewComponentWithLabels()));
            } else {
                compoToExport.put("Sliced View (with Labels)", Optional.empty());
            }
            imageExportService.exportComponents(compoToExport);
        } catch (Exception ex) {
            ErrorHandler.errorPanel("Problem during output.", ex, Level.SEVERE);
        }
    }

    @Reference
    public void setImageExportService(ImageExportService imageExportService) {
        this.imageExportService = imageExportService;
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Override
    public Optional<List<MenuItem>> getMenuItems() {
        MenuItem menuItem = new MenuItem(BUNDLE.getString("saveImage"), (Void t) -> {
            actionPerformed(null);
            return t;
        });
        try (InputStream resourceAsStream = SaveImageAction.class.getClassLoader().getResourceAsStream(SAVE_IMAGE_MENU_ICON)) {
            menuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        menuItem.setWeight(MENU_POSITION);
        return Optional.of(Arrays.asList(menuItem));
    }
    private static final String SAVE_IMAGE_MENU_ICON = "camera_toolbar.png";

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.FILE;
    }

    @Override
    public boolean isToolbarDefault() {
        return true;
    }

    @Override
    public int getToolbarIndex() {
        return TOOLBAR_INDEX;
    }
}
