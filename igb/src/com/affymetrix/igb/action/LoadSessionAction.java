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
import java.util.prefs.InvalidPreferencesFormatException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class LoadSessionAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public LoadSessionAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("loadSession")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Import16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_L);
	}

	public void actionPerformed(ActionEvent e) {
		  JFileChooser chooser = PreferenceUtils.getJFileChooser();
		  int option = chooser.showOpenDialog(Application.getSingleton().getFrame().getContentPane());
		  if (option == JFileChooser.APPROVE_OPTION) {
			  File f = chooser.getSelectedFile();
			  try {
				  PreferenceUtils.importPreferences(f);
				  loadWindowPreferences();
				  loadSessionPreferences();
				  clearSessionPreferences();
			  }
			  catch (InvalidPreferencesFormatException ipfe) {
				  ErrorHandler.errorPanel("ERROR", "Invalid preferences format:\n" + ipfe.getMessage()
						  + "\n\nYou can only load a session from a file that was created with save session.");
			  }
			  catch (Exception x) {
				  ErrorHandler.errorPanel("ERROR", "Error loading session from file", x);
			  }
		  }
	}

	private void loadWindowPreferences() {
		((IGB)IGB.getSingleton()).getWindowService().restoreState();
	}

	private void loadSessionPreferences() {
		SeqMapView mapView = Application.getSingleton().getMapView();
		mapView.loadSession();
		for (IGBTabPanel panel : ((IGB)Application.getSingleton()).getTabs()) {
			panel.loadSession();
		}
	}

	private void clearSessionPreferences() {
		try {
			PreferenceUtils.getSessionPrefsNode().removeNode();
		}
		catch (BackingStoreException x) {}
	}
}
