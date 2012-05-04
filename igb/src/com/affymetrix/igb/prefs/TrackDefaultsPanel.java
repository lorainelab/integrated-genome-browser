/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.prefs;

/**
 *
 * @author auser
 */
public class TrackDefaultsPanel extends TrackPreferencesPanel{
	private static final long serialVersionUID = 1L;
	public TrackDefaultsPanel(){
		super("Track Defaults",TrackDefaultView.getSingleton());	
	}
	@Override
	protected void enableSpecificComponents()
	{
		viewModePanel.setVisible(false);
		viewModeComboBox.setVisible(false);
		autoRefreshCheckBox.setVisible(false);
		refreshButton.setVisible(false);
		applyButton.setVisible(false);
	}
	@Override
	protected void deleteAndRestoreButtonActionPerformed(java.awt.event.ActionEvent evt) {
		((TrackDefaultView)tdv).deleteTrackDefaultButton();
	}
	@Override
	protected void selectAndAddButtonActionPerformed(java.awt.event.ActionEvent evt) {  
		((TrackDefaultView)tdv).addTrackDefaultButton();
	}
}