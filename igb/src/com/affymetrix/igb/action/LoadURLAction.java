package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.util.MergeOptionChooser;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author jnicol
 */
public final class LoadURLAction extends AbstractAction {
	private static final long serialVersionUID = 1l;
	private static final MergeOptionChooser chooser = new MergeOptionChooser();
	private final JFrame gviewerFrame;
	private final Box mergeOptionBox = chooser.box;
	private JDialog dialog = null;

	public LoadURLAction(JFrame gviewerFrame) {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("openURL")),
					MenuUtil.getIcon("toolbarButtonGraphics/general/Open16.gif"));
		this.gviewerFrame = gviewerFrame;
	}

	public void actionPerformed(ActionEvent e) {
		loadURL();
	}

	private void loadURL() {
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		if (gmodel.getSelectedSeqGroup() == null) {
			chooser.no_merge_button.setEnabled(true);
			chooser.no_merge_button.setSelected(true);
			chooser.merge_button.setEnabled(false);
		} else {
			// default to "merge" if already have a selected seq group to merge with,
			//    because non-merging is an uncommon choice
			chooser.merge_button.setSelected(true);
			chooser.merge_button.setEnabled(true);
		}
		chooser.genome_name_TF.setEnabled(chooser.no_merge_button.isSelected());
		chooser.genome_name_TF.setText(LoadFileAction.UNKNOWN_GROUP_PREFIX + " " + LoadFileAction.unknown_group_count);

		JOptionPane pane = new JOptionPane("Enter URL", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION );
		pane.setWantsInput(true);

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
			
		final boolean mergeSelected = chooser.merge_button.isSelected();
		if (!mergeSelected) {
			// Not merging, so create a new Seq Group
			LoadFileAction.unknown_group_count++;
		}

		final AnnotatedSeqGroup loadGroup = mergeSelected ? gmodel.getSelectedSeqGroup() : gmodel.addSeqGroup(chooser.genome_name_TF.getText());

		if (!mergeSelected) {
			// Select the "unknown" group.
			gmodel.setSelectedSeqGroup(loadGroup);
		}

		LoadFileAction.openURI(uri, getFriendlyName(urlStr), mergeSelected, loadGroup);
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
}
