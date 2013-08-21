package com.affymetrix.igb.util;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.general.ID;
import com.affymetrix.genometryImpl.general.IParameters;
import com.affymetrix.genometryImpl.general.NewInstance;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.HeatMapExtended;
import com.affymetrix.genometryImpl.util.IDComparator;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.swing.NumericFilter;
import com.jidesoft.combobox.ColorComboBox;
import cytoscape.visual.ui.editors.continuous.ColorInterpolator;
import cytoscape.visual.ui.editors.continuous.GradientColorInterpolator;
import cytoscape.visual.ui.editors.continuous.GradientEditorPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

/**
 *
 * @author hiralv
 */
@SuppressWarnings("unchecked")
public class ConfigureOptionsDialog<T extends ID & NewInstance> extends JDialog {

	private T returnValue, selectedCP;
	private JOptionPane optionPane;
	private JComboBox comboBox;
	private JPanel paramsPanel;
	private JButton okOption, cancelOption;
	private Map<String, T> name2CP;
	private Map<String, Object> paramMap;
	
	/**
	 * Creates the reusable dialog.
	 */
	public ConfigureOptionsDialog(Class clazz, String label) {
		super((Frame) null, true);
		init(clazz, label);
	}

	private void init(Class clazz, String label) throws SecurityException {
		JPanel pan = new JPanel();
		pan.setLayout(new BorderLayout());

		okOption = new JButton("OK");
		cancelOption = new JButton("Cancel");
		Object[] options = new Object[]{okOption, cancelOption};

		optionPane = new JOptionPane(pan, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);
		optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

		setResizable(true);
		setContentPane(optionPane);
		//setModal(false);
		setAlwaysOnTop(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		comboBox = new JComboBox();
		TreeSet<T> colorProviders = new TreeSet<T>(new IDComparator());
		colorProviders.addAll(ExtensionPointHandler.getExtensionPoint(clazz).getExtensionPointImpls());
		name2CP = new HashMap<String, T>();

		comboBox.addItem("None");
		for (T cp : colorProviders) {
			name2CP.put(cp.getDisplay(), cp);
			comboBox.addItem(cp.getDisplay());
		}
		comboBox.setSelectedItem("None");

		JPanel optionsBox = new JPanel();
		optionsBox.setLayout(new BoxLayout(optionsBox, BoxLayout.X_AXIS));
		optionsBox.add(new JLabel(label+" :  "));
		optionsBox.add(comboBox);

		paramsPanel = new JPanel();
		paramsPanel.setLayout(new BoxLayout(paramsPanel, BoxLayout.PAGE_AXIS));

		pan.add(optionsBox, BorderLayout.CENTER);
		pan.add(paramsPanel, BorderLayout.PAGE_END);

		addListeners();
		pack();
	}

	private void addOptions(final IParameters cp, final JPanel paramsPanel) {
		paramMap = new HashMap<String, Object>();
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new JLabel("                "));

		paramsPanel.removeAll();
		if (cp != null && cp.getParametersType() != null) {
			for (Entry<String, Class<?>> entry : cp.getParametersType().entrySet()) {
				final String label = entry.getKey();
				final Class<?> clazz = entry.getValue();
				JComponent component = null;

				if (Number.class.isAssignableFrom(clazz) || String.class.isAssignableFrom(clazz)) {
					final JTextField tf;
					if (Number.class.isAssignableFrom(clazz)) {
						tf = new JTextField(6);
						if(Integer.class.isAssignableFrom(clazz)){
							((AbstractDocument) tf.getDocument()).setDocumentFilter(new NumericFilter.IntegerNumericFilter());
						} else {
							((AbstractDocument) tf.getDocument()).setDocumentFilter(new NumericFilter.FloatNumericFilter());
						}
						
					} else {
						tf = new JTextField(10);
					}
					tf.setText(String.valueOf(cp.getParameterValue(label)));
					tf.getDocument().addDocumentListener(new DocumentListener() {
						public void insertUpdate(DocumentEvent e) {
							setParameter();
						}

						public void removeUpdate(DocumentEvent e) {
							setParameter();
						}

						public void changedUpdate(DocumentEvent e) {
							setParameter();
						}

						private void setParameter() {
							if (Number.class.isAssignableFrom(clazz)) {
								if(tf.getText() != null && tf.getText().length() > 0) {
									if(Integer.class.isAssignableFrom(clazz)){
										ConfigureOptionsDialog.this.setParameter(cp, label, Integer.valueOf(tf.getText()));
									} else {
										ConfigureOptionsDialog.this.setParameter(cp, label, Float.valueOf(tf.getText()));
									}
								}
							} else {
								ConfigureOptionsDialog.this.setParameter(cp, label, tf.getText());
							}
						}
					});

					tf.setMaximumSize(new java.awt.Dimension(60, 20));
					tf.setPreferredSize(new java.awt.Dimension(60, 20));
					tf.setMaximumSize(new java.awt.Dimension(60, 20));
					component = tf;
				} else if (Color.class.isAssignableFrom(clazz)) {
					final ColorComboBox colorComboBox = new ColorComboBox();
					colorComboBox.setSelectedColor((Color) cp.getParameterValue(label));
					colorComboBox.addItemListener(new ItemListener() {
						public void itemStateChanged(ItemEvent e) {
							setParameter(cp, label, e.getItem());
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
				} else if (HeatMapExtended.class.isAssignableFrom(clazz)) {
					final GradientEditorPanel editor = new GradientEditorPanel(ConfigureOptionsDialog.this);
					Object hm = cp.getParameterValue(label);
					float[] positions;
					Color[] colorRanges;
					if (hm instanceof HeatMapExtended) {
						positions = ((HeatMapExtended) hm).getValues();
						colorRanges = ((HeatMapExtended) hm).getRangeColors();
					} else {
						HeatMapExtended hme = (HeatMapExtended)cp.getParameterValue(label);
						positions = hme.getValues();
						colorRanges = hme.getColors();
					}
					editor.setVirtualRange(positions, colorRanges);

					final JButton editButton = new JButton("Edit");
					editButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent evt) {
							editor.setTitle("Configure Heatmap");
							editor.setModal(true);
							editor.setAlwaysOnTop(false);
							editor.setLocationRelativeTo(ConfigureOptionsDialog.this);
							editor.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
							Object value = editor.showDialog();
							if (value.equals(JOptionPane.OK_OPTION)) {
								ColorInterpolator colorInterpolator = new GradientColorInterpolator(editor.getVirtualRange());
								setParameter(cp, label, new HeatMapExtended("HeatMapExtended",
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

				if (panel.getComponentCount() > 4) {
					paramsPanel.add(Box.createVerticalStrut(10));
					paramsPanel.add(panel);
					panel = new JPanel();
					panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
					panel.add(new JLabel("                "));
				}
			}
		}

		if (panel.getComponentCount() > 0) {
			paramsPanel.add(Box.createVerticalStrut(10));
			paramsPanel.add(panel);
		}
	}

	private void setParameter(IParameters cp, String key, Object value) {
//		boolean isValid = cp.setParameterValue(key, value);
//		okOption.setEnabled(isValid);
		paramMap.put(key, value);
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
				T cp = name2CP.get((String) e.getItem());
				// If a user selects same color provider as initial then reuses the same object
				if (returnValue != null && cp != null && cp.getName().equals(returnValue.getName())) {
					cp = returnValue;
				} else if (cp != null) {
					cp = (T) cp.newInstance();
				}
				selectedCP = cp;
				if (cp instanceof IParameters) {
					initParamPanel((IParameters) cp);
				}
			}
		});

		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (ae.getSource() == okOption) {
					returnValue = selectedCP;
					if(returnValue instanceof IParameters){
						((IParameters)returnValue).setParametersValue(paramMap);
					}
					optionPane.setValue(JOptionPane.OK_OPTION);
				} else {
					optionPane.setValue(JOptionPane.CANCEL_OPTION);
				}
				dispose();
			}
		};
		okOption.addActionListener(al);
		cancelOption.addActionListener(al);
	}

	@Override
	public void setEnabled(boolean b){
		comboBox.setEnabled(b);
	}
	
	public void setInitialValue(T cp) {
		if (cp == null) {
			comboBox.setSelectedItem("None");
		} else {
			comboBox.setSelectedItem(cp.getDisplay());
		}
		returnValue = cp;
		selectedCP = cp;
		if (cp instanceof IParameters) {
			initParamPanel((IParameters) cp);
		}
	}

	public T showDialog() {
		setVisible(true);
		return returnValue;
	}
	
	public Object getValue(){
		return optionPane.getValue();
	}
}
