package com.lorainelab.igb.frame;

import com.lorainelab.igb.frame.api.FrameManagerService;
import aQute.bnd.annotation.component.Component;
import static com.affymetrix.common.CommonUtils.APP_NAME;
import static com.affymetrix.common.CommonUtils.APP_VERSION;
import javax.swing.JFrame;

@Component(immediate = true)
public class FrameManager implements FrameManagerService {

    private final JFrame igbMainFrame;

    public FrameManager() {
        igbMainFrame = new JFrame(APP_NAME + " " + APP_VERSION);
    }

    @Override
    public JFrame getIgbMainFrame() {
        return igbMainFrame;
    }

}
