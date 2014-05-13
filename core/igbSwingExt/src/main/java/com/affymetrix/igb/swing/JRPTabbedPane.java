package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.util.Idable;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JRPTabbedPane extends JTabbedPane implements JRPHierarchicalWidget {

	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPTabbedPane(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPTabbedPane(String id, int tabPlacement) {
		super(tabPlacement);
		this.id = id;
		init();
	}

	public JRPTabbedPane(String id, int tabPlacement, int tabLayoutPolicy) {
		super(tabPlacement, tabLayoutPolicy);
		this.id = id;
		init();
	}

	private void init() {
		ScriptManager.getInstance().addWidget(this);
		addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (ScriptManager.getInstance().isMouseDown()) {
					ScriptManager.getInstance().recordOperation(new Operation(JRPTabbedPane.this, "setSelectedIndex(" + getSelectedIndex() + ")"));
				}
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

	public SubRegionFinder getSubRegionFinder(String subId) {
		int index = -1;
		for (int i = 0; i < getComponentCount() && index == -1; i++) {
			Component comp = getComponentAt(i);
			if (comp instanceof Idable && subId.equals(((Idable) comp).getId())) {
				index = i;
			}
		}
		final int tabIndex = index;
		return new SubRegionFinder() {
			@Override
			public Rectangle getRegion() {
				return getBoundsAt(tabIndex);
			}
		};
	}
}
