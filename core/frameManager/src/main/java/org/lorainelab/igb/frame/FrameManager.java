package org.lorainelab.igb.frame;

import static com.affymetrix.common.CommonUtils.IGB_NAME;
import static com.affymetrix.common.CommonUtils.IGB_VERSION;
import com.affymetrix.common.PreferenceUtils;
import org.lorainelab.igb.frame.api.FrameManagerService;
import java.awt.Rectangle;
import javax.swing.JFrame;
import net.miginfocom.swing.MigLayout;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class FrameManager implements FrameManagerService {

    public static final String MAIN_WINDOW_PREF_KEY = "main window";
    private final JFrame igbMainFrame;
    private static final int DEFAULT_FRAME_HEIGHT = 720;
    private static final int DEFAULT_FRAME_WIDTH = 1165;

    public FrameManager() {
        igbMainFrame = new JFrame(IGB_NAME + " " + IGB_VERSION);
        initializeWindowSize();
        igbMainFrame.setLayout(new MigLayout("fill"));
    }

    private void initializeWindowSize() {
        Rectangle frameBounds = getMainFrameBounds();
        PreferenceUtils.setWindowSize(igbMainFrame, frameBounds);
    }

    private Rectangle getMainFrameBounds() {
        // 1.61 ratio -- near golden ratio
        Rectangle frameBounds = PreferenceUtils.retrieveWindowLocation(MAIN_WINDOW_PREF_KEY, new Rectangle(0, 0, DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT));
        return frameBounds;
    }

    @Override
    public JFrame getIgbMainFrame() {
        return igbMainFrame;
    }

}
