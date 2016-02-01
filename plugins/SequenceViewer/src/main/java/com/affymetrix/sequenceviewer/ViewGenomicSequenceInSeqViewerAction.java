package com.affymetrix.sequenceviewer;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.util.ErrorHandler;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.lorainelab.igb.image.exporter.service.ImageExportService;
import org.lorainelab.igb.menu.api.MenuItemEventService;
import org.lorainelab.igb.menu.api.model.MenuBarMenuItemEvent;
import org.lorainelab.igb.menu.api.model.MenuBarParentMenu;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.services.IgbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lorainelab.igb.menu.api.MenuBarEntryProvider;

@Component(name = ViewGenomicSequenceInSeqViewerAction.COMPONENT_NAME, immediate = true, provide = {GenericAction.class, MenuBarEntryProvider.class})
public class ViewGenomicSequenceInSeqViewerAction extends GenericAction implements SymSelectionListener, MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ViewGenomicSequenceInSeqViewerAction.class);
    public static final String COMPONENT_NAME = "ViewGenomicSequenceInSeqViewerAction";
    private static final long serialVersionUID = 1L;
    private IgbService igbService;
    private ImageExportService imageExportService;
    private MenuItemEventService menuItemEventService;

    public ViewGenomicSequenceInSeqViewerAction() {
        super(AbstractSequenceViewer.BUNDLE.getString("ViewGenomicSequenceInSeqViewer"), null, "16x16/actions/Sequence_Viewer.png", "22x22/actions/Sequence_Viewer.png", KeyEvent.VK_UNDEFINED, null, false);
        setEnabled(false);
        menuItem = new MenuItem(AbstractSequenceViewer.BUNDLE.getString("ViewGenomicSequenceInSeqViewer"), (Void t) -> {
            actionPerformed(null);
            return t;
        });
        menuItem.setEnabled(false);
    }

    @Activate
    public void activate() {
        GenometryModel.getInstance().addSymSelectionListener(this);
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Reference
    public void setMenuItemEventService(MenuItemEventService menuItemEventService) {
        this.menuItemEventService = menuItemEventService;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            DefaultSequenceViewer sv = new DefaultSequenceViewer(igbService, imageExportService);
            sv.startSequenceViewer();
        } catch (Exception ex) {
            ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex, Level.WARNING);
        }
    }

    @Override
    public void symSelectionChanged(SymSelectionEvent evt) {
        if ((evt.getSelectedGraphSyms().isEmpty() && igbService.getSeqMapView().getSeqSymmetry() == null)
                || (!evt.getSelectedGraphSyms().isEmpty() && evt.getSelectedGraphSyms().get(0) instanceof GraphSym)) {
            setEnabled(false);
            menuItem.setEnabled(false);
        } else {
            setEnabled(true);
            menuItem.setEnabled(true);
        }
        MenuBarMenuItemEvent menuItemEvent = new MenuBarMenuItemEvent(menuItem, MenuBarParentMenu.VIEW);
        menuItemEventService.getEventBus().post(menuItemEvent);
    }

    @Override
    public Optional<List<MenuItem>> getMenuItems() {

        try (InputStream resourceAsStream = ViewGenomicSequenceInSeqViewerAction.class.getClassLoader().getResourceAsStream(SEQUENCE_VIEWER_ICON)) {
            menuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        menuItem.setWeight(MENU_WEIGHT);
        return Optional.of(Arrays.asList(menuItem));
    }
    private MenuItem menuItem;
    private static final int MENU_WEIGHT = 65;
    private static final String SEQUENCE_VIEWER_ICON = "Sequence_Viewer.png";

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.VIEW;
    }

    @Reference
    public void setImageExportService(ImageExportService imageExportService) {
        this.imageExportService = imageExportService;
    }

}
