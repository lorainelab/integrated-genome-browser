package com.affymetrix.igb.util;

import com.affymetrix.genometry.general.ID;
import com.affymetrix.genometry.general.IParameters;
import com.affymetrix.genometry.general.NewInstance;
import com.affymetrix.igb.shared.ConfigureOptionsPanel;
import com.affymetrix.igb.shared.ConfigureOptionsPanel.Filter;
import com.affymetrix.igb.tiers.TierLabelManager;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.prefs.Preferences;

/**
 *
 * @author hiralv
 */
@SuppressWarnings("unchecked")
public class ConfigureOptionsDialog<T extends ID & NewInstance> extends JDialog {

    private static final long serialVersionUID = 1L;

    private ConfigureOptionsPanel<T> configureOptionPanel;
    private JOptionPane optionPane;
    private JButton okOption, cancelOption;
    private IParameters iParameters = null;

    /**
     * Creates the reusable dialog.
     */
    public ConfigureOptionsDialog(Class clazz, String label) {
        this(clazz, label, null);
    }

    public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter) {
        this(clazz, label, filter, true);
    }

    public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter, List<Preferences> preferences, TierLabelManager tierLabelManager) {
        this(clazz, label, filter, true, preferences, tierLabelManager);
    }

    public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter, boolean includeNone) {
        this(clazz, label, filter, includeNone, null, null);
    }

    public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter, boolean includeNone, TierLabelManager tierLabelManager) {
        this(clazz, label, filter, includeNone, null, tierLabelManager);
    }

    public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter, boolean includeNone, List<Preferences> preferences, TierLabelManager tierLabelManager) {
        super((Frame) null, true);
        init(clazz, label, filter, includeNone, preferences, tierLabelManager);
    }

    private void init(Class clazz, String label, Filter<T> filter, boolean includeNone) throws SecurityException {
        init(clazz, label, filter, includeNone, null, null);
    }

    private void init(Class clazz, String label, Filter<T> filter, boolean includeNone, List<Preferences> preferences, TierLabelManager tierLabelManager) throws SecurityException {
        configureOptionPanel = new ConfigureOptionsPanel<T>(clazz, label, filter, includeNone, preferences, tierLabelManager) {
            @Override
            public void revalidate() {
                super.revalidate();
                ConfigureOptionsDialog.this.pack();
            }
        };
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

    public IParameters getiParameters() {
        return iParameters;
    }

    public void setiParameters(IParameters iParameters) {
        this.iParameters = iParameters;
        configureOptionPanel.setSaved_IParameters(iParameters);
    }

    private void addListeners() {

        ActionListener al = ae -> {
            if (ae.getSource() == okOption) {
                optionPane.setValue(JOptionPane.OK_OPTION);
            } else {
                optionPane.setValue(JOptionPane.CANCEL_OPTION);
            }
            dispose();
        };
        okOption.addActionListener(al);
        cancelOption.addActionListener(al);
    }

    @Override
    public void setEnabled(boolean b) {
        configureOptionPanel.setEnabled(b);
    }

    public void setInitialValue(T cp) {
        configureOptionPanel.setInitialValue(cp);
    }

    public T showDialog() {
        //If initial value was not set, then set it here
        setVisible(true);
        return configureOptionPanel.getReturnValue(optionPane.getValue() instanceof Integer && (Integer) optionPane.getValue() == JOptionPane.OK_OPTION);
    }

    public Object getValue() {
        configureOptionPanel.getReturnValue(optionPane.getValue() instanceof Integer && (Integer) optionPane.getValue() == JOptionPane.OK_OPTION);
        if(configureOptionPanel.getSaved_IParameters() != null)
           setiParameters(configureOptionPanel.getSaved_IParameters());
        return optionPane.getValue();
    }

    public ConfigureOptionsPanel<T> getConfigureOptionPanel() {
        return configureOptionPanel;
    }

}
