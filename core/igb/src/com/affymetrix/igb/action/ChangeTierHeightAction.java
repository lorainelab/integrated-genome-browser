/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.Application;
import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author auser
 */
public class ChangeTierHeightAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1l;
	private static final ChangeTierHeightAction ACTION = new ChangeTierHeightAction();
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ChangeTierHeightAction getAction() {
		return ACTION;
	}

	private ChangeTierHeightAction() {
		super(MessageFormat.format(BUNDLE.getString("changeTierHeightAction"), APP_NAME), null,
				null, null,
				KeyEvent.VK_A, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		List <TierLabelGlyph> labels = Application.getSingleton().getMapView().getTierManager().getSelectedTierLabels();
		String height = "";
		if(labels.isEmpty() || labels == null){
			ErrorHandler.errorPanel("changeTierHeight called with an empty list");
			return;
		}
		if(labels.size() == 1){
			height = height+labels.get(0).getReferenceTier().getAnnotStyle().getHeight();
		}
		JPanel panel = new JPanel();
		JLabel tierHeightLabel = new JLabel("Track Height: ");
		JTextField tierHeightField = new JTextField(height, 20);
		panel.add(tierHeightLabel);
		panel.add(tierHeightField);
		int isOK = JOptionPane.showConfirmDialog(null, panel, "Change Track Height", JOptionPane.OK_CANCEL_OPTION);
		if(isOK == JOptionPane.OK_OPTION){
			try{
				double updatedHeight = (double)(Float.parseFloat(tierHeightField.getText()));
				for(TierLabelGlyph tlg : labels){
					TierGlyph currentTier = tlg.getReferenceTier();
					currentTier.getAnnotStyle().setHeight(updatedHeight);
				}
			}catch(Exception ex){
				ErrorHandler.errorPanel("Invalid Track Height");
			}
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}
	}
}
