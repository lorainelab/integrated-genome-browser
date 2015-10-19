package com.lorainelab.image.exporter;

import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import com.lorainelab.image.exporter.service.ImageExportService;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nick
 */
@aQute.bnd.annotation.component.Component(name = SaveImageAction.COMPONENT_NAME, immediate = true, provide = {IgbMenuItemProvider.class, GenericAction.class})
public class SaveImageAction extends GenericAction implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "SaveImageAction";
    private static final Logger logger = LoggerFactory.getLogger(SaveImageAction.class);
    private static final long serialVersionUID = 1L;
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private ImageExportService imageExportService;
    private final int TOOLBAR_INDEX = 4;
    private static final int MENU_POSITION = 5;
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
//            exportDialog.display();
            Map<String, Component> compoToExport = new LinkedHashMap<>();
            compoToExport.put("Whole Frame", igbService.getApplicationFrame());
            compoToExport.put("Main View", igbService.getMainViewComponent());
            compoToExport.put("Main View (with Labels)", igbService.getMainViewComponentWithLabels());
            compoToExport.put("Sliced View (with Labels)", igbService.getSpliceViewComponentWithLabels());
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
    public String getParentMenuName() {
        return "file";
    }

    @Override
    public JRPMenuItem getMenuItem() {
        return new JRPMenuItem(BUNDLE.getString("saveImage"), this, getMenuItemWeight());
    }

    @Override
    public int getMenuItemWeight() {
        return MENU_POSITION;
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
