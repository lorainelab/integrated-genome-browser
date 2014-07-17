package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.swing.JRPFileChooser;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.HeadlessException;
import javax.swing.JDialog;

/** A JFileChooser that has a checkbox for whether you want to merge annotations.
 *  Note that an alternative way of adding a checkbox to a JFileChooser
 *  is to use JFileChooser.setAccessory().  The only advantage to this
 *  subclass is more control of where the JCheckBox is placed inside the
 *  dialog.
 */
public final class MergeOptionChooser extends JRPFileChooser {
	private static final long serialVersionUID = 1L;
	public final OptionChooserImpl optionChooser;
	
	public MergeOptionChooser(String id) {
		super(id);
		setAcceptAllFileFilterUsed(false);
		optionChooser = new OptionChooserImpl();
	}


	@Override
	protected JDialog createDialog(Component parent) throws HeadlessException {
		JDialog dialog = super.createDialog(parent);

		optionChooser.refreshSpeciesList();
		dialog.getContentPane().add(optionChooser, BorderLayout.SOUTH);
		dialog.pack();
		return dialog;
	}

	public Object getSelectedSpecies(){
		return SpeciesLookup.getPreferredName(optionChooser.getSpeciesCB().getSelectedItem().toString());
	}
	
	public Object getSelectedVersion(){
		return SynonymLookup.getDefaultLookup().getPreferredName(optionChooser.getVersionCB().getSelectedItem().toString());
	}

}
