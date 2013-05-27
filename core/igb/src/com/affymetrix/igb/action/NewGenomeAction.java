package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.general.GenericVersion;

import com.affymetrix.igb.view.NewGenome;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class NewGenomeAction extends SeqMapViewActionA {
	
	private static final long serialVersionUID = 1l;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	
	private static final NewGenomeAction ACTION = new NewGenomeAction();
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static NewGenomeAction getAction() {
		return ACTION;
	}

	private NewGenomeAction() {
		super(BUNDLE.getString("addNewSpecies"), "16x16/actions/blank_placeholder.png", null);
		this.ordinal = 200;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		NewGenome ng = new NewGenome();
		int reply = JOptionPane.showConfirmDialog(getSeqMapView(), ng, getText(), JOptionPane.OK_CANCEL_OPTION);
		if(reply == JOptionPane.OK_OPTION && ng.getVersionName().length() > 0 && ng.getSpeciesName().length() > 0){
			AnnotatedSeqGroup group = gmodel.addSeqGroup(ng.getVersionName());
			GenericVersion version  = GeneralLoadUtils.getLocalFilesVersion(group, ng.getSpeciesName());
			
			if(ng.shouldSwitch()){
				gmodel.setSelectedSeqGroup(group);
			}
		}
	}
}
