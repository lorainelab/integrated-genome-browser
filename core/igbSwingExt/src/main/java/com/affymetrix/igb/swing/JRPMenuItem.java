package com.affymetrix.igb.swing;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;

public class JRPMenuItem extends JMenuItem implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPMenuItem(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPMenuItem(String id, Action a) {
		super(a);
		this.id = id;
		init();
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
}
