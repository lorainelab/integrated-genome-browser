package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.script.ScriptManager;
import javax.swing.JScrollBar;

public class JRPScrollBar extends JScrollBar implements JRPWidget {

    private static final long serialVersionUID = 1L;
    private final String id;

    public JRPScrollBar(String id) {
        super();
        this.id = id;
        init();
    }

    public JRPScrollBar(String id, int orientation) {
        super(orientation);
        this.id = id;
        init();
    }

    public JRPScrollBar(String id, int orientation, int value, int extent, int min, int max) {
        super(orientation, value, extent, min, max);
        this.id = id;
        init();
    }

    private void init() {
        ScriptManager.getInstance().addWidget(this);
        addAdjustmentListener(e -> ScriptManager.getInstance().recordOperation(new Operation(JRPScrollBar.this, "setValue(" + getValue() + ")")));
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
