/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.ParameteredAction;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.factories.TransformTierGlyph;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * This action is used to change the fixed height of a Track which is locked.
 * 
 * @author Anuj
 */
public class ChangeTierHeightAction extends SeqMapViewActionA implements ParameteredAction{
	private static final long serialVersionUID = 1l;
	private static final ChangeTierHeightAction ACTION = new ChangeTierHeightAction();
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ChangeTierHeightAction getAction() {
		return ACTION;
	}

	private ChangeTierHeightAction() {
		super(BUNDLE.getString("changeTierHeightAction"), "16x16/actions/resize_track.png", "22x22/actions/resize_track.png");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		List <TierLabelGlyph> labels = getTierManager().getSelectedTierLabels();
		String height = "";
		if(labels.isEmpty() || labels == null){
			ErrorHandler.errorPanel("changeTierHeight called with an empty list");
			return;
		}
		if(labels.size() == 1){
			int h;
			TierGlyph currentTier = labels.get(0).getReferenceTier();
			if(currentTier instanceof TransformTierGlyph){	
				h = ((TransformTierGlyph)currentTier).getFixedPixHeight();
				height += h;
			}
		}
		JPanel panel = new JPanel();
		JLabel tierHeightLabel = new JLabel("Track Height: ");
		JTextField tierHeightField = new JTextField(height, 20);
		panel.add(tierHeightLabel);
		panel.add(tierHeightField);
		int isOK = JOptionPane.showConfirmDialog(null, panel, "Change Track Height", JOptionPane.OK_CANCEL_OPTION);
		if(isOK == JOptionPane.OK_OPTION){
			try{
				changeHeight(Integer.parseInt(tierHeightField.getText()));
			}catch(Exception ex){
				ErrorHandler.errorPanel("Invalid Track Height");
			}
		}
	}

	private void changeHeight(int updatedHeight) throws NumberFormatException {
		for(TierLabelGlyph tlg : getTierManager().getSelectedTierLabels()){
			TierGlyph currentTier = tlg.getReferenceTier();
			if(currentTier instanceof TransformTierGlyph){
				((TransformTierGlyph)currentTier).setFixedPixHeight(updatedHeight);
				tlg.getPixelBox().height=updatedHeight;
			}
		}
		AffyLabelledTierMap lm = (AffyLabelledTierMap)(this.getSeqMapView().getSeqMap());
		lm.repackTheTiers(true, true);
	}

	public void performAction(Object... parameters) {
		if(parameters.length < 1 || parameters[0].getClass() != Integer.class)
			return;
		int height = Integer.valueOf(parameters[0].toString());
		changeHeight(height);
	}

	@Override
	public boolean isEnabled() {
		return Selections.isAnyLocked();
	}
}
