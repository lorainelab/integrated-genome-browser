package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import javax.swing.event.MouseInputAdapter;

import com.affymetrix.igb.tiers.AccordionTierResizer;
import com.affymetrix.igb.tiers.TierResizer;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class TrackResizingAction extends SeqMapViewActionA {
	private static final TrackResizingAction trackAjustAllAction = new TrackResizingAction(BUNDLE.getString("adjustAllTracks"),null, null);
	private static final TrackResizingAction trackAdjustAdjacentAction = new TrackResizingAction(BUNDLE.getString("adjustAdjacentTracks"),null, null);
	
	static{
		trackAjustAllAction.setMouseInputAdapter(new AccordionTierResizer(trackAjustAllAction.getTierMap()));
		trackAdjustAdjacentAction.setMouseInputAdapter(new TierResizer(trackAdjustAdjacentAction.getTierMap()));
		
		trackAjustAllAction.setOtherAction(trackAdjustAdjacentAction);
		trackAdjustAdjacentAction.setOtherAction(trackAjustAllAction);
		
		trackAjustAllAction.actionPerformed(null);
	}
	
	public static TrackResizingAction getAdjustAllAction(){
		return trackAjustAllAction;
	}
	
	public static TrackResizingAction getAdjustAdjacentAction(){
		return trackAdjustAdjacentAction;
	}
	
	TrackResizingAction otherAction;
	MouseInputAdapter resizer;
	protected TrackResizingAction(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}
	
	private void setMouseInputAdapter(MouseInputAdapter resizer){
		this.resizer = resizer;
	}
	
	private void setOtherAction(TrackResizingAction otherAction){
		this.otherAction = otherAction;
	}
	
	@Override
	public boolean isToggle() {
		return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		otherAction.removeListeners();
		otherAction.putValue(SELECTED_KEY, false);
		
		this.addListeners();
		this.putValue(SELECTED_KEY, true);
	}
	
	protected final void addListeners(){
		getLabelMap().addMouseListener(resizer);
		getLabelMap().addMouseMotionListener(resizer);
	}
	
	protected final void removeListeners(){
		getLabelMap().removeMouseListener(resizer);
		getLabelMap().removeMouseMotionListener(resizer);
	}
}
