package com.lorainelab.image.exporter.service;

import java.awt.Component;
import java.io.File;

/**
 *
 * @author dcnorris
 */
public interface ImageExportService {

    void exportComponent(Component component);

    void headlessComponentExport(Component component, File f, String ext, boolean isScript);

}
