package com.affymetrix.igb.shared;

import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.igb.util.ConfigureOptionsDialog;
import javax.swing.JOptionPane;

/**
 *
 * @author hiralv
 */
public class TrackOperationWithParametersAction extends TrackOperationAction {

    private static final long serialVersionUID = 1L;

    public TrackOperationWithParametersAction(Operator operator) {
        super(operator);
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        ConfigureOptionsDialog<Operator> optionDialog = new ConfigureOptionsDialog<>(Operator.class, "Track Operation");
        optionDialog.setTitle("Track Operation");
        optionDialog.setLocationRelativeTo(getSeqMapView());
        optionDialog.setInitialValue(getOperator());
//        optionDialog.setEnabled(false);
        optionDialog.setVisible(true);
        Object value = optionDialog.getValue();

        if (value != null && (Integer) value == JOptionPane.OK_OPTION) {
            super.actionPerformed(e);
        }
    }
}
