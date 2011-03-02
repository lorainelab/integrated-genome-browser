package com.affymetrix.igb.view;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

import com.affymetrix.igb.view.load.GeneralLoadView;
import com.jidesoft.status.MemoryStatusBarItem;

public final class StatusBar extends JPanel {
	private static final long serialVersionUID = 1l;

	private final JLabel status_ta;
	public final JProgressBar progressBar;
	MemoryStatusBarItem memory_item;
	private JButton stopBtn;

	public StatusBar() {
		String tt_status = "Shows Selected Item, or other Message";
	
		status_ta = new JLabel("");
		progressBar = new JProgressBar();
		memory_item = new MemoryStatusBarItem();
		memory_item.setShowMaxMemory(true);
		stopBtn = new JButton("x");
		stopBtn.addActionListener(new ButtonListener());
		
		status_ta.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		progressBar.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
	
		status_ta.setToolTipText(tt_status);
		progressBar.setMaximumSize(new Dimension(150, 5));
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);

		layout.setAutoCreateContainerGaps(false);
		layout.setAutoCreateGaps(true);
		layout.setHonorsVisibility(false);

		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addComponent(status_ta)
				.addGap(1, 1, Short.MAX_VALUE)
				.addComponent(progressBar)
				.addComponent(stopBtn)
				.addComponent(memory_item, 1, 200, 200));

		layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(status_ta)
				.addGap(1, 1, Short.MAX_VALUE)
				.addComponent(progressBar)
				.addComponent(stopBtn)
				.addComponent(memory_item));

	}

	/** Sets the String in the status bar.
	 *  HTML can be used if prefixed with "<html>".
	 *  Can be safely called from any thread.
	 *  @param s  a String, null is ok; null will erase the status String.
	 */
	public final void setStatus(String s) {
		if (s == null) {
			s = "";
		}

		status_ta.setText(s);
	}

	public String getStatus() {
		return status_ta.getText();
	}

	class ButtonListener implements ActionListener {
		ButtonListener() {}

		public void actionPerformed(ActionEvent e) {
			GeneralLoadView.getLoadView().stoploading = true;
			if (e.getActionCommand().equals("x")) {
				GeneralLoadView.getLoadView().stoploading = true;
				System.out.println("stop button clicked");
				progressBar.setValue(progressBar.getMinimum());
				//progressBar.setVisible(false);
				status_ta.setText("");
				memory_item.stop();
		    	
				System.gc();
			}
		}
	}
}
