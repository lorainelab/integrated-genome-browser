package com.affymetrix.igb.update;

import com.affymetrix.genometry.util.StatusAlert;
import java.text.MessageFormat;
import javax.swing.Icon;
import static com.affymetrix.igb.update.ShowUpdate.*;

/**
 *
 * @author hiralv
 */
public class UpdateStatusAlert implements StatusAlert {

    public static final String UPDATE_AVAILABLE = "Update Available";
    private static final String ICON_PATH = "16x16/actions/warning.png";

    private final Update update;

    public UpdateStatusAlert(Update update) {
        this.update = update;
    }

    public Icon getIcon() {
        return null;
    }

    public String getDisplayMessage() {
        return UPDATE_AVAILABLE;
    }

    public String getToolTip() {
        return MessageFormat.format("A new version {0} of IGB is available", update.getVersion().toString());
    }

    public int actionPerformed() {
        return showUpdate(update) ? StatusAlert.HIDE_ALERT : StatusAlert.KEEP_ALERT;
    }
}
