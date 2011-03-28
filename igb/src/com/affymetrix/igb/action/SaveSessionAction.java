package com.affymetrix.igb.action;

import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.prefs.BackingStoreException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class SaveSessionAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public SaveSessionAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("saveSession")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Export16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_S);
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = PreferenceUtils.getJFileChooser();
		int option = chooser.showSaveDialog(Application.getSingleton().getFrame().getContentPane());
		if (option == JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			try {
				saveWindowPreferences();
				saveSessionPreferences();
				PreferenceUtils.exportPreferences(PreferenceUtils.getTopNode(), f);
				clearSessionPreferences();
			}
			catch (Exception x) {
				ErrorHandler.errorPanel("ERROR", "Error saving session to file", x);
			}
		}
	}

	private void saveWindowPreferences() {
		((IGB)IGB.getSingleton()).getWindowService().saveState();
	}

	private void saveSessionPreferences() {
		SeqMapView mapView = Application.getSingleton().getMapView();
		mapView.saveSession();
		for (IGBTabPanel panel : ((IGB)Application.getSingleton()).getTabs()) {
			panel.saveSession();
		}
	}

	private void clearSessionPreferences() {
		try {
			PreferenceUtils.getSessionPrefsNode().removeNode();
		}
		catch (BackingStoreException x) {}
	}
}
