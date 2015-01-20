package com.affymetrix.igb.swing;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

public class JRPButton extends JButton implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPButton(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPButton(String id, Action a) {
		super(a);
		this.id = id;
		init();
	}

	public JRPButton(String id, Icon icon) {
		super(icon);
		this.id = id;
		init();
	}

	public JRPButton(String id, String text) {
		super(text);
		this.id = id;
		init();
	}

	public JRPButton(String id, String text, Icon icon) {
		super(text, icon);
		this.id = id;
		init();
	}

	private void init() {
		ScriptManager.getInstance().addWidget(this);
		addActionListener(e -> ScriptManager.getInstance().recordOperation(new Operation(JRPButton.this, "doClick()")));
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
