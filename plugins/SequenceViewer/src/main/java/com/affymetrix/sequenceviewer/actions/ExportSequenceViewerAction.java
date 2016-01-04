package com.affymetrix.sequenceviewer.actions;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.sequenceviewer.AbstractSequenceViewer;
import org.lorainelab.igb.image.exporter.service.ImageExportService;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ExportSequenceViewerAction extends GenericAction {

    public static final String COMPONENT_NAME = "ExportSequenceViewerAction";
    private static final long serialVersionUID = 1L;
    private final ImageExportService imageExportService;
    public final java.awt.Component comp;

    public ExportSequenceViewerAction(java.awt.Component comp, ImageExportService imageExportService) {
        super(AbstractSequenceViewer.BUNDLE.getString("saveImage"), null, "22x22/actions/Sequence_Viewer_export.png");
        this.comp = comp;
        this.imageExportService = imageExportService;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Map<String, Optional<Component>> components = new HashMap<>();
        components.put("Whole View", Optional.of(comp));
        imageExportService.exportComponents(components);
    }

}
