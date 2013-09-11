package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import java.awt.event.ActionEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class ClearVisualTools extends GenericAction{
	private static final long serialVersionUID = 1l;
	private static GenericAction[] actions = {
		ToggleHairlineAction.getAction(),
		ToggleHairlineLabelAction.getAction(),
		DrawCollapseControlAction.getAction(),
		ShowIGBTrackMarkAction.getAction(),
		ShowFilterMarkAction.getAction(),
		ShowLockedTrackIconAction.getAction(),
		ToggleEdgeMatchingAction.getAction()
	};
	
	private static ClearVisualTools ACTION = new ClearVisualTools();
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	public static ClearVisualTools getAction(){
		return ACTION;
	}

	private ClearVisualTools(){
		super(BUNDLE.getString("clearVisualTools"), "16x16/actions/hide_visual_tools.png", "22x22/actions/hide_visual_tools.png");
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
