/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.prefs;

/**
 * This class is a subclass of TrackPreferences Panel and is used for Tracks->Defaults Tab which makes some of the components 
 * invisible on Panel which are inappropriate
 * 
 * @author Anuj
 */
public class TrackDefaultsPanel extends TrackPreferencesPanel{
	private static final long serialVersionUID = 1L;
	public TrackDefaultsPanel(){
		super("Track Defaults",TrackDefaultView.getSingleton());	
	}
	@Override
	protected void enableSpecificComponents()
	{
		autoRefreshCheckBox.setVisible(false);
		refreshButton.setVisible(false);
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