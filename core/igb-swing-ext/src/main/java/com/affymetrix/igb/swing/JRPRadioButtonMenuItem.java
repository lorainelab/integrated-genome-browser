package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.script.ScriptManager;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JRadioButtonMenuItem;

public class JRPRadioButtonMenuItem extends JRadioButtonMenuItem implements WeightedJRPWidget {

	private static final long serialVersionUID = 1L;
	private final String id;
        private int weight;

	public JRPRadioButtonMenuItem(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPRadioButtonMenuItem(String id, Action a) {
		super(a);
		this.id = id;
		init();
	}

	public JRPRadioButtonMenuItem(String id, Icon icon) {
		super(icon);
		this.id = id;
		init();
	}

	public JRPRadioButtonMenuItem(String id, Icon icon, boolean selected) {
		super(icon, selected);
		this.id = id;
		init();
	}

        /**/
	public JRPRadioButtonMenuItem(String id, String text) {
            this(id, text, -1);
	}
        
        public JRPRadioButtonMenuItem(String id, String text, int weight) {
		super(text);
		this.id = id;
		init();
	}

	public JRPRadioButtonMenuItem(String id, String text, boolean selected) {
		super(text, selected);
		this.id = id;
		init();
	}

	public JRPRadioButtonMenuItem(String id, String text, Icon icon) {
		super(text, icon);
		this.id = id;
		init();
	}

	public JRPRadioButtonMenuItem(String id, String text, Icon icon, boolean selected) {
		super(text, icon, selected);
		this.id = id;
		init();
	}

	private void init() {
		ScriptManager.getInstance().addWidget(this);
		addActionListener(e -> ScriptManager.getInstance().recordOperation(new Operation(JRPRadioButtonMenuItem.this, "doClick()")));
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
