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
import com.affymetrix.igb.swing.JRPMenuItem;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import org.lorainelab.igb.services.window.menus.IgbToolBarParentMenu;
import org.lorainelab.igb.image.exporter.service.ImageExportService;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;

@Component(name = ViewReadSequenceInSeqViewerAction.COMPONENT_NAME, immediate = true, provide = {GenericAction.class, IgbMenuItemProvider.class})
public class ViewReadSequenceInSeqViewerAction extends GenericAction implements SymSelectionListener, IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "ViewReadSequenceInSeqViewerAction";
    private static final long serialVersionUID = 1L;
    private IgbService igbService;
    private ImageExportService imageExportService;

    public ViewReadSequenceInSeqViewerAction() {
        super(AbstractSequenceViewer.BUNDLE.getString("ViewReadSequenceInSeqViewer"), null, "16x16/actions/Genome_Viewer_reads.png", "22x22/actions/Genome_Viewer_reads.png", KeyEvent.VK_UNDEFINED, null, false);
        this.setEnabled(false);
        GenometryModel.getInstance().addSymSelectionListener(this);
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
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
        if (!evt.getSelectedGraphSyms().isEmpty() && !(evt.getSelectedGraphSyms().get(0) instanceof GraphSym)) {
            if (evt.getSelectedGraphSyms().get(0) instanceof SymWithResidues) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        } else {
            setEnabled(false);
        }
    }

    @Override
    public IgbToolBarParentMenu getParentMenu() {
        return IgbToolBarParentMenu.VIEW;
    }

    @Override
    public JRPMenuItem getMenuItem() {
        JRPMenuItem readSequenceMenuItem = new JRPMenuItem("SequenceViewer_viewAlignmentSequenceInSeqViewer", this);
        return readSequenceMenuItem;
    }

    @Override
    public int getMenuItemWeight() {
        return 6;
    }

    @Reference
    public void setImageExportService(ImageExportService imageExportService) {
        this.imageExportService = imageExportService;
    }
}
