package com.affymetrix.igb.util;

import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.InfoLabel;
import com.affymetrix.genoviz.swing.recordplayback.JRPComboBox;
import com.affymetrix.genoviz.swing.recordplayback.JRPFileChooser;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.*;

import javax.swing.*;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.*;

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
	private class SpeciesAction extends GenericAction {
		public SpeciesAction() {
			super(null, null, null);
		}
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getModifiers() != 0) {
				super.actionPerformed(e);
			}
			populateVersionCB();
			if(speciesCB.getSelectedIndex() == 0) {
				speciesCB.setEditable(true);
			}
			else {
				speciesCB.setEditable(false);
			}
			versionCB.setSelectedIndex(0);
		}
	};
	private final SpeciesAction speciesAction = new SpeciesAction();
	private class VersionAction extends GenericAction {
		public VersionAction() {
			super(null, null, null);
		}
		private static final long serialVersionUID = 1L;
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
	};
	private final VersionAction versionAction = new VersionAction();

	public MergeOptionChooser(String id) {
		super(id);
		
		setAcceptAllFileFilterUsed(false);
		speciesCB = new JRPComboBox(id + "_speciesCB");
		versionCB = new JRPComboBox(id + "_versionCB");
		speciesCB.addActionListener(speciesAction);
		versionCB.addActionListener(versionAction);
		
		box = new Box(BoxLayout.Y_AXIS);
		box.setBorder(BorderFactory.createEmptyBorder(5, 5, 8, 5));

		Box labelBox = new Box(FlowLayout.LEFT);
		labelBox.add(new JLabel("Choose species and genome from menus below or click on menu and type in custom values"));
		
		Box buttonBox = new Box(BoxLayout.X_AXIS);
		buttonBox.add(Box.createHorizontalStrut(5));
		buttonBox.add(setInfoLabel(speciesCB, "Choose species or click in menu and enter custom species"));
		buttonBox.add(Box.createHorizontalStrut(5));
		buttonBox.add(setInfoLabel(versionCB, "Choose genome or click in menu and enter custom genome"));
		
		box.add(labelBox);
		box.add(buttonBox);
	}

	private JPanel setInfoLabel(JComponent component, String tooltip){
		JLabel infolabel = new InfoLabel(CommonUtils.getInstance().getIcon("16x16/actions/info.png"));
		infolabel.setToolTipText(tooltip);
		
		JPanel pane = new JPanel();
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		pane.add(component, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.PAGE_START;
		c.insets = new Insets(0,0,10,0);  
		pane.add(infolabel, c);
		
		return pane;
	}

	@Override
	protected JDialog createDialog(Component parent) throws HeadlessException {
		JDialog dialog = super.createDialog(parent);

		refreshSpeciesList();
		dialog.getContentPane().add(box, BorderLayout.SOUTH);
		dialog.pack();
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
