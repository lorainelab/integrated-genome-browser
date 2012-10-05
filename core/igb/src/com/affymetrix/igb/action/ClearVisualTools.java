package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import java.awt.event.ActionEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class ClearVisualTools extends GenericAction{
	private static GenericAction[] actions = {
		ToggleHairlineAction.getAction(),
		ToggleHairlineLabelAction.getAction(),
		DrawCollapseControlAction.getAction(),
		ShowIGBTrackMarkAction.getAction(),
		ShowLockedTrackIconAction.getAction()
	};
	
	private static ClearVisualTools ACTION = new ClearVisualTools();
	
	public static ClearVisualTools getAction(){
		return ACTION;
	}

	private ClearVisualTools(){
		super(BUNDLE.getString("clearVisualTools"), null, null);
	}
	
	@Override
	public void actionPerformed(ActionEvent evt){
		super.actionPerformed(evt);
		for(GenericAction action : actions){
			action.putValue(GenericAction.SELECTED_KEY, Boolean.FALSE);
			action.actionPerformed(evt);
		}
	}
	
}
