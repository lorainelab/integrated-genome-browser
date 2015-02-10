package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.script.ScriptManager;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;

public class JRPCheckBoxMenuItem extends JCheckBoxMenuItem implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPCheckBoxMenuItem(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPCheckBoxMenuItem(String id, Action a) {
		super(a);
		this.id = id;
		init();
	}

	public JRPCheckBoxMenuItem(String id, Icon icon) {
		super(icon);
		this.id = id;
		init();
	}

	public JRPCheckBoxMenuItem(String id, String text) {
		super(text);
		this.id = id;
		init();
	}

	public JRPCheckBoxMenuItem(String id, String text, boolean b) {
		super(text, b);
		this.id = id;
		init();
	}

	public JRPCheckBoxMenuItem(String id, String text, Icon icon) {
		super(text, icon);
		this.id = id;
		init();
	}

	public JRPCheckBoxMenuItem(String id, String text, Icon icon, boolean b) {
		super(text, icon, b);
		this.id = id;
		init();
	}

	private void init() {
		ScriptManager.getInstance().addWidget(this);
		addActionListener(e -> ScriptManager.getInstance().recordOperation(new Operation(JRPCheckBoxMenuItem.this, "doClick()")));
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
