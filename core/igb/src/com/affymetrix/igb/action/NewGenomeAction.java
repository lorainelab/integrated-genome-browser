package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;

import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.view.NewGenome;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.general.ServerList;

/**
 *
 * @author hiralv
 */
public class NewGenomeAction extends OpenURIAction {
	
	private static final long serialVersionUID = 1l;
	
	private static final NewGenomeAction ACTION = new NewGenomeAction();
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static NewGenomeAction getAction() {
		return ACTION;
	}

	private NewGenomeAction() {
		super(BUNDLE.getString("addNewSpecies"), null, 
				"16x16/actions/blank_placeholder.png", null,
				KeyEvent.VK_UNDEFINED, null, false);
		this.ordinal = 200;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		NewGenome ng = new NewGenome();
		int reply = JOptionPane.showConfirmDialog(getSeqMapView(), ng, getText(), JOptionPane.OK_CANCEL_OPTION);
		if(reply == JOptionPane.OK_OPTION && ng.getVersionName().length() > 0 && ng.getSpeciesName().length() > 0){
			AnnotatedSeqGroup group = gmodel.addSeqGroup(ng.getVersionName());
			String refSeqPath = ng.getRefSeqFile();
			
			if(refSeqPath != null && refSeqPath.length() > 0){
				boolean mergeSelected = gmodel.getSeqGroup(ng.getVersionName()) == null;
				String fileName = getFriendlyName(refSeqPath);
				if(Constants.genomeTxt.equals(fileName) || Constants.modChromInfoTxt.equals(fileName)){
					try {
						ChromInfoParser.parse(group, getInputStream(refSeqPath));
						GenericVersion version = GeneralLoadUtils.getLocalFilesVersion(group, ng.getSpeciesName());
						ServerList.getServerInstance().fireServerInitEvent(version.gServer, ServerStatus.Initialized, false);
					} catch (Exception ex) {
						Logger.getLogger(NewGenomeAction.class.getName()).log(Level.SEVERE, null, ex);
					}
				} else {
					openURI(new File(refSeqPath).toURI(), fileName, mergeSelected, group, ng.getSpeciesName(), false);
				}
			} else {
				GenericVersion version = GeneralLoadUtils.getLocalFilesVersion(group, ng.getSpeciesName());
				ServerList.getServerInstance().fireServerInitEvent(version.gServer, ServerStatus.Initialized, false);
			}
		
			if(ng.shouldSwitch()){
				gmodel.setSelectedSeqGroup(group);
			}
		}
	}
	
	private InputStream getInputStream(String fileName) throws Exception {
		return LocalUrlCacher.getInputStream(relativeToAbsolute(fileName).toURL());
	}

	/* This method is used to convert the given file path from relative to absolute.
	 */
	private URI relativeToAbsolute(String path) throws URISyntaxException {
		if (!(path.startsWith("file:") && !(path.startsWith("http:")) && !(path.startsWith("https:")) && !(path.startsWith("ftp:")))) {
			return getAbsoluteFile(path).toURI();
		}
		return new URI(path);
	}

	/*Returns the File object at given path
	 */
	private File getAbsoluteFile(String path) {
		return new File(path).getAbsoluteFile();
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
