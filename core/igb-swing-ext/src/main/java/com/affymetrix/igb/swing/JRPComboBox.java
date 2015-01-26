package com.affymetrix.igb.swing;

import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class JRPComboBox extends JComboBox implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPComboBox(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPComboBox(String id, ComboBoxModel aModel) {
		super(aModel);
		this.id = id;
		init();
	}

	public JRPComboBox(String id, Object[] items) {
		super(items);
		this.id = id;
		init();
	}

	public JRPComboBox(String id, Vector<?> items) {
		super(items);
		this.id = id;
		init();
	}

	private void init() {
		ScriptManager.getInstance().addWidget(this);
		// use PopupMenuListener to only get user initiated changes
		addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//				RecordPlaybackHolder.getInstance().recordOperation(getOperation((String)getSelectedItem()));
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				ScriptManager.getInstance().recordOperation(new Operation(JRPComboBox.this, "setSelectedItem(\"" + getSelectedItem() + "\")"));
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});
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
