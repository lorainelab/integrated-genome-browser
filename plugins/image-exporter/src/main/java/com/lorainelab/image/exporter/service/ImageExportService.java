package com.lorainelab.image.exporter.service;

import java.awt.Component;
import java.io.File;
import java.util.Map;

/**
 *
 * @author dcnorris
 */
public interface ImageExportService {

    void exportComponents(Map<String, Component> components);

    void headlessComponentExport(Component component, File f, String ext, boolean isScript);

}
