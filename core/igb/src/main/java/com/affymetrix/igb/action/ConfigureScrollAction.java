package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.swing.NumericFilter;
import com.affymetrix.genoviz.widget.AutoScroll;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.swing.JRPTextField;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

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

    protected ConfigureScrollAction(String text, String small_icon, String large_icon) {
        super(text, small_icon, large_icon);
    }

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ConfigureScrollAction getAction() {
        return ACTION;
    }

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
        autoScrollConfigDialog.setVisible(true);
    }

    /**
     * This dialog provides an interactive way for auto scroll configuration
     * e.g. Config dialog remains visible during auto scrolling for updating
     * scroll parameter conveniently Smart to start & stop: supports
     * enter/return key and stop when parameters changes
     */
    class AutoScrollConfigDialog extends JDialog {

        private static final long serialVersionUID = 1L;

        private JOptionPane optionPane;
        private JRPTextField pix_to_scrollTF, time_intervalTF;
        private JLabel bases_per_minuteL, minutes_per_seqL;
        private JButton startOption, stopOption, closeOption;
        private Object[] startOptions, stopOptions;

        /**
         * Creates the reusable dialog.
         */
        public AutoScrollConfigDialog(final Frame aFrame, final SeqMapView seqMapView) {
            super(aFrame, true);
            init(seqMapView);
            addListeners();
        }

        private void init(final SeqMapView seqMapView) throws SecurityException {
            JPanel pan = new JPanel();
            pix_to_scrollTF = new JRPTextField("AutoScrollAction_pix_to_scroll", "" + as_pix_to_scroll);
            time_intervalTF = new JRPTextField("AutoScrollAction_time_interval", "" + as_time_interval);
            startOption = new JButton("Start");
            stopOption = new JButton("Stop");
            closeOption = new JButton("Close");
            startOptions = new Object[]{startOption, closeOption};
            stopOptions = new Object[]{stopOption, closeOption};
            optionPane = new JOptionPane(pan, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION,
                    null, startOptions, startOptions[0]);

            setTitle("AutoScroll Parameters");
            setResizable(false);
            setLocationRelativeTo(seqMapView);
            setContentPane(optionPane);
            //setModal(false);
            setAlwaysOnTop(false);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            float bases_per_minute = (float) // 1000 ==> ms/s , 60 ==> s/minute, as_time_interval ==> ms/scroll
                    (1.0 * autoScroll.get_bases_per_pix() * as_pix_to_scroll * 1000 * 60 / as_time_interval);
            bases_per_minute = Math.abs(bases_per_minute);
            float minutes_per_seq = seqMapView.getViewSeq().getLength() / bases_per_minute;
            bases_per_minuteL = new JLabel("" + (bases_per_minute / 1000000));
            minutes_per_seqL = new JLabel("" + (minutes_per_seq));

            pan.setLayout(new GridLayout(4, 2));
            pan.add(new JLabel("Scroll increment (pixels)"));
            pan.add(pix_to_scrollTF);
            pan.add(new JLabel("Time interval (milliseconds)"));
            pan.add(time_intervalTF);
            pan.add(new JLabel("Megabases per minute:  "));
            pan.add(bases_per_minuteL);
            pan.add(new JLabel("Total minutes for seq:  "));
            pan.add(minutes_per_seqL);

            pack();
        }

        private void addListeners() {
            DocumentListener dl = new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    update(e);
                }

                public void removeUpdate(DocumentEvent e) {
                    update(e);
                }

                public void changedUpdate(DocumentEvent e) {
                    update(e);
                }
            };

            ActionListener al = ae -> {
                if (ae.getSource() == startOption) {
                    start();
                } else if (ae.getSource() == stopOption) {
                    stop();
                } else if (ae.getSource() == closeOption) {
                    stop();
                    dispose();
                }
            };

            ((AbstractDocument) pix_to_scrollTF.getDocument()).setDocumentFilter(new NumericFilter.IntegerNumericFilter(-1000, 1000));
            ((AbstractDocument) time_intervalTF.getDocument()).setDocumentFilter(new NumericFilter.IntegerNumericFilter(1, 1000));
            pix_to_scrollTF.getDocument().addDocumentListener(dl);
            time_intervalTF.getDocument().addDocumentListener(dl);

            startOption.addActionListener(al);
            stopOption.addActionListener(al);
            closeOption.addActionListener(al);
        }

        private void start() {
            StartAutoScrollAction.getAction().start();
            optionPane.setOptions(stopOptions); // Change Start button to Stop
            optionPane.setInitialValue(stopOptions[0]); // Press Enter will trigger the new Stop button
        }

        private void stop() {
            StopAutoScrollAction.getAction().actionPerformed(null);
            optionPane.setOptions(startOptions);
            optionPane.setInitialValue(startOptions[0]);
        }

        private void update(DocumentEvent e) {
            // Stop before try/catch block
            if (autoScroll.isScrolling()) {
                stop();

                // Regain the focus
                // Todo: Need to get source from the event
                if (e.getDocument() == pix_to_scrollTF.getDocument()) {
                    pix_to_scrollTF.requestFocusInWindow();
                } else if (e.getDocument() == time_intervalTF.getDocument()) {
                    time_intervalTF.requestFocusInWindow();
                }
            }

            try {
                as_pix_to_scroll = Integer.parseInt(pix_to_scrollTF.getText());
                as_time_interval = Integer.parseInt(time_intervalTF.getText());

                float bases_per_minute = (float) // 1000 ==> ms/s , 60 ==> s/minute, as_time_interval ==> ms/scroll
                        (1.0 * autoScroll.get_bases_per_pix() * as_pix_to_scroll * 1000 * 60 / as_time_interval);
                bases_per_minute = Math.abs(bases_per_minute);
                float minutes_per_seq = (autoScroll.get_end_pos() - autoScroll.get_start_pos()) / bases_per_minute;
                bases_per_minuteL.setText("" + (bases_per_minute / 1000000));
                minutes_per_seqL.setText("" + (minutes_per_seq));

                startOption.setEnabled(true);
                stopOption.setEnabled(true);

                autoScroll.configure(as_pix_to_scroll, as_time_interval);

            } catch (NumberFormatException nfe) {
                bases_per_minuteL.setText("N/A");
                minutes_per_seqL.setText("N/A");

                startOption.setEnabled(false);
                stopOption.setEnabled(false);
            }

        }
    }
}
