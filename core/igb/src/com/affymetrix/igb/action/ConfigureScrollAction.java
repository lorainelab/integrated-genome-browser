package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;
import com.affymetrix.genoviz.widget.AutoScroll;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author sgblanch
 * @version $Id: AutoScrollAction.java 11333 2012-05-01 17:54:56Z anuj4159 $
 */
public class ConfigureScrollAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1l;
	/*
	 * Units to scroll are either in pixels or bases
	 */
	private int as_pix_to_scroll = 4;
	private int as_time_interval = 20;
	private AutoScroll autoScroll;
	
	private static final ConfigureScrollAction ACTION = new ConfigureScrollAction();

	private ConfigureScrollAction() {
		super(BUNDLE.getString("configureAutoScroll"), null,
				"16x16/actions/configure_autoscroll.png",
				"22x22/actions/configure_autoscroll.png", // tool bar eligible
				KeyEvent.VK_A, null, true);
		this.ordinal = -4009000;
	}

	protected ConfigureScrollAction(String text, String small_icon, String large_icon){
		super(text, small_icon, large_icon);
	}

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}

	public static ConfigureScrollAction getAction() { return ACTION; }

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		this.configure(getSeqMapView());
	}

	private void configure(final SeqMapView seqMapView) {
		if (seqMapView.getViewSeq() == null) {
			return;
		}

		autoScroll = seqMapView.getAutoScroll();

		// turn OFF autoscroll while configuring
		StopAutoScrollAction.getAction().actionPerformed(null);
		
		// Show the config dialog
		AutoScrollConfigDialog autoScrollConfigDialog = new AutoScrollConfigDialog(null, seqMapView);
		autoScrollConfigDialog.showDialog();
	}

	// Normalize a text field so that it holds an integer, with a fallback value
	// if there is a problem, and a minimum and maximum
	private static int normalizeTF(JRPTextField tf, int fallback, int min, int max) {
		assert min <= fallback && fallback <= max;
		int result;
		try {
			result = Integer.parseInt(tf.getText());
		} catch (NumberFormatException nfe) {
			Toolkit.getDefaultToolkit().beep();
			result = fallback;
		}
		if (result < min) {
			result = min;
		} else if (result > max) {
			result = max;
		}
		tf.setText(Integer.toString(result));
		return result;
	}

	class AutoScrollConfigDialog extends JDialog {

		private JOptionPane optionPane;
		private JPanel pan;
		private final JRPTextField pix_to_scrollTF;
		private final JRPTextField time_intervalTF;
		private final Object[] startOptions = new Object[]{"Start", "Close"};
		private final Object[] stopOptions = new Object[]{"Stop", "Close"};

		/**
		 * Creates the reusable dialog.
		 */
		public AutoScrollConfigDialog (Frame aFrame, SeqMapView seqMapView) {
			super(aFrame, true);
			
			setLocationRelativeTo(seqMapView);
			
			pan = new JPanel();
			setLocationRelativeTo(seqMapView);
			pix_to_scrollTF = new JRPTextField("AutoScrollAction_pix_to_scroll", "" + as_pix_to_scroll);
			time_intervalTF = new JRPTextField("AutoScrollAction_time_interval", "" + as_time_interval);

			float bases_per_minute = (float) // 1000 ==> ms/s , 60 ==> s/minute, as_time_interval ==> ms/scroll
					(1.0 * autoScroll.get_bases_per_pix() * as_pix_to_scroll * 1000 * 60 / as_time_interval);
			bases_per_minute = Math.abs(bases_per_minute);
			float minutes_per_seq = seqMapView.getViewSeq().getLength() / bases_per_minute;
			final JLabel bases_per_minuteL = new JLabel("" + (bases_per_minute / 1000000));
			final JLabel minutes_per_seqL = new JLabel("" + (minutes_per_seq));

			pan.setLayout(new GridLayout(4, 2));
			pan.add(new JLabel("Scroll increment (pixels)"));
			pan.add(pix_to_scrollTF);
			pan.add(new JLabel("Time interval (milliseconds)"));
			pan.add(time_intervalTF);
			pan.add(new JLabel("Megabases per minute:  "));
			pan.add(bases_per_minuteL);
			pan.add(new JLabel("Total minutes for seq:  "));
			pan.add(minutes_per_seqL);

			KeyAdapter kl = new KeyAdapter() {

				@Override
				public void keyReleased(KeyEvent e) {
					as_pix_to_scroll = normalizeTF(pix_to_scrollTF, as_pix_to_scroll, -1000, 1000);
					as_time_interval = normalizeTF(time_intervalTF, as_time_interval, 1, 1000);

					float bases_per_minute = (float) // 1000 ==> ms/s , 60 ==> s/minute, as_time_interval ==> ms/scroll
							(1.0 * autoScroll.get_bases_per_pix() * as_pix_to_scroll * 1000 * 60 / as_time_interval);
					bases_per_minute = Math.abs(bases_per_minute);
					float minutes_per_seq = (autoScroll.get_end_pos() - autoScroll.get_start_pos()) / bases_per_minute;
					bases_per_minuteL.setText("" + (bases_per_minute / 1000000));
					minutes_per_seqL.setText("" + (minutes_per_seq));
				}
			};

			pix_to_scrollTF.addKeyListener(kl);
			time_intervalTF.addKeyListener(kl);
			
			// Click on pix to scroll and time interval text boxs will stop the auto scroll
			MouseListener ml = new MouseListener(){

				public void mouseClicked(MouseEvent me) {
				}

				public void mousePressed(MouseEvent me) {
				}

				public void mouseReleased(MouseEvent me) {
					if(optionPane.getOptions()[0].equals("Stop")) {
						StopAutoScrollAction.getAction().actionPerformed(null);
						optionPane.setOptions(startOptions);
						optionPane.setInitialValue(startOptions[0]);
					}
					((JRPTextField)me.getSource()).selectAll();
					((JRPTextField)me.getSource()).requestFocusInWindow();
				}

				public void mouseEntered(MouseEvent me) {
				}

				public void mouseExited(MouseEvent me) {
				}
			};
			
			pix_to_scrollTF.addMouseListener(ml);
			time_intervalTF.addMouseListener(ml);
		}
		
		private void showDialog() {

			optionPane = new JOptionPane(pan,
					JOptionPane.QUESTION_MESSAGE,
					JOptionPane.YES_NO_OPTION,
					null,
					startOptions,
					startOptions[0]);

			setTitle("AutoScroll Parameters");
			setResizable(false);
			setContentPane(optionPane);
			//setModal(false);
			setAlwaysOnTop(false);
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			
			//Register an event handler that reacts to option pane state changes.
			optionPane.addPropertyChangeListener(
					new PropertyChangeListener() {

						@Override
						public void propertyChange(PropertyChangeEvent e) {

							String prop = e.getPropertyName();

							if (isVisible()
									&& (e.getSource() == optionPane)
									&& (JOptionPane.VALUE_PROPERTY.equals(prop)
									|| JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {

								Object value = optionPane.getValue();

								if (value == JOptionPane.UNINITIALIZED_VALUE) {
									return;
								}
								
								//Reset the JOptionPane's value.
								//If you don't do this, then if the user presses the same button next time, no property change event will be fired.
								optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
								
								if (value.equals(startOptions[0]) || value.equals(stopOptions[0])) {
									as_pix_to_scroll = normalizeTF(pix_to_scrollTF, as_pix_to_scroll, -1000, 1000);
									as_time_interval = normalizeTF(time_intervalTF, as_time_interval, 1, 1000);
									autoScroll.configure(as_pix_to_scroll, as_time_interval);

									if (value.equals(startOptions[0])) {
										StartAutoScrollAction.getAction().start();
										optionPane.setOptions(stopOptions); // Change Start button to Stop
										optionPane.setInitialValue(stopOptions[0]); // Press Enter will trigger the new Stop button
									} else if (value.equals(stopOptions[0])) {
										StopAutoScrollAction.getAction().actionPerformed(null);
										optionPane.setOptions(startOptions);
										optionPane.setInitialValue(startOptions[0]);
									} 
								} else if (value.equals("Close")) {
										StopAutoScrollAction.getAction().actionPerformed(null); // Stop auto scroll when closing config dialog
										optionPane.setOptions(startOptions);
										dispose();
								}
							}
						}
					});
			pack();
			setVisible(true);
		}
	}
}
