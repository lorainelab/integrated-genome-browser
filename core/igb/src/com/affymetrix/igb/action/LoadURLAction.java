package com.affymetrix.igb.action;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.UniFileFilter;

import com.affymetrix.igb.shared.OpenURIAction;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
/**
 *
 * @author jnicol
 */
public final class LoadURLAction extends OpenURIAction {
	private static final long serialVersionUID = 1l;
	private static final LoadURLAction ACTION = new LoadURLAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static LoadURLAction getAction() {
		return ACTION;
	}

	private LoadURLAction() {
		super(BUNDLE.getString("openURL"), null,
				"16x16/status/network-receive.png",
				"22x22/status/network-receive.png",
				KeyEvent.VK_UNDEFINED, null, true);
		this.ordinal = -9009100;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		loadURL();
	}
	
	private void loadURL() {
		JOptionPane pane = new JOptionPane("Enter URL", JOptionPane.QUESTION_MESSAGE, 
				JOptionPane.OK_CANCEL_OPTION);
		final JTextField urlTextField = new JTextField();
		
		List<UniFileFilter> filters = getSupportedFiles(FileTypeCategory.Sequence);
		Set<String> all_known_endings = new HashSet<String>();
		for (UniFileFilter filter : filters) {
			all_known_endings.addAll(filter.getExtensions());
		}
		final UniFileFilter seq_ref_filter = new UniFileFilter(all_known_endings.toArray(new String[all_known_endings.size()]), "Known Types");
		final UniFileFilter all_known_types = getAllKnowFilter();
		
		chooser = getFileChooser(getID());
		chooser.optionChooser.refreshSpeciesList();
		String clipBoardContent = GeneralUtils.getClipboard();
		if(LocalUrlCacher.isURL(clipBoardContent)){
			urlTextField.setText(clipBoardContent);
			checkLoadSeqCB(false, clipBoardContent, seq_ref_filter);
		}
		
        urlTextField.getDocument().addDocumentListener(new DocumentListener(){
            public void changedUpdate(DocumentEvent de) {
				checkLoadSeqCB(true, urlTextField.getText(), seq_ref_filter);
            }

            @Override
            public void insertUpdate(DocumentEvent de) {
				checkLoadSeqCB(true, urlTextField.getText(), seq_ref_filter);
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
				checkLoadSeqCB(true, urlTextField.getText(), seq_ref_filter);
            }
        });
		
		pane.setMessage(new Object[]{"Enter URL", urlTextField});
		
		JDialog dialog = pane.createDialog(igbService.getFrame(), BUNDLE.getString("openURL"));
		dialog.setModal(true);
		dialog.getContentPane().add(chooser.optionChooser, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocationRelativeTo(igbService.getFrame());
		dialog.setVisible(true);
		
		String urlStr = urlTextField.getText();
		
		int result = JOptionPane.CANCEL_OPTION;
		if (pane.getValue() != null && pane.getValue() instanceof Integer) {
			result = (Integer) pane.getValue();
		}
		
		if(urlStr == null && urlStr.length() > 0 || result!=JOptionPane.OK_OPTION){
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
			ErrorHandler.errorPanel("Invalid URL", "The URL " + urlStr + " is not valid.  Please enter a valid URL", Level.SEVERE);
			return;
		}
		
		String friendlyName = getFriendlyName(urlStr);
		
		if (!checkFriendlyName(friendlyName, all_known_types)) {
			ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + url, Level.WARNING);
			return;
		}
		
		final AnnotatedSeqGroup loadGroup = gmodel.addSeqGroup((String)chooser.getSelectedVersion());

		final boolean mergeSelected = loadGroup == gmodel.getSelectedSeqGroup();
	
		openURI(uri, friendlyName, mergeSelected, loadGroup, (String)chooser.getSelectedSpecies(), !chooser.optionChooser.getLoadAsSeqCB().isSelected());
		
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
	
	private void checkLoadSeqCB(boolean checkURL, String fileName, UniFileFilter filter) {
		if(checkURL && LocalUrlCacher.isURL(fileName)) {
			checkLoadSeqCB(fileName, filter);
		} else {
			checkLoadSeqCB(fileName, filter);
		}
	}
	
	private void checkLoadSeqCB(String fileName, UniFileFilter filter) {
		boolean enableLoadAsSeqCB = filter.accept(new File(fileName));
		chooser.optionChooser.getLoadAsSeqCB().setEnabled(enableLoadAsSeqCB);
		if(!enableLoadAsSeqCB) {
			chooser.optionChooser.getLoadAsSeqCB().setSelected(false);
		} // Uncheck for disabled
	}
}
