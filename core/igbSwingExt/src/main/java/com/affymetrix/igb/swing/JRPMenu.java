package com.affymetrix.igb.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JMenu;

public class JRPMenu extends JMenu implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPMenu(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPMenu(String id, Action a) {
		super(a);
		this.id = id;
		init();
	}

	public JRPMenu(String id, String s) {
		super(s);
		this.id = id;
		init();
	}

	public JRPMenu(String id, String s, boolean b) {
		super(s, b);
		this.id = id;
		init();
	}

	private void init() {
		ScriptManager.getInstance().addWidget(this);
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ScriptManager.getInstance().recordOperation(new Operation(JRPMenu.this, "doClick()"));
			}
		});
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
