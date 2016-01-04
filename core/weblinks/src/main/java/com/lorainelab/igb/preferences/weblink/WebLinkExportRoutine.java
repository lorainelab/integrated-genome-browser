package org.lorainelab.igb.igb.preferences.weblink;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import org.lorainelab.igb.igb.services.window.WindowServiceLifecycleHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = WebLinkExportRoutine.COMPONENT_NAME, immediate = true, provide = WindowServiceLifecycleHook.class)
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
