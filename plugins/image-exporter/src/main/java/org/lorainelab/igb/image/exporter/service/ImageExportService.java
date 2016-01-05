package org.lorainelab.igb.image.exporter.service;

import java.awt.Component;
import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author dcnorris
 */
public interface ImageExportService {

    void exportComponents(Map<String, Optional<Component>> components);

    void headlessComponentExport(Component component, File f, String ext, boolean isScript);

}
