package com.affymetrix.igb.swing;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.border.Border;

public class JRPWrapper extends JComponent implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private final String id;
	private final JComponent baseComponent;

	public JRPWrapper(String id, JComponent baseComponent) {
		super();
		this.id = id;
		this.baseComponent = baseComponent;
	}

	public JComponent getBaseComponent() {
		return baseComponent;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean consecutiveOK() {
		return false;
	}

///////////////////////////////////////////////////////
//    pass all JComponent methods to baseComponent   //
///////////////////////////////////////////////////////
	public void setBackground(Color bg) {
		baseComponent.setBackground(bg);
	}

	public void setBorder(Border border) {
		baseComponent.setBorder(border);
	}

	public boolean requestFocusInWindow() {
		return baseComponent.requestFocusInWindow();
	}

	public void setVisible(boolean aFlag) {
		baseComponent.setVisible(aFlag);
	}
    // ...
	// add as needed
}
