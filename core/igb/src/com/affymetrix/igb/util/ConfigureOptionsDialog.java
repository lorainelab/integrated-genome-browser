package com.affymetrix.igb.util;

import com.affymetrix.igb.shared.ConfigureOptionsPanel;
import com.affymetrix.genometryImpl.general.ID;
import com.affymetrix.genometryImpl.general.NewInstance;
import com.affymetrix.igb.shared.ConfigureOptionsPanel.Filter;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 *
 * @author hiralv
 */
@SuppressWarnings("unchecked")
public class ConfigureOptionsDialog<T extends ID & NewInstance> extends JDialog {

	private ConfigureOptionsPanel<T> configureOptionPanel;
	private JOptionPane optionPane;
	private JButton okOption, cancelOption;
	
	/**
	 * Creates the reusable dialog.
	 */
	public ConfigureOptionsDialog(Class clazz, String label) {
		this(clazz, label, null);
	}

	public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter) {
		this(clazz, label, filter, true);
	}
	
	public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter, boolean includeNone) {
		super((Frame) null, true);
		init(clazz, label, filter, includeNone);
	}
	
	private void init(Class clazz, String label, Filter<T> filter, boolean includeNone) throws SecurityException {
		configureOptionPanel = new ConfigureOptionsPanel<T>(clazz, label, filter, includeNone);

		okOption = new JButton("OK");
		cancelOption = new JButton("Cancel");
		Object[] options = new Object[]{okOption, cancelOption};

		optionPane = new JOptionPane(configureOptionPanel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);
		optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

		setResizable(true);
		setContentPane(optionPane);
		//setModal(false);
		setAlwaysOnTop(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		addListeners();
		pack();
	}

	private void addListeners() {

		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (ae.getSource() == okOption) {
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
		configureOptionPanel.setEnabled(b);
	}
	
	public void setInitialValue(T cp) {
		configureOptionPanel.setInitialValue(cp);
	}
	
	public T showDialog() {
		//If initial value was not set, then set it here
		setVisible(true);
		return configureOptionPanel.getReturnValue(optionPane.getValue() instanceof Integer && (Integer)optionPane.getValue() == JOptionPane.OK_OPTION); 
	}
	
	public Object getValue(){
		configureOptionPanel.getReturnValue(optionPane.getValue() instanceof Integer && (Integer)optionPane.getValue() == JOptionPane.OK_OPTION); 
		return optionPane.getValue();
	}
}
