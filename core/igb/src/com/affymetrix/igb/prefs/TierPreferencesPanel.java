package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.event.GroupSelectionEvent;

/**
 *
 * @author Anuj
 */
public class TierPreferencesPanel extends TrackPreferencesPanel {
	private static final long serialVersionUID = 1L;
	/**
	 * Creates new form TierPreferencesPanel
	 */
	public TierPreferencesPanel() {
		super( "Tracks",TierPrefsView.getSingleton());
		this.setToolTipText("Set Track Properties");
		validate();
	}
	@Override
	protected void enableSpecificComponents()
	{
		viewModePanel.setVisible(true);
		viewModeComboBox.setVisible(true);
		autoRefreshCheckBox.setVisible(true);
		refreshButton.setVisible(true);
		applyButton.setVisible(true);
	}
	@Override
	public void refresh()
	{
		((TierPrefsView)tdv).refreshList();
	}
	
	@Override
	public void mapRefresh(){
		if (isVisible()) {
				((TierPrefsView)tdv).refreshList();
			}
	}
	
	@Override
	public void groupSelectionChanged(GroupSelectionEvent evt) {
		mapRefresh();
	}
	
	//<editor-fold defaultstate="collapsed" desc="comment">
	@Override
	protected void deleteAndRestoreButtonActionPerformed(java.awt.event.ActionEvent evt) {
		//</editor-fold>
		((TierPrefsView)tdv).restoreToDefault();
	}
	@Override
	protected void selectAndAddButtonActionPerformed(java.awt.event.ActionEvent evt) {  
		((TierPrefsView)tdv).selectAll();
	}
	@Override
	protected void viewModeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {                                                 
		((TierPrefsView)tdv).viewModeCB();
	}
}
