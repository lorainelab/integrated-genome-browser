package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.ColorByScoreEditor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackStyle;

public class SetColorByScoreAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final SetColorByScoreAction ACTION = new SetColorByScoreAction();

	public static SetColorByScoreAction getAction() {
		return ACTION;
	}

	private SetColorByScoreAction() {
		super("Set Range of Color By Score", null);
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
		if(theTiers.size() == 1){
			TierLabelGlyph tlg = theTiers.get(0);
			TierGlyph tg = (TierGlyph) tlg.getInfo();
			style = (TrackStyle)tg.getAnnotStyle();
			min = style.getMinScoreColor();
			max = style.getMaxScoreColor();
			minText+= min;
			maxText+=max;
		}
		editor = new ColorByScoreEditor(minText, maxText);
		int isOK = JOptionPane.showConfirmDialog(null, editor, "Set Color By Score", JOptionPane.OK_CANCEL_OPTION);
		
		switch(isOK){
			case JOptionPane.OK_OPTION : 
				float updatedMinRange = editor.getMinRange();
				float updatedMaxRange = editor.getMaxRange();
				for(TierLabelGlyph label : theTiers){
					TierGlyph tg = (TierGlyph)label.getInfo();
					style = (TrackStyle)tg.getAnnotStyle();
					style.setMinScoreColor(updatedMinRange);
					style.setMaxScoreColor(updatedMaxRange);
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
