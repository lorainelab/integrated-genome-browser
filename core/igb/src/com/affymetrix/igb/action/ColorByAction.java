package com.affymetrix.igb.action;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map.Entry;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

import com.affymetrix.igb.colorproviders.ColorProviderHolder;
import com.jidesoft.combobox.ColorComboBox;
import cytoscape.visual.ui.editors.continuous.GradientEditorPanel;
import cytoscape.visual.ui.editors.continuous.ColorInterpolator;
import cytoscape.visual.ui.editors.continuous.GradientColorInterpolator;

import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.HeatMapExtended;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.swing.NumericFilter;
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
	
	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		ITrackStyleExtended style = getTierManager().getSelectedTiers().get(0).getAnnotStyle();
		ColorProvider cp = style.getColorProvider();
		
		ColorByDialog colorByDialog = new ColorByDialog();
		colorByDialog.setLocationRelativeTo(getSeqMapView());
		colorByDialog.setInitialValue(cp);
		cp = colorByDialog.showDialog();
		
		style.setColorProvider(cp);
		refreshMap(false, false);
		//TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
	
	private class ColorByDialog extends JDialog {

		private ColorProvider returnValue, selectedCP;
		private JOptionPane optionPane;
		private JComboBox comboBox;
		private JPanel paramsPanel;
		
		/**
		 * Creates the reusable dialog.
		 */
		public ColorByDialog () {
			super((Frame)null, true);
			init();
		}

		private void init() throws SecurityException {
			JPanel pan = new JPanel();
			pan.setLayout(new BorderLayout());
	
			optionPane = new JOptionPane(pan, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			
			setTitle("Color By");
			setResizable(true);
			setContentPane(optionPane);
			//setModal(false);
			setAlwaysOnTop(false);
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			
			comboBox = new JComboBox(ColorProviderHolder.OPTIONS.keySet().toArray());
			comboBox.setSelectedItem(ColorProviderHolder.getCPName(null));
			
			JPanel optionsBox = new JPanel();
			optionsBox.setLayout(new BoxLayout(optionsBox, BoxLayout.X_AXIS));
			optionsBox.add(new JLabel("Color By :  "));
			optionsBox.add(comboBox);
			
			paramsPanel = new JPanel();
			paramsPanel.setLayout(new BoxLayout(paramsPanel, BoxLayout.PAGE_AXIS));
			
			pan.add(optionsBox, BorderLayout.CENTER);
			pan.add(paramsPanel, BorderLayout.PAGE_END);
		
			addListeners();
			pack();
		}

		private void addOptions(final ColorProvider cp, final JPanel paramsPanel) {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			panel.add(new JLabel("                "));
			
			paramsPanel.removeAll();
			if (cp != null && cp.getParameters() != null) {
				for (Entry<String, Class<?>> entry : cp.getParameters().entrySet()) {
					final String label = entry.getKey();
					Class<?> clazz = entry.getValue();
					JComponent component = null;

					if (Number.class.isAssignableFrom(clazz) || String.class.isAssignableFrom(clazz)) {
						final JTextField tf;
						if(Number.class.isAssignableFrom(clazz)){
							tf = new JTextField(6);
							((AbstractDocument)tf.getDocument()).setDocumentFilter(new NumericFilter.FloatNumericFilter());
						}else{
							tf = new JTextField(10);
						}
						tf.setText(String.valueOf(cp.getParameterValue(label)));
						tf.getDocument().addDocumentListener(new DocumentListener(){
							
							public void insertUpdate(DocumentEvent e) { setParameter(); }

							public void removeUpdate(DocumentEvent e) { setParameter(); }

							public void changedUpdate(DocumentEvent e) { setParameter(); }
							
							private void setParameter(){
								cp.setParameter(label, tf.getText());
							}
						});
						
						tf.setMaximumSize(new java.awt.Dimension(60, 20));
						tf.setPreferredSize(new java.awt.Dimension(60, 20));
						tf.setMaximumSize(new java.awt.Dimension(60, 20));
						component = tf;
					} else if (Color.class.isAssignableFrom(clazz)) {
						final ColorComboBox colorComboBox = new ColorComboBox();
						colorComboBox.setSelectedColor((Color)cp.getParameterValue(label));
						colorComboBox.addItemListener(new ItemListener() {
							public void itemStateChanged(ItemEvent e) {
								cp.setParameter(label, e.getItem());
							}
						});
						colorComboBox.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
						colorComboBox.setButtonVisible(false);
						colorComboBox.setColorValueVisible(false);
						colorComboBox.setMaximumSize(new java.awt.Dimension(20, 20));
						colorComboBox.setPreferredSize(new java.awt.Dimension(20, 20));
						colorComboBox.setMaximumSize(new java.awt.Dimension(20, 20));
						//colorComboBox.setStretchToFit(true);
						component = colorComboBox;
					} else if (HeatMapExtended.class.isAssignableFrom(clazz)){
						final GradientEditorPanel editor = new GradientEditorPanel(ColorByDialog.this);
						Object hm = cp.getParameterValue(label);
						float[] positions;
						Color[] colorRanges;
						if (hm instanceof HeatMapExtended) {
							positions = ((HeatMapExtended) hm).getValues();
							colorRanges = ((HeatMapExtended) hm).getRangeColors();
						} else {
							positions = HeatMapExtended.DEFAULT_VALUES;
							colorRanges = HeatMapExtended.DEFAULT_COLORS;
						}
						editor.setVirtualRange(positions, colorRanges);
						
						final JButton editButton = new JButton("Edit");
						editButton.addActionListener(new ActionListener(){
							@Override
							public void actionPerformed(ActionEvent evt) {
								editor.setTitle("Configure Heatmap");
								editor.setModal(true);
								editor.setAlwaysOnTop(false);
								editor.setLocationRelativeTo(ColorByDialog.this);
								editor.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
								Object value = editor.showDialog();
								if(value.equals(JOptionPane.OK_OPTION)){	
									ColorInterpolator colorInterpolator = new GradientColorInterpolator(editor.getVirtualRange());
									cp.setParameter(label, 
											new HeatMapExtended("HeatMapExtended", 
											colorInterpolator.getColorRange(256), 
											editor.getVirtualRange().getVirtualValues(), 
											editor.getVirtualRange().getColors()));
								}
							}
						});
						component = editButton;
					}
					
					if (component != null) {
						panel.add(new JLabel(label));
						panel.add(component);
						panel.add(Box.createHorizontalStrut(30));
					}
					
					if(panel.getComponentCount() > 4){
						paramsPanel.add(Box.createVerticalStrut(10));
						paramsPanel.add(panel);
						panel = new JPanel();
						panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
						panel.add(new JLabel("                "));
					}
				}
			}
			
			if(panel.getComponentCount() > 0){
				paramsPanel.add(Box.createVerticalStrut(10));
				paramsPanel.add(panel);
			}
		}
		
		private void initParamPanel(ColorProvider cp) {
			addOptions(cp, paramsPanel);
			ThreadUtils.runOnEventQueue(new Runnable() {
				public void run() {
					pack();
				}
			});
		}
		
		private void addListeners() {
			comboBox.addItemListener(new ItemListener() {
				
				public void itemStateChanged(ItemEvent e) {
					ColorProvider cp = ColorProviderHolder.getCPInstance(ColorProviderHolder.OPTIONS.get(e.getItem().toString()));
					selectedCP = cp;
					initParamPanel(cp);
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
							returnValue = selectedCP;
							dispose();
						}
					}
				}
			});
		}

		public void setInitialValue(ColorProvider cp){
			comboBox.setSelectedItem(ColorProviderHolder.getCPName(cp == null ? null : cp.getClass()));
			returnValue = cp;
			selectedCP = cp;
			initParamPanel(cp);
		}
		
		public ColorProvider showDialog() {
			setVisible(true);
			return returnValue;
		}
	}
}
