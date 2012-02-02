package com.affymetrix.igb.util;

import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.recordplayback.JRPComboBox;
import com.affymetrix.genoviz.swing.recordplayback.JRPFileChooser;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.*;

import javax.swing.*;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/** A JFileChooser that has a checkbox for whether you want to merge annotations.
 *  Note that an alternative way of adding a checkbox to a JFileChooser
 *  is to use JFileChooser.setAccessory().  The only advantage to this
 *  subclass is more control of where the JCheckBox is placed inside the
 *  dialog.
 */
public final class MergeOptionChooser extends JRPFileChooser {
	private static final long serialVersionUID = 1L;

	private static final String SELECT_SPECIES = BUNDLE.getString("speciesCap");
	public final Box box;
	public final JRPComboBox speciesCB;
	public final JRPComboBox versionCB;
	private final GenericAction speciesAction = new GenericAction() {
		private static final long serialVersionUID = 1L;
		@Override
		public String getText() {
			return null;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getModifiers() != 0) {
				super.actionPerformed(e);
			}
			populateVersionCB();
			if(speciesCB.getSelectedIndex() == 0)
				speciesCB.setEditable(true);
			else
				speciesCB.setEditable(false);
			versionCB.setSelectedIndex(0);
		}
		@Override
		public String getId() {
			return MergeOptionChooser.this.getId() + "_speciesAction";
		}
	};
	private final GenericAction versionAction = new GenericAction() {
		private static final long serialVersionUID = 1L;
		@Override
		public String getText() {
			return null;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getModifiers() != 0) {
				super.actionPerformed(e);
			}
			if (versionCB.getSelectedIndex() == 0) {
				versionCB.setEditable(true);
			} else {
				versionCB.setEditable(false);
			}
		}
		@Override
		public String getId() {
			return MergeOptionChooser.this.getId() + "_versionAction";
		}
	};

	public MergeOptionChooser(String id) {
		super(id);
		
		setAcceptAllFileFilterUsed(false);
		speciesCB = new JRPComboBox(id + "_speciesCB");
		versionCB = new JRPComboBox(id + "_versionCB");
		speciesCB.addActionListener(speciesAction);
		versionCB.addActionListener(versionAction);
		
		box = new Box(BoxLayout.X_AXIS);
		box.setBorder(BorderFactory.createEmptyBorder(5, 5, 8, 5));

		box.add(Box.createHorizontalStrut(5));
		box.add(IGBUtils.setInfoLabel(speciesCB, "Choose or enter species"));
	
		box.add(Box.createHorizontalStrut(5));
		box.add(IGBUtils.setInfoLabel(versionCB, "Choose or enter version"));
		
	}

	@Override
	protected JDialog createDialog(Component parent) throws HeadlessException {
		JDialog dialog = super.createDialog(parent);

		refreshSpeciesList();
		dialog.getContentPane().add(box, BorderLayout.SOUTH);
		return dialog;
	}

	public void refreshSpeciesList(){
		speciesCB.removeAllItems();
		speciesCB.addItem(OpenURIAction.UNKNOWN_SPECIES_PREFIX + " " + OpenURIAction.unknown_group_count);
		for(String species : GeneralLoadUtils.getSpeciesList()){
			speciesCB.addItem(species);
		}

		String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();

		if(!SELECT_SPECIES.equals(speciesName))
			speciesCB.setSelectedItem(speciesName);
		else
			speciesCB.setSelectedIndex(0);

		AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		if (group != null) {
			versionCB.setSelectedItem(group.getID());
		} else {
			versionCB.setSelectedIndex(0);
		}
	}

	private void populateVersionCB(){
		String speciesName = (String) speciesCB.getSelectedItem();
		versionCB.removeAllItems();
		versionCB.addItem(OpenURIAction.UNKNOWN_GENOME_PREFIX + " " + OpenURIAction.unknown_group_count);
		for(String version : GeneralLoadUtils.getGenericVersions(speciesName)){
			versionCB.addItem(version);
		}
	}

}
