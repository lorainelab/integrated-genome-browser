package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.script.ScriptManager;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;

public class JRPMenuItem extends JMenuItem implements WeightedJRPWidget {

    private static final long serialVersionUID = 1L;
    private final String id;
    private int weight = -1;

    public JRPMenuItem(String id) {
        super();
        this.id = id;
        init();
    }

    public JRPMenuItem(String id, Action a, int weight) {
        super(a);
        this.id = id;
        this.weight = weight;
        init();
    }
    
    public JRPMenuItem(String id, Action a) {
        this(id, a, -1);
    }

    public JRPMenuItem(String id, Icon icon) {
        super(icon);
        this.id = id;
        init();
    }

    public JRPMenuItem(String id, String text) {
        super(text);
        this.id = id;
        init();
    }

    public JRPMenuItem(String id, String text, Icon icon) {
        super(text, icon);
        this.id = id;
        init();
    }

    public JRPMenuItem(String id, String text, int mnemonic) {
        super(text, mnemonic);
        this.id = id;
        init();
    }

    private void init() {
        ScriptManager.getInstance().addWidget(this);
        addActionListener(e -> ScriptManager.getInstance().recordOperation(new Operation(JRPMenuItem.this, "doClick()")));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean consecutiveOK() {
        return true;
    }

    @Override
    public int getWeight() {
        return weight;
    }
}
