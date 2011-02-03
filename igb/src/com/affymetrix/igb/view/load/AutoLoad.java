package com.affymetrix.igb.view.load;

import java.awt.Adjustable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JScrollBar;

/**
 *
 * @author hiralv
 */
public class AutoLoad implements AdjustmentListener {

	private final Adjustable zoomer;
	private final JScrollBar scroller;

	private final int threshold = 175;

	protected int zoomer_value, scroller_value,prev_zoomer_value, prev_scroller_value;

	public AutoLoad(Adjustable zoomer, JScrollBar scroller){
		this.zoomer = zoomer;
		this.scroller = scroller;
		
		this.zoomer.addAdjustmentListener(this);
		this.scroller.addAdjustmentListener(this);
	}

	public void adjustmentValueChanged(AdjustmentEvent evt) {
		//    System.out.println("adjustmentValueChanged to: " + evt.getValue());
		Adjustable source = evt.getAdjustable();
		//    System.out.println(source);
		if(source != zoomer && source != scroller)
			return;
		
		if (source == zoomer) {

			zoomer_value = source.getValue();
			if (zoomer_value == prev_zoomer_value ||
					zoomer_value < threshold){
				return;
			}

			prev_zoomer_value = zoomer_value;
		}
		else if (source == scroller) {
			
			scroller_value = source.getValue();
			if (scroller_value == prev_scroller_value ||
					zoomer_value < threshold ||
					scroller.getValueIsAdjusting()){
				return;
			}
			
			prev_scroller_value = scroller_value;

		}

		GeneralLoadView.loadVisibleFeatures();
	}


}
