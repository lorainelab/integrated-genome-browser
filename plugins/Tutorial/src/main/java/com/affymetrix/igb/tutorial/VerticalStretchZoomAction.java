/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.tutorial;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.swing.RPAdjustableJSlider;
import com.affymetrix.igb.swing.ScriptManager;
import java.awt.event.ActionEvent;
import javax.swing.SwingWorker;

/**
 *
 * @author dcnorris
 */
public class VerticalStretchZoomAction extends GenericAction implements IAmount {

    private static final long serialVersionUID = 1L;
    private static final int STEPS = 30;
    private static final VerticalStretchZoomAction ACTION = new VerticalStretchZoomAction();
    private double amount = 0.2;
    private boolean lastStep;

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static VerticalStretchZoomAction getAction() {
        return ACTION;
    }

    private VerticalStretchZoomAction() {
        super(null, null, null);
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    private SwingWorker<Void, Void> getSw(final RPAdjustableJSlider yzoomer, final int nextValue, final int endValue, final int step) {
        return new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                yzoomer.setValue(nextValue);
                return null;
            }

            @Override
            protected void done() {
                int newNextValue = nextValue + step;
                if (Math.abs(newNextValue - endValue) <= Math.abs(step) && !lastStep) {
                    int finalStep = step - Math.abs(newNextValue - endValue);
                    lastStep = true;
                    getSw(yzoomer, newNextValue, endValue, finalStep).execute();
                } else if (lastStep) {
                    actionDone();
                } else {
                    getSw(yzoomer, newNextValue, endValue, step).execute();
                }
            }
        };
    }

    public void execute() {
        final RPAdjustableJSlider yzoomer = (RPAdjustableJSlider) ScriptManager.getInstance().getWidget("SeqMapView_yzoomer");
        int min = yzoomer.getMinimum();
        int max = yzoomer.getMaximum();
        int zoomAmount = (int) ((max - min) * amount);
        int newValue = yzoomer.getValue();
        if (newValue + zoomAmount < min) {
            newValue = 0;
        } else if (newValue + zoomAmount > max) {
            newValue = max;
        }
        //yzoomer.setValue(newValue);
        int startValue = yzoomer.getValue();
        int endValue = newValue + zoomAmount;
        int step = (endValue - startValue) / STEPS;
        lastStep = false;
        getSw(yzoomer, startValue + step, endValue, step).execute();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        execute();
    }
}
