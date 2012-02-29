package com.affymetrix.igb.tutorial;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.recordplayback.RPAdjustableJSlider;
import com.affymetrix.genoviz.swing.recordplayback.RecordPlaybackHolder;
import java.awt.event.ActionEvent;
import javax.swing.SwingWorker;

public class TweeningZoomAction extends GenericAction implements IAmount {

	private static final long serialVersionUID = 1L;
	private static final int STEPS = 30;
	private static final TweeningZoomAction ACTION = new TweeningZoomAction();
	private double amount = 0.2;

	public static TweeningZoomAction getAction() {
		return ACTION;
	}

	private TweeningZoomAction() {
		super();
	}

	@Override
	public String getText() {
		return null;
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
				if (Math.abs(newNextValue - endValue) < Math.abs(2 * step)) {
					actionDone();
				} else {
					getSw(xzoomer, newNextValue, endValue, step).execute();
				}
			}
		};
	}

	public void execute() {
		final RPAdjustableJSlider xzoomer = (RPAdjustableJSlider) RecordPlaybackHolder.getInstance().getWidget("SeqMapView_xzoomer");
		int min = xzoomer.getMinimum();
		int max = xzoomer.getMaximum();
		int zoomAmount = (int) ((max - min) * amount);
		int newValue = xzoomer.getValue();
//		if (newValue + zoomAmount < min) {
//			newValue = min - zoomAmount;
//		}
//		else if (newValue + zoomAmount > max) {
//			newValue = max - zoomAmount;
//		}
		xzoomer.setValue(newValue);
		int startValue = newValue;
		//int endValue = newValue + zoomAmount;
		int endValue = zoomAmount;
		int step = (endValue - startValue) / STEPS;
		getSw(xzoomer, startValue + step, endValue, step).execute();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		execute();
	}
}
