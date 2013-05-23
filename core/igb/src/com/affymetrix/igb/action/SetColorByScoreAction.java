package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JOptionPane;

import com.affymetrix.genometryImpl.color.ColorProviderI;
import com.affymetrix.genometryImpl.color.Score;
import com.affymetrix.genometryImpl.util.ErrorHandler;

import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.ColorByScoreEditor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackStyle;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class SetColorByScoreAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final SetColorByScoreAction ACTION = new SetColorByScoreAction();

	static{
//		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static SetColorByScoreAction getAction() {
		return ACTION;
	}

	private SetColorByScoreAction() {
		super(BUNDLE.getString("setColorByScore"), "16x16/actions/blank_placeholder.png", null);
	}

	public void updateColorByScore(List<TierLabelGlyph> theTiers){
		if(theTiers == null || theTiers.isEmpty()){
			ErrorHandler.errorPanel("updateColorByScore called with an empty list");
			return;
		}
		ColorByScoreEditor editor;
		float min,max;
		TrackStyle style;
		String minText = "";
		String maxText = "";
		String intervalsText = "";
		if(theTiers.size() == 1){
			TierLabelGlyph tlg = theTiers.get(0);
			TierGlyph tg = (TierGlyph) tlg.getInfo();
			style = (TrackStyle)tg.getAnnotStyle();
			ColorProviderI cp = style.getColorProvider();
//			if(cp instanceof Score){
//				min = ((Score)cp).getMinScoreColor();
//				max = ((Score)cp).getMaxScoreColor();
//			}else{
				min = Score.DEFAULT_MIN_SCORE;
				max = Score.DEFAULT_MAX_SCORE;
//			}
			minText+= min;
			maxText+=max;
		}
		editor = new ColorByScoreEditor(minText, maxText, intervalsText);
		int isOK = JOptionPane.showConfirmDialog(null, editor, "Set Color By Score", JOptionPane.OK_CANCEL_OPTION);
		
		switch(isOK){
			case JOptionPane.OK_OPTION : 
				float updatedMinRange = editor.getMinRange();
				float updatedMaxRange = editor.getMaxRange();
				//int  updatedIntervals = editor.getColorIntervals(); 
				for(TierLabelGlyph label : theTiers){
					TierGlyph tg = (TierGlyph)label.getInfo();
					style = (TrackStyle)tg.getAnnotStyle();
					ColorProviderI cp = style.getColorProvider();
					if(cp instanceof Score){
//						((Score)cp).setMinScoreColor(updatedMinRange);
//						((Score)cp).setMaxScoreColor(updatedMaxRange);
					}
				//	style.setColorIntervals(updatedIntervals);
				}
		}
		refreshMap(false, false);
	}

	@Override
	public void actionPerformed(ActionEvent e){
		super.actionPerformed(e);
		updateColorByScore(getTierManager().getSelectedTierLabels());
	}
}
