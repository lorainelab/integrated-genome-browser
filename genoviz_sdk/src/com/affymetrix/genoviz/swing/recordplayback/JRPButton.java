package com.affymetrix.genoviz.swing.recordplayback;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

public class JRPButton extends JButton implements JRPWidget {
	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPButton(String id) {
		super();
		this.id = id;
		init();
	}

	public JRPButton(String id, Action a) {
		super(a);
		this.id = id;
		init();
	}
	public JRPButton(String id, Icon icon) {
		super(icon);
		this.id = id;
		init();
    }
	public JRPButton(String id, String text) {
		super(text);
		this.id = id;
		init();
    }
	public JRPButton(String id, String text, Icon icon) {
		super(text, icon);
		this.id = id;
		init();
    }
    private void init() {
		RecordPlaybackHolder.getInstance().addWidget(this);
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				RecordPlaybackHolder.getInstance().recordOperation(new Operation(JRPButton.this));
			}
		});
    }
    @Override
	public String getID() {
		return id;
	}

	@Override
	public void execute(String... params) {
		doClick();
	}

	@Override
	public String[] getParms() {
		return new String[]{id};
	}
}
