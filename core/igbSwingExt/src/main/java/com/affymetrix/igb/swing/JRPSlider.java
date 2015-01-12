package com.affymetrix.igb.swing;

import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JRPSlider extends JSlider implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPSlider(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPSlider(String id, BoundedRangeModel brm) {
		super(brm);
		this.id = id;
		init();
	}

	public JRPSlider(String id, int orientation) {
		super(orientation);
		this.id = id;
		init();
	}

	public JRPSlider(String id, int min, int max) {
		super(min, max);
		this.id = id;
		init();
	}

	public JRPSlider(String id, int min, int max, int value) {
		super(min, max, value);
		this.id = id;
		init();
	}

	public JRPSlider(String id, int orientation, int min, int max, int value) {
		super(orientation, min, max, value);
		this.id = id;
		init();
	}

	private void init() {
		ScriptManager.getInstance().addWidget(this);
		addChangeListener(e -> ScriptManager.getInstance().recordOperation(new Operation(JRPSlider.this, "setValue(" + getValue() + ")")));
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
