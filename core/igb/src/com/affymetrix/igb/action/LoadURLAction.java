package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URL;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author jnicol
 */
public final class LoadURLAction extends AbstractLoadFileOrURLAction {
	private static final long serialVersionUID = 1l;
	private static final LoadURLAction ACTION = new LoadURLAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static LoadURLAction getAction() {
		return ACTION;
	}

	private final JFrame gviewerFrame;
	private JDialog dialog = null;

	private LoadURLAction() {
		super(BUNDLE.getString("openURL"), null, "16x16/places/network-server.png", "22x22/places/network-server.png", KeyEvent.VK_UNDEFINED, null, true);
		this.gviewerFrame = ((IGB)IGB.getSingleton()).getFrame();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		loadURL();
	}

	private void loadURL() {
		JOptionPane pane = new JOptionPane("Enter URL", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION );
		pane.setWantsInput(true);
		chooser = getFileChooser(getID());
		Box mergeOptionBox = chooser.box;
		chooser.refreshSpeciesList();
		dialog = pane.createDialog(gviewerFrame, BUNDLE.getString("openURL"));
		dialog.setModal(true);
		dialog.getContentPane().add(mergeOptionBox, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocationRelativeTo(gviewerFrame);
		dialog.setVisible(true);

		String urlStr = (String)pane.getInputValue();
		if(urlStr == null || JOptionPane.UNINITIALIZED_VALUE.equals(urlStr)){
			return;
		}
		urlStr = urlStr.trim();
		URL url;
		URI uri;
		try {
			url = new URL(urlStr);
			uri = url.toURI();
		} catch (Exception ex) {
			// verify these are valid
			ErrorHandler.errorPanel("Invalid URL", "The URL " + urlStr + " is not valid.  Please enter a valid URL");
			return;
		}
		
		String friendlyName = getFriendlyName(urlStr);
		
		if (!checkFriendlyName(friendlyName)) {
			ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + url);
			return;
		}
		
		final AnnotatedSeqGroup loadGroup = gmodel.addSeqGroup((String)chooser.versionCB.getSelectedItem());

		final boolean mergeSelected = loadGroup == gmodel.getSelectedSeqGroup();
	
		openURI(uri, friendlyName, mergeSelected, loadGroup, (String)chooser.speciesCB.getSelectedItem());
		
	}

	private static String getFriendlyName(String urlStr) {
		// strip off final "/" character, if it exists.
		if (urlStr.endsWith("/")) {
			urlStr = urlStr.substring(0,urlStr.length()-1);
		}

		//strip off all earlier slashes.
		urlStr = urlStr.substring(urlStr.lastIndexOf('/')+1);

		return urlStr;
	}

	@Override
	protected String getID() {
		return "loadURL";
	}
}
