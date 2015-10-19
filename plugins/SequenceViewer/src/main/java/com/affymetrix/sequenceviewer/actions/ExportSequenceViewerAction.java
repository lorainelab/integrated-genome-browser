package com.affymetrix.sequenceviewer.actions;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.sequenceviewer.AbstractSequenceViewer;
import com.lorainelab.image.exporter.service.ImageExportService;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, Component> components = new HashMap<>();
        components.put("Whole View", comp);
        imageExportService.exportComponents(components);
    }

}
