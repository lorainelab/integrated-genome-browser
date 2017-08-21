package com.affymetrix.sequenceviewer;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.symmetry.SymWithResidues;
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

/*
*Deepti Joshi-IGBF-1139
*Added an additional condition in the if statements in symSelectionChanged method to check if the selection contains a single sequence. If it contains multiple sequences, 
*"View read sequence" is disabled.
*Also changed the structure of the if conditions.
*/

@Component(name = ViewReadSequenceInSeqViewerAction.COMPONENT_NAME, immediate = true, provide = {GenericAction.class, MenuBarEntryProvider.class})
public class ViewReadSequenceInSeqViewerAction extends GenericAction implements SymSelectionListener, MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ViewReadSequenceInSeqViewerAction.class);
    public static final String COMPONENT_NAME = "ViewReadSequenceInSeqViewerAction";
    private static final long serialVersionUID = 1L;
    private IgbService igbService;
    private ImageExportService imageExportService;
    private MenuItemEventService menuItemEventService;

    public ViewReadSequenceInSeqViewerAction() {
        super(AbstractSequenceViewer.BUNDLE.getString("ViewReadSequenceInSeqViewer"), null, "16x16/actions/Genome_Viewer_reads.png", "22x22/actions/Genome_Viewer_reads.png", KeyEvent.VK_UNDEFINED, null, false);
        this.setEnabled(false);
        menuItem = new MenuItem(AbstractSequenceViewer.BUNDLE.getString("ViewReadSequenceInSeqViewer"), (Void t) -> {
            actionPerformed(null);
            return t;
        });
        menuItem.setEnabled(false);
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
        super.actionPerformed(e);
        try {
            ReadSequenceViewer sv = new ReadSequenceViewer(igbService, imageExportService);
            sv.startSequenceViewer();
        } catch (Exception ex) {
            ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex, Level.WARNING);
        }
    }

    @Override
    public void symSelectionChanged(SymSelectionEvent evt) {
    //  Start---Deepti Joshi IGBF-1139
        setEnabled(false);
        menuItem.setEnabled(false);
        if (!evt.getSelectedGraphSyms().isEmpty() 
            && !(evt.getSelectedGraphSyms().get(0) instanceof GraphSym)
            && (evt.getSelectedGraphSyms().get(0) instanceof SymWithResidues)
            && (evt.getSelectedGraphSyms().size()==1)){
                setEnabled(true);
                menuItem.setEnabled(true);
            }  
    //  End---Deepti Joshi IGBF-1139
        MenuBarMenuItemEvent menuItemEvent = new MenuBarMenuItemEvent(menuItem, MenuBarParentMenu.VIEW);
        menuItemEventService.getEventBus().post(menuItemEvent);
    }

    @Override
    public Optional<List<MenuItem>> getMenuItems() {

        try (InputStream resourceAsStream = ViewReadSequenceInSeqViewerAction.class.getClassLoader().getResourceAsStream(SEQUENCE_VIEWER_ICON)) {
            menuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        menuItem.setWeight(MENU_WEIGHT);
        return Optional.of(Arrays.asList(menuItem));
    }
    private MenuItem menuItem;
    private static final int MENU_WEIGHT = 70;
    private static final String SEQUENCE_VIEWER_ICON = "Genome_Viewer_reads.png";

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.VIEW;
    }

    @Reference
    public void setImageExportService(ImageExportService imageExportService) {
        this.imageExportService = imageExportService;
    }
}
