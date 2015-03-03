package com.affymetrix.igb.swing;

import com.affymetrix.genoviz.swing.StyledJComboBox;
import com.affymetrix.igb.swing.script.ScriptManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 *
 * @author nick
 */
public class JRPStyledJComboBox extends StyledJComboBox implements JRPWidget {

    private static final long serialVersionUID = 1L;
    private final String id;

    public JRPStyledJComboBox(String id) {
        super();
        this.id = id;
        init();
    }

    private void init() {
        ScriptManager.getInstance().addWidget(this);
        // use PopupMenuListener to only get user initiated changes
        addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//				RecordPlaybackHolder.getInstance().recordOperation(getOperation((String)getSelectedItem()));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                ScriptManager.getInstance().recordOperation(new Operation(JRPStyledJComboBox.this, "setSelectedItem(\"" + getSelectedItem() + "\")"));
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean consecutiveOK() {
        return false;
    }
}
