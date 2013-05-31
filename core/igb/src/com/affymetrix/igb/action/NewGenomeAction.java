package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.JOptionPane;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.event.GenericActionHolder;

import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.view.NewGenome;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

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
				openURI(new File(refSeqPath).toURI(), fileName, mergeSelected, group, fileName, false);
			} else {
				GeneralLoadUtils.getLocalFilesVersion(group, ng.getSpeciesName());
			}
		
			if(ng.shouldSwitch()){
				gmodel.setSelectedSeqGroup(group);
			}
		}
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
