package com.affymetrix.igb.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JRadioButton;

public class JRPRadioButton extends JRadioButton implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPRadioButton(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPRadioButton(String id, Action a) {
		super(a);
		this.id = id;
		init();
	}

	public JRPRadioButton(String id, Icon icon) {
		super(icon);
		this.id = id;
		init();
	}

	public JRPRadioButton(String id, Icon icon, boolean selected) {
		super(icon, selected);
		this.id = id;
		init();
	}

	public JRPRadioButton(String id, String text) {
		super(text);
		this.id = id;
		init();
	}

	public JRPRadioButton(String id, String text, boolean selected) {
		super(text, selected);
		this.id = id;
		init();
	}

	public JRPRadioButton(String id, String text, Icon icon) {
		super(text, icon);
		this.id = id;
		init();
	}

	public JRPRadioButton(String id, String text, Icon icon, boolean selected) {
		super(text, icon, selected);
		this.id = id;
		init();
	}

	private void init() {
		if (id != null) {
			ScriptManager.getInstance().addWidget(this);
		}
		addActionListener(e -> ScriptManager.getInstance().recordOperation(new Operation(JRPRadioButton.this, "doClick()")));
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
