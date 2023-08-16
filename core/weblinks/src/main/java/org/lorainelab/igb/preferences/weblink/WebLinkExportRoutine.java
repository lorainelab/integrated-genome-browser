package org.lorainelab.igb.preferences.weblink;

import org.lorainelab.igb.services.window.WindowServiceLifecycleHook;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = WebLinkExportRoutine.COMPONENT_NAME, immediate = true, service = WindowServiceLifecycleHook.class)
public class WebLinkExportRoutine implements WindowServiceLifecycleHook {

    private static final Logger logger = LoggerFactory.getLogger(WebLinkExportRoutine.class);
    public static final String COMPONENT_NAME = "WebLinkExportRoutine";
    private WebLinkExporter exporter;

    public WebLinkExportRoutine() {
    }

    @Override
    public void stop() {
        if (exporter != null) {
            exporter.exportUserWebLinks();
        }
    }

    @Override
    public void start() {

    }

    @Reference
    public void setWebLinkUtils(WebLinkExporter exporter) {
        this.exporter = exporter;
    }

}
