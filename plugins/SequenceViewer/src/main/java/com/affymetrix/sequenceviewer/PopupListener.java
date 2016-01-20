package com.affymetrix.sequenceviewer;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.AxisPopupListener;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.google.common.collect.Lists;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.lorainelab.igb.context.menu.AnnotationContextMenuProvider;
import org.lorainelab.igb.context.menu.MenuSection;
import org.lorainelab.igb.context.menu.model.AnnotationContextEvent;
import org.lorainelab.igb.context.menu.model.ContextMenuItem;
import org.lorainelab.igb.context.menu.model.MenuIcon;
import org.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hiralv
 */
@Component(immediate = true, provide = {AnnotationContextMenuProvider.class, AxisPopupListener.class})
public class PopupListener implements AnnotationContextMenuProvider, AxisPopupListener {

    private static final Logger LOG = LoggerFactory.getLogger(PopupListener.class);
    JMenuItem genomicSequenceViewer, readSequenceViewer;
    private final static String SEQUENCE_VIEWER_ICONPATH = "Sequence_Viewer.png";

    public PopupListener() {
    }

    @Override
    public void addPopup(JPopupMenu popup) {
        popup.add(genomicSequenceViewer);
    }

    @Reference(optional = false, target = "(&(component.name=ViewGenomicSequenceInSeqViewerAction))")
    public void setGenomicSequenceViewer(IgbMenuItemProvider genomicSequenceViewer) {
        this.genomicSequenceViewer = genomicSequenceViewer.getMenuItem();
    }

    @Reference(optional = false, target = "(&(component.name=ViewReadSequenceInSeqViewerAction))")
    public void setReadSequenceViewer(IgbMenuItemProvider readSequenceViewer) {
        this.readSequenceViewer = readSequenceViewer.getMenuItem();
    }

    @Override
    public Optional<List<ContextMenuItem>> buildMenuItem(AnnotationContextEvent event) {
        List<SeqSymmetry> selectedItems = event.getSelectedItems();
        if (!selectedItems.isEmpty() && !(selectedItems.get(0) instanceof GraphSym)) {
            List<ContextMenuItem> contextMenuItems = Lists.newArrayList();
            if (genomicSequenceViewer.getAction().isEnabled()) {
                ContextMenuItem genomicSequenceMenuItem = getGenomicSequenceMenuItem();
                contextMenuItems.add(genomicSequenceMenuItem);
            }
            if (readSequenceViewer.getAction().isEnabled()) {
                ContextMenuItem readSequenceMenuItem = getReadSequenceMenuItem();
                contextMenuItems.add(readSequenceMenuItem);
            }
            return Optional.of(contextMenuItems);
        }
        return Optional.empty();
    }

    private ContextMenuItem getReadSequenceMenuItem() {
        ContextMenuItem readSequenceMenuItem = new ContextMenuItem(readSequenceViewer.getText(), (Void t) -> {
            readSequenceViewer.getAction().actionPerformed(null);
            return t;
        });
        try (InputStream resourceAsStream = PopupListener.class.getClassLoader().getResourceAsStream(SEQUENCE_VIEWER_ICONPATH)) {
            readSequenceMenuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        readSequenceMenuItem.setMenuSection(MenuSection.SEQUENCE);
        return readSequenceMenuItem;
    }

    private ContextMenuItem getGenomicSequenceMenuItem() {
        ContextMenuItem genomicSequenceMenuItem = new ContextMenuItem(genomicSequenceViewer.getText(), (Void t) -> {
            genomicSequenceViewer.getAction().actionPerformed(null);
            return t;
        });
        try (InputStream resourceAsStream = PopupListener.class.getClassLoader().getResourceAsStream(SEQUENCE_VIEWER_ICONPATH)) {
            genomicSequenceMenuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        genomicSequenceMenuItem.setMenuSection(MenuSection.SEQUENCE);
        return genomicSequenceMenuItem;
    }

}
