package com.affymetrix.igb.tutorial;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.swing.RPAdjustableJSlider;
import com.affymetrix.igb.swing.script.ScriptManager;
import java.awt.event.ActionEvent;
import javax.swing.SwingWorker;

public class TweeningZoomAction extends GenericAction implements IAmount {

    private static final long serialVersionUID = 1L;
    private static final int STEPS = 30;
    private static final TweeningZoomAction ACTION = new TweeningZoomAction();
    private double amount = 0.2;
    private boolean lastStep;

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static TweeningZoomAction getAction() {
        return ACTION;
    }

    private TweeningZoomAction() {
        super(null, null, null);
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    private SwingWorker<Void, Void> getSw(final RPAdjustableJSlider xzoomer, final int nextValue, final int endValue, final int step) {
        return new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                xzoomer.setValue(nextValue);
                return null;
            }

            @Override
            protected void done() {
                int newNextValue = nextValue + step;
                if (Math.abs(newNextValue - endValue) <= Math.abs(step) && !lastStep) {
                    int finalStep = step - Math.abs(newNextValue - endValue);
                    lastStep = true;
                    getSw(xzoomer, newNextValue, endValue, finalStep).execute();
                } else if (lastStep) {
                    actionDone();
                } else {
                    getSw(xzoomer, newNextValue, endValue, step).execute();
                }
            }
        };
    }

    public void execute() {
        final RPAdjustableJSlider xzoomer = (RPAdjustableJSlider) ScriptManager.getInstance().getWidget("SeqMapView_xzoomer");
        int min = xzoomer.getMinimum();
        int max = xzoomer.getMaximum();
        int zoomAmount = (int) ((max - min) * amount);
        int newValue = xzoomer.getValue();
        if (newValue + zoomAmount < min) {
            newValue = 0;
        } else if (newValue + zoomAmount > max) {
            newValue = max;
        }
        //xzoomer.setValue(newValue);
        int startValue = xzoomer.getValue();
        int endValue = newValue + zoomAmount;
        int step = (endValue - startValue) / STEPS;
        lastStep = false;
        getSw(xzoomer, startValue + step, endValue, step).execute();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        execute();
    }
}
