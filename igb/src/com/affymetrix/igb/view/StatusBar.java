package com.affymetrix.igb.view;

import com.affymetrix.igb.Application;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.text.DecimalFormat;

public final class StatusBar extends JPanel {

	private final JLabel status_ta;
	public final JProgressBar progressBar;
	private final JLabel memory_ta;
	private final JPopupMenu popup_menu = new JPopupMenu();
	private final DecimalFormat num_format;
	/** Delay in milliseconds between updates of the status (such as memory usage).  */
	static int timer_delay_ms = 5000;

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

		status_ta = new JLabel("");
		progressBar = new JProgressBar();
		memory_ta = new JLabel("");
		status_ta.setBorder(new BevelBorder(BevelBorder.LOWERED));
		progressBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
		// this border leaves some extra space, especially on the right side,
		// so the Mac OS can put the "resize window" gui there
		memory_ta.setBorder(
				BorderFactory.createCompoundBorder(
				BorderFactory.createBevelBorder(BevelBorder.LOWERED),
				BorderFactory.createEmptyBorder(0, 12, 0, 15)));

		status_ta.setToolTipText(tt_status);
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		memory_ta.setToolTipText(tt_status_memory);

		num_format = new DecimalFormat();
		num_format.setMaximumFractionDigits(1);
		num_format.setMinimumFractionDigits(1);

		BorderLayout bl = new BorderLayout();
		setLayout(bl);

		this.add(status_ta, BorderLayout.WEST);
		this.add(progressBar, BorderLayout.CENTER);
		this.add(memory_ta, BorderLayout.EAST);

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

		updateSafely(status_ta, s);
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
	public void updateMemory() {
		Runtime rt = Runtime.getRuntime();
		long memory = rt.totalMemory() - rt.freeMemory();

		double mb = 1.0 * memory / (1024 * 1024);
		String text = num_format.format(mb) + " MB";

		long max_memory = rt.maxMemory();
		if (max_memory != Long.MAX_VALUE) {
			double max = 1.0 * rt.maxMemory() / (1024 * 1024);
			text += " / " + num_format.format(max) + " MB";
		}
		updateSafely(memory_ta, text);
	}

	/**
	 *  Update a JLabel in a way that is safe from either the GUI thread or
	 *  any other thread.
	 */
	void updateSafely(final JLabel label, final String text) {
		if (SwingUtilities.isEventDispatchThread()) {
			label.setText(text);
		} else {
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					label.setText(text);
				}
			});
		}
	}
}
