package com.affymetrix.genoviz.swing.recordplayback;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JRPTabbedPane extends JTabbedPane implements JRPWidget {
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
		RecordPlaybackHolder.getInstance().addWidget(this);
		addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				RecordPlaybackHolder.getInstance().recordOperation(new Operation(id, "setSelectedIndex(" + getSelectedIndex() + ")"));
			}
		});
    }
	@Override
	public String getID() {
		return id;
	}
}
