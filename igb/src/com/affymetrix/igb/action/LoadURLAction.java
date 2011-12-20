package com.affymetrix.igb.action;

import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.util.MergeOptionChooser;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
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
public final class LoadURLAction extends OpenURIAction {
	private static final long serialVersionUID = 1l;
	private static final LoadURLAction ACTION = new LoadURLAction();

	public static LoadURLAction getAction() {
		return ACTION;
	}

	private static final MergeOptionChooser chooser = new MergeOptionChooser("loadURL");
	private final JFrame gviewerFrame;
	private static final Box mergeOptionBox = chooser.box;
	private JDialog dialog = null;

	private LoadURLAction() {
		super(IGBServiceImpl.getInstance());
		this.gviewerFrame = ((IGB)IGB.getSingleton()).getFrame();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		loadURL();
	}

	private void loadURL() {
		JOptionPane pane = new JOptionPane("Enter URL", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION );
		pane.setWantsInput(true);

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
			
		final AnnotatedSeqGroup loadGroup = gmodel.addSeqGroup((String)chooser.versionCB.getSelectedItem());

		final boolean mergeSelected = loadGroup == gmodel.getSelectedSeqGroup();

		openURI(uri, getFriendlyName(urlStr), mergeSelected, loadGroup, (String)chooser.speciesCB.getSelectedItem(), false);
		
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
	public String getText() {
		return BUNDLE.getString("openURL");
	}

	@Override
	public String getIconPath() {
		return null;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
