package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import java.awt.event.ActionEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class ShowAllVisualToolsAction extends GenericAction{
	private static GenericAction[] actions = {
		ToggleHairlineAction.getAction(),
		ToggleHairlineLabelAction.getAction(),
		DrawCollapseControlAction.getAction(),
		ShowIGBTrackMarkAction.getAction(),
		ShowLockedTrackIconAction.getAction(),
		ToggleEdgeMatchingAction.getAction()
	};
	
	private static ShowAllVisualToolsAction ACTION = new ShowAllVisualToolsAction();
	
	public static ShowAllVisualToolsAction getAction(){
		return ACTION;
	}

	private ShowAllVisualToolsAction(){
		super(BUNDLE.getString("showAllVisualTools"), "16x16/actions/show_visual_tools.png", "22x22/actions/show_visual_tools.png");
	}
	
	@Override
	public void actionPerformed(ActionEvent evt){
		super.actionPerformed(evt);
		for(GenericAction action : actions){
			action.putValue(GenericAction.SELECTED_KEY, Boolean.TRUE);
			action.actionPerformed(evt);
		}
	}
	
}