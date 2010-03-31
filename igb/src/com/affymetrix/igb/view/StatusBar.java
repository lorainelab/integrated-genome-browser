package com.affymetrix.igb.view;

import com.affymetrix.igb.Application;
import java.awt.BorderLayout;
import java.awt.Cursor;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import java.text.DecimalFormat;
import java.util.Set;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.EmptyBorder;

public final class StatusBar extends JPanel {

	private final JLabel status_ta;
	public final JProgressBar progressBar;
	private final JButton mainCancel;
	private final JPopupMenu runningTasks;
	private final ImageIcon closeIcon;
	private final JLabel memory_ta;
	private final JPopupMenu popup_menu = new JPopupMenu();
	private final DecimalFormat num_format;
	/** Delay in milliseconds between updates of the status (such as memory usage).  */
	private static final int timer_delay_ms = 5000;

	public StatusBar() {
		Application app = Application.getSingleton();

		String gc_name = app.getResourceString("perform_garbage_collection");
		if (gc_name != null && gc_name.length() > 0) {
			performGcAction.putValue(Action.NAME, gc_name);
		}

		String tt_hairline = app.getResourceString("status_bar_hairline_desc");
		String tt_status = app.getResourceString("status_bar_desc");
		String tt_status_memory = app.getResourceString("status_bar_memory_desc");
		if (tt_hairline == null || tt_hairline.length() == 0) {
			tt_hairline = "Hairline Position";
		}

		if (tt_status == null || tt_status.length() == 0) {
			tt_status = "Shows Selected Item, or other Message";
		}

		if (tt_status_memory == null || tt_status_memory.length() == 0) {
			tt_status_memory = "Memory Used / Available";
		}

		java.net.URL imgURL = com.affymetrix.igb.IGB.class.getResource("x_icon.gif");
		closeIcon = new ImageIcon(imgURL);
		
		status_ta = new JLabel("");
		progressBar = new JProgressBar();
		memory_ta = new JLabel("");
		runningTasks = new JPopupMenu();
		mainCancel = new JButton(closeIcon);
		mainCancel.setBorder(new EmptyBorder(mainCancel.getInsets()));
		status_ta.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		progressBar.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		memory_ta.setBorder(BorderFactory.createEmptyBorder(0,0,0,15));

		status_ta.setToolTipText(tt_status);
		progressBar.setMaximumSize(new Dimension(150, 5));
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		mainCancel.setVisible(false);
		memory_ta.setToolTipText(tt_status_memory);
		memory_ta.setHorizontalAlignment(SwingConstants.TRAILING);

		num_format = new DecimalFormat();
		num_format.setMaximumFractionDigits(1);
		num_format.setMinimumFractionDigits(1);

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);

		layout.setAutoCreateContainerGaps(false);
		layout.setAutoCreateGaps(true);
		layout.setHonorsVisibility(false);

		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addComponent(status_ta)
				.addGap(1, 1, Short.MAX_VALUE)
				.addComponent(progressBar)
				.addComponent(mainCancel)
				.addComponent(memory_ta, 1, 200, 200));

		layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(status_ta)
				.addGap(1, 1, Short.MAX_VALUE)
				.addComponent(progressBar)
				.addComponent(mainCancel)
				.addComponent(memory_ta));

		mainCancel.addActionListener(showTasks);
		progressBar.setCursor(new Cursor(Cursor.HAND_CURSOR));
		JMenuItem gc_MI = new JMenuItem(performGcAction);
		popup_menu.add(gc_MI);

		memory_ta.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				if (evt.isPopupTrigger()) {
					popup_menu.show(memory_ta, evt.getX(), evt.getY());
				}
			}
			@Override
			public void mouseReleased(MouseEvent evt) {
				if (evt.isPopupTrigger()) {
					popup_menu.show(memory_ta, evt.getX(), evt.getY());
				}
			}
		});

		progressBar.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent evt) {
				showRunningTasks();
			}
			
			@Override
			public void mouseReleased(MouseEvent evt) {
				showRunningTasks();
			}
		});

		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				updateMemory();
			}
		};
		Timer timer = new Timer(timer_delay_ms, al);
		timer.setInitialDelay(0);
		timer.start();
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
		updateMemory();
	}

	public String getStatus() {
		return status_ta.getText();
	}

	Action performGcAction = new AbstractAction("Release Unused Memory") {

		public void actionPerformed(ActionEvent ae) {
			System.gc();
		}
	};
	
	/**
	 *  Causes the memory indicator to update its value.  Normally you do not
	 *  need to call this method as the memory value will be updated from
	 *  time to time automatically.
	 */
	private void updateMemory() {
		Runtime rt = Runtime.getRuntime();
		long memory = rt.totalMemory() - rt.freeMemory();

		double mb = 1.0 * memory / (1024 * 1024);
		String text = num_format.format(mb) + " MB";

		long max_memory = rt.maxMemory();
		if (max_memory != Long.MAX_VALUE) {
			double max = 1.0 * rt.maxMemory() / (1024 * 1024);
			text += " / " + num_format.format(max) + " MB";
		}
		memory_ta.setText(text);
	}

	Action cancel = new AbstractAction() {

		public void actionPerformed(ActionEvent ae) {
			hideRunningTasks();
		}
	};

	Action showTasks = new AbstractAction() {

		public void actionPerformed(ActionEvent ae) {
			showRunningTasks();
		}
	};

	/**
	 * Populate popup menu with running process. Updated every time string is added or removed from process String in application.
	 * @param processString		Set of currently running process.
	 */
	public final void setCancelPopup(Set<String> processString) {
		runningTasks.removeAll();

		boolean isShowing = runningTasks.isShowing();

		if (isShowing) {
			hideRunningTasks();
		}

		if (processString.isEmpty()) {
			mainCancel.setVisible(false);
			return;
		}


		for (String s : processString) {
			JPanel comp = new JPanel(new BorderLayout());
			JLabel taskName = new JLabel(s.substring(0, Math.min(25, s.length())));
			JButton cancelTask = new JButton(closeIcon);
			cancelTask.setBorder(new EmptyBorder(cancelTask.getInsets()));
			cancelTask.addActionListener(cancel);
			comp.add(taskName, BorderLayout.WEST);
			comp.add(cancelTask, BorderLayout.EAST);
			runningTasks.add(comp);
		}
		
		
		if (isShowing) {
			showRunningTasks();
		}

		mainCancel.setVisible(true);
	}

	/**
	 * Shows Running Threads in popup menu.
	 */
	private final void showRunningTasks() {
		int x = (int) mainCancel.getAlignmentX() - mainCancel.getWidth() - progressBar.getWidth();
		int y = (int) mainCancel.getAlignmentY() + mainCancel.getHeight();
		//int y = (int) getAlignmentY() - runningTasks.getHeight();
		runningTasks.show(mainCancel, x, y);
	}

	/**
	 * Hide running tasks.
	 */
	private final void hideRunningTasks() {
		runningTasks.setVisible(false);
	}
}
