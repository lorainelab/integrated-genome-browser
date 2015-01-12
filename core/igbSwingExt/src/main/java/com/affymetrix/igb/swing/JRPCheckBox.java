package com.affymetrix.igb.swing;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;

public class JRPCheckBox extends JCheckBox implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private String id;

	public JRPCheckBox(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPCheckBox(String id, Action a) {
		super(a);
		this.id = id;
		init();
	}

	public JRPCheckBox(String id, Icon icon) {
		super(icon);
		this.id = id;
		init();
	}

	public JRPCheckBox(String id, Icon icon, boolean selected) {
		super(icon, selected);
		this.id = id;
		init();
	}

	public JRPCheckBox(String id, String text) {
		super(text);
		this.id = id;
		init();
	}

	public JRPCheckBox(String id, String text, boolean selected) {
		super(text, selected);
		this.id = id;
		init();
	}

	public JRPCheckBox(String id, String text, Icon icon) {
		super(text, icon);
		this.id = id;
		init();
	}

	public JRPCheckBox(String id, String text, Icon icon, boolean selected) {
		super(text, icon, selected);
		this.id = id;
		init();
	}

	private void init() {
		if (id != null) {
			ScriptManager.getInstance().addWidget(this);
		}
		addActionListener(e -> ScriptManager.getInstance().recordOperation(new Operation(JRPCheckBox.this, "doClick()")));
	}

	public void setId(String id) {
		if (this.id == null) {
			this.id = id;
			ScriptManager.getInstance().addWidget(this);
		}
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean consecutiveOK() {
		return true;
	}
}
