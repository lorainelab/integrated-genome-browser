package com.affymetrix.igb.action;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import com.jidesoft.combobox.ColorComboBox;

import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.SeqMapView;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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

		private JOptionPane optionPane;
		private JComboBox comboBox;
		private JPanel paramsPanel;
		
		/**
		 * Creates the reusable dialog.
		 */
		public AutoScrollConfigDialog (final SeqMapView seqMapView) {
			super((Frame)null, true);
			init(seqMapView);
			addListeners();
		}

		private void init(final SeqMapView seqMapView) throws SecurityException {
			JPanel pan = new JPanel();
			pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
			
			optionPane = new JOptionPane(pan, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			
			setTitle("Color By");
			setResizable(false);
			setLocationRelativeTo(seqMapView);
			setContentPane(optionPane);
			//setModal(false);
			setAlwaysOnTop(false);
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			
			comboBox = new JComboBox(new String[]{"None", "RGB", "Score", "Strand"});
			Box optionsBox = Box.createHorizontalBox();
			optionsBox.add(new JLabel("Color By :  "));
			optionsBox.add(comboBox);
			
			final JPanel emptyBox = new JPanel();
			emptyBox.setLayout(new BoxLayout(emptyBox, BoxLayout.X_AXIS));
			emptyBox.add(new JLabel("            "));
			
			paramsPanel = new JPanel();
			paramsPanel.setLayout(new BoxLayout(paramsPanel, BoxLayout.X_AXIS));
						
			emptyBox.add(paramsPanel);
			pan.add(optionsBox);
			pan.add(emptyBox);
			pack();
		}

		private void addOptions(String label, Class clazz, JPanel pan){
			pan.add(new JLabel(label));
			if(Number.class.isAssignableFrom(clazz)){
				JTextField tf = new JTextField(6);
				tf.setMaximumSize(new java.awt.Dimension(50, 20));
				tf.setPreferredSize(new java.awt.Dimension(50, 20));
				tf.setMaximumSize(new java.awt.Dimension(50, 20));
				pan.add(tf);
			} else if(Color.class.isAssignableFrom(clazz)) {
				ColorComboBox colorComboBox = new ColorComboBox();
				colorComboBox.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
				colorComboBox.setButtonVisible(false);
				colorComboBox.setColorValueVisible(false);
				colorComboBox.setMaximumSize(new java.awt.Dimension(20, 20));
				colorComboBox.setPreferredSize(new java.awt.Dimension(20, 20));
				colorComboBox.setMaximumSize(new java.awt.Dimension(20, 20));
				//colorComboBox.setStretchToFit(true);
				pan.add(colorComboBox);
			}
		}
		
		private void addListeners() {
			comboBox.addItemListener(new ItemListener() {
				
				public void itemStateChanged(ItemEvent e) {
					paramsPanel.removeAll();
					if(e.getItem() == "RGB"){
						// Add Nothing
					} else if (e.getItem() == "Score") {
						addOptions("min", Float.class, paramsPanel);
						paramsPanel.add(Box.createHorizontalStrut(30));
						addOptions("min", Float.class, paramsPanel);
					} else if (e.getItem() == "Strand") {
						addOptions("+", Color.class, paramsPanel);
						paramsPanel.add(Box.createHorizontalStrut(30));
						addOptions("-", Color.class, paramsPanel);
					}
					
					ThreadUtils.runOnEventQueue(new Runnable() {
						public void run() {
							pack();
						}
					});
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
