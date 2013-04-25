package com.affymetrix.igb.action;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import com.jidesoft.combobox.ColorComboBox;

import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.color.RGB;
import com.affymetrix.genometryImpl.color.Score;
import com.affymetrix.genometryImpl.color.Strand;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.SeqMapView;
import static com.affymetrix.igb.IGBConstants.BUNDLE;


/**
 *
 * @author hiralv
 */
public class ColorByAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final ColorByAction ACTION = new ColorByAction("colorByAction");
		
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ColorByAction getAction() {
		return ACTION;
	}

	private ColorByAction(String transKey) {
		super(BUNDLE.getString(transKey) , "16x16/actions/blank_placeholder.png", null);
	}
	
	private void setColorBy(TierLabelGlyph tlg, ColorProvider cp) {
		ITrackStyleExtended style = tlg.getReferenceTier().getAnnotStyle();
		
		refreshMap(false, false);
	}

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		AutoScrollConfigDialog autoScrollConfigDialog = new AutoScrollConfigDialog(getSeqMapView());
		autoScrollConfigDialog.setVisible(true);
		//setColorBy(getTierManager().getSelectedTierLabels().get(0), RGB.getInstance());
		//TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
	
	class AutoScrollConfigDialog extends JDialog {

		private JPanel paramsPanel;
		private Map<String, Class<? extends ColorProvider>> options;
		
		/**
		 * Creates the reusable dialog.
		 */
		public AutoScrollConfigDialog (final SeqMapView seqMapView) {
			super((Frame)null, true);
			
			options = new HashMap<String, Class<? extends ColorProvider>>();
			options.put("RGB", RGB.class);
			options.put("Score", Score.class);
			options.put("Strand", Strand.class);
			
			init(seqMapView);
		}

		private void init(final SeqMapView seqMapView) throws SecurityException {
			JPanel pan = new JPanel();
			pan.setLayout(new BorderLayout());
	
			JOptionPane optionPane = new JOptionPane(pan, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			
			setTitle("Color By");
			setResizable(true);
			setLocationRelativeTo(seqMapView);
			setContentPane(optionPane);
			//setModal(false);
			setAlwaysOnTop(false);
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			
			JComboBox comboBox = new JComboBox(options.keySet().toArray());
			JPanel optionsBox = new JPanel();
			optionsBox.setLayout(new BoxLayout(optionsBox, BoxLayout.X_AXIS));
			optionsBox.add(new JLabel("Color By :  "));
			optionsBox.add(comboBox);
			
			paramsPanel = new JPanel();
			paramsPanel.setLayout(new BoxLayout(paramsPanel, BoxLayout.LINE_AXIS));
			
			pan.add(optionsBox, BorderLayout.CENTER);
			pan.add(paramsPanel, BorderLayout.PAGE_END);
			initParamPanel(comboBox.getSelectedItem().toString());
			addListeners(comboBox, optionPane);
			pack();
		}

		private void addOptions(ColorProvider cp, JPanel pan) {
			pan.removeAll();
			pan.add(new JLabel("                "));
			if (cp.getParameters() != null) {
				for (Entry<String, Class<?>> entry : cp.getParameters().entrySet()) {
					String label = entry.getKey();
					Class<?> clazz = entry.getValue();
					JComponent component = null;

					if (Number.class.isAssignableFrom(clazz)) {
						JTextField tf = new JTextField(6);
						tf.setText(String.valueOf(cp.getParameterValue(label)));
						tf.setMaximumSize(new java.awt.Dimension(60, 20));
						tf.setPreferredSize(new java.awt.Dimension(60, 20));
						tf.setMaximumSize(new java.awt.Dimension(60, 20));
						component = tf;

					} else if (Color.class.isAssignableFrom(clazz)) {
						ColorComboBox colorComboBox = new ColorComboBox();
						colorComboBox.setSelectedColor((Color)cp.getParameterValue(label));
						colorComboBox.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
						colorComboBox.setButtonVisible(false);
						colorComboBox.setColorValueVisible(false);
						colorComboBox.setMaximumSize(new java.awt.Dimension(20, 20));
						colorComboBox.setPreferredSize(new java.awt.Dimension(20, 20));
						colorComboBox.setMaximumSize(new java.awt.Dimension(20, 20));
						//colorComboBox.setStretchToFit(true);
						component = colorComboBox;
					}
					
					if (component != null) {
						pan.add(new JLabel(label));
						pan.add(component);
						pan.add(Box.createHorizontalStrut(30));
					}
				}
			}
		}
		
		private void initParamPanel(String selectedItem) {
			try {
				ColorProvider cp = options.get(selectedItem).getConstructor().newInstance();
				addOptions(cp, paramsPanel);

				ThreadUtils.runOnEventQueue(new Runnable() {
					public void run() {
						pack();
					}
				});
			} catch (Exception ex) {
			}
		}
		
		private void addListeners(final JComboBox comboBox, final JOptionPane optionPane) {
			comboBox.addItemListener(new ItemListener() {
				
				public void itemStateChanged(ItemEvent e) {
					initParamPanel(e.getItem().toString());
				}
			});

			optionPane.addPropertyChangeListener(new PropertyChangeListener(){
				public void propertyChange(PropertyChangeEvent evt) {
					Object value = optionPane.getValue();
					if(value != null){
						if(value.equals(JOptionPane.CANCEL_OPTION)){
							optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
							dispose();
						}else if (value.equals(JOptionPane.OK_OPTION)){
							optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
							dispose();
						}
					}
				}
			});
		}
		
	}
}
