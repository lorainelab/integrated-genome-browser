package com.affymetrix.igb.util;

import com.affymetrix.genometry.general.ID;
import com.affymetrix.genometry.general.NewInstance;
import com.affymetrix.igb.shared.ConfigureOptionsPanel;
import com.affymetrix.igb.shared.ConfigureOptionsPanel.Filter;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

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

    /**
     * Creates the reusable dialog.
     */
    public ConfigureOptionsDialog(Class clazz, String label) {
        this(clazz, label, null);
    }

    public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter) {
        this(clazz, label, filter, true);
    }

    public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter, Preferences preferences) {
        this(clazz, label, filter, true, preferences);
    }

    public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter, boolean includeNone) {
        this(clazz, label, filter, includeNone, null);
    }

    public ConfigureOptionsDialog(Class clazz, String label, Filter<T> filter, boolean includeNone, Preferences preferences) {
        super((Frame) null, true);
        init(clazz, label, filter, includeNone, preferences);
    }

    private void init(Class clazz, String label, Filter<T> filter, boolean includeNone) throws SecurityException {
        init(clazz, label, filter, includeNone, null);
    }

    private void init(Class clazz, String label, Filter<T> filter, boolean includeNone, Preferences preferences) throws SecurityException {
        configureOptionPanel = new ConfigureOptionsPanel<T>(clazz, label, filter, includeNone, preferences) {
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
        return optionPane.getValue();
    }

    public ConfigureOptionsPanel<T> getConfigureOptionPanel() {
        return configureOptionPanel;
    }

}
