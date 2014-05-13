package com.affymetrix.igb.swing;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import com.affymetrix.genoviz.awt.AdjustableJSlider;

public class RPAdjustableJSlider extends AdjustableJSlider implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private String id;

	public RPAdjustableJSlider(String id) {
		super();
		this.id = id;
		init();
	}

	public RPAdjustableJSlider(String id, int orientation) {
		super(orientation);
		this.id = id;
		init();
	}

	private void init() {
		if (id != null) {
			ScriptManager.getInstance().addWidget(this);
		}
		addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				ScriptManager.getInstance().recordOperation(new Operation(RPAdjustableJSlider.this, "setValue(" + getValue() + ")"));
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
