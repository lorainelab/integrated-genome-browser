package com.lorainelab.igb.frame;

import aQute.bnd.annotation.component.Component;
import static com.affymetrix.common.CommonUtils.APP_NAME;
import static com.affymetrix.common.CommonUtils.APP_VERSION;
import com.lorainelab.igb.frame.api.FrameManagerService;
import javax.swing.JFrame;
import net.miginfocom.swing.MigLayout;

@Component(immediate = true)
public class FrameManager implements FrameManagerService {

    private final JFrame igbMainFrame;

    public FrameManager() {
        igbMainFrame = new JFrame(APP_NAME + " " + APP_VERSION);
        igbMainFrame.setLayout(new MigLayout("fill"));
    }

    @Override
    public JFrame getIgbMainFrame() {
        return igbMainFrame;
    }

}
