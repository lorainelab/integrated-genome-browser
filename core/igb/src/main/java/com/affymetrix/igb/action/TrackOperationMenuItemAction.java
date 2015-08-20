package com.affymetrix.igb.action;

import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.operator.service.OperatorServiceRegistry;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.util.IDComparator;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.TrackOperationAction;
import com.affymetrix.igb.shared.TrackUtils;
import com.affymetrix.igb.util.ConfigureOptionsDialog;
import com.google.common.collect.Lists;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class TrackOperationMenuItemAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(TrackOperationMenuItemAction.class);
    private final List<RootSeqSymmetry> syms;

    public TrackOperationMenuItemAction(List<RootSeqSymmetry> syms) {
        this.syms = syms;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (syms.isEmpty()) {
            return;
        }
        FileTypeCategory fileTypeCategory = syms.get(0).getCategory();
        boolean allSameType = syms.stream().map(sym -> sym.getCategory()).allMatch(ftc -> ftc == fileTypeCategory);
        if (!allSameType) {
            return;
        }
        Set<Operator> allOperators = new TreeSet<>(new IDComparator());
        allOperators.addAll(OperatorServiceRegistry.getOperators());
        int size = syms.size();
        List<Operator> matchingOperators = Lists.newArrayList();
        for (Operator operator : allOperators) {
            if (operator.getOperandCountMin(fileTypeCategory) > 0) {
                if (size == 0 && TrackUtils.getInstance().checkCompatible(Lists.newArrayList(syms.get(0)), operator)) {
                    matchingOperators.add(operator);
                } else if (TrackUtils.getInstance().checkCompatible(syms, operator)) {
                    matchingOperators.add(operator);
                }
            }
        }

        ConfigureOptionsDialog<Operator> configureOptionsDialog = new ConfigureOptionsDialog<>(Operator.class, "Track Operation", operator -> matchingOperators.contains(operator));
        configureOptionsDialog.setTitle("Track Operation");
        configureOptionsDialog.setLocationRelativeTo(IGB.getInstance().getMapView());
        configureOptionsDialog.setInitialValue(matchingOperators.get(0));
        Operator showDialog = configureOptionsDialog.showDialog();
        Object value = configureOptionsDialog.getValue();
        if (value != null && value instanceof Integer &&(Integer) value == JOptionPane.OK_OPTION) {
            if (showDialog == null) {
                return;
            }
            new TrackOperationAction(showDialog).actionPerformed(e);
        }
    }

}
