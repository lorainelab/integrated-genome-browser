package com.lorainelab.igb.preferences.weblink;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.lorainelab.igb.service.api.IWindowRoutine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = WebLinkExportRoutine.COMPONENT_NAME, immediate = true, provide = IWindowRoutine.class)
public class WebLinkExportRoutine implements IWindowRoutine {

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

    @Reference(optional = false)
    public void setWebLinkUtils(WebLinkExporter exporter) {
        this.exporter = exporter;
    }

}
