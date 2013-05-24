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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
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

import com.jidesoft.combobox.ColorComboBox;
import cytoscape.visual.ui.editors.continuous.GradientEditorPanel;
import cytoscape.visual.ui.editors.continuous.ColorInterpolator;
import cytoscape.visual.ui.editors.continuous.GradientColorInterpolator;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.color.ColorProviderI;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.general.IParameters;
import com.affymetrix.genometryImpl.util.IDComparator;
import com.affymetrix.genometryImpl.style.HeatMap;
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
		ColorProviderI cp = style.getColorProvider();
		
		ColorByDialog colorByDialog = new ColorByDialog();
		colorByDialog.setLocationRelativeTo(getSeqMapView());
		colorByDialog.setInitialValue(cp);
		cp = colorByDialog.showDialog();
		
		style.setColorProvider(cp);
		refreshMap(false, false);
		//TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
	
	private class ColorByDialog extends JDialog {

		private ColorProviderI returnValue, selectedCP;
		private JOptionPane optionPane;
		private JComboBox comboBox;
		private JPanel paramsPanel;
		private JButton okOption, cancelOption;
		private Map<String, ColorProviderI> name2CP;
		
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
	
			okOption = new JButton("OK");
			cancelOption = new JButton("Cancel");
			Object[] options = new Object[]{okOption, cancelOption};
			
			optionPane = new JOptionPane(pan, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			
			setTitle("Color By");
			setResizable(true);
			setContentPane(optionPane);
			//setModal(false);
			setAlwaysOnTop(false);
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			
			comboBox = new JComboBox();
			TreeSet<ColorProviderI> colorProviders = new TreeSet<ColorProviderI>(new IDComparator());
			colorProviders.addAll(ExtensionPointHandler.getExtensionPoint(ColorProviderI.class).getExtensionPointImpls());
			name2CP = new HashMap<String, ColorProviderI>();
			
			comboBox.addItem("None");
			for(ColorProviderI cp : colorProviders){
				name2CP.put(cp.getDisplay(), cp);
				comboBox.addItem(cp.getDisplay());
			}
			comboBox.setSelectedItem("None");
			
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

		private void addOptions(final IParameters cp, final JPanel paramsPanel) {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			panel.add(new JLabel("                "));
			
			paramsPanel.removeAll();
			if (cp != null && cp.getParametersType() != null) {
				for (Entry<String, Class<?>> entry : cp.getParametersType().entrySet()) {
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
								cp.setParameterValue(label, tf.getText());
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
								cp.setParameterValue(label, e.getItem());
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
									cp.setParameterValue(label, 
											new HeatMapExtended("HeatMapExtended", 
											colorInterpolator.getColorRange(HeatMap.BINS), 
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
		
		private void initParamPanel(IParameters cp) {
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
					ColorProviderI cp = name2CP.get((String)e.getItem());
					// If a user selects same color provider as initial then reuses the same object
					if(returnValue != null && cp != null && cp.getName().equals(returnValue.getName())){
						cp = returnValue;
					} else if(cp != null){
						cp = cp.clone();
					}
					selectedCP = cp;
					if(cp instanceof IParameters){
						initParamPanel((IParameters)cp);
					}
				}
			});

			ActionListener al = new ActionListener(){
				public void actionPerformed(ActionEvent ae) {
					optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
					if (ae.getSource() == okOption) {	
						returnValue = selectedCP;
					} 
					dispose();
				}
			};
			okOption.addActionListener(al);
			cancelOption.addActionListener(al);
		}

		public void setInitialValue(ColorProviderI cp){
			if(cp == null){
				comboBox.setSelectedItem("None");
			}else{
				comboBox.setSelectedItem(cp.getDisplay());
			}
			returnValue = cp;
			selectedCP = cp;
			if(cp instanceof IParameters){
				initParamPanel((IParameters)cp);
			}
		}
		
		public ColorProviderI showDialog() {
			setVisible(true);
			return returnValue;
		}
	}
}
