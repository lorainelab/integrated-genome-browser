package com.lorainelab.image.exporter;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nick
 */
@Component(name = SaveImageAction.COMPONENT_NAME, immediate = true, provide = {IgbMenuItemProvider.class, GenericAction.class})
public class SaveImageAction extends GenericAction implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "SaveImageAction";
    private static final Logger logger = LoggerFactory.getLogger(SaveImageAction.class);
    private static final long serialVersionUID = 1L;
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private ExportDialog exportDialog;
    private final int TOOLBAR_INDEX = 4;
    private static final int MENU_POSITION = 5;

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
            exportDialog.display(false);
        } catch (Exception ex) {
            ErrorHandler.errorPanel("Problem during output.", ex, Level.SEVERE);
        }
    }

    @Reference
    public void setExportDialog(ExportDialog exportDialog) {
        this.exportDialog = exportDialog;
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
