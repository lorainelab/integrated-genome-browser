package com.affymetrix.igb.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
public final class MergeOptionChooser extends JFileChooser {

	ButtonGroup bgroup = new ButtonGroup();
	public JRadioButton merge_button = new JRadioButton(BUNDLE.getString("mergeWithCurrentlyLoadedData"), true);
	public JRadioButton no_merge_button = new JRadioButton(BUNDLE.getString("createNewGenome"), false);
	public JTextField genome_name_TF = new JTextField(BUNDLE.getString("unknownGenome"));
	Box box = null;

	public MergeOptionChooser() {
		super();
		bgroup.add(no_merge_button);
		bgroup.add(merge_button);
		merge_button.setSelected(true);

		genome_name_TF.setEnabled(no_merge_button.isSelected());

		no_merge_button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				genome_name_TF.setEnabled(no_merge_button.isSelected());
			}
		});

		box = new Box(BoxLayout.X_AXIS);
		box.setBorder(BorderFactory.createEmptyBorder(5, 5, 8, 5));
		box.add(Box.createHorizontalStrut(5));
		box.add(merge_button);
		box.add(no_merge_button);
		box.add(Box.createRigidArea(new Dimension(5, 0)));
		box.add(genome_name_TF);

		merge_button.setMnemonic('M');
		no_merge_button.setMnemonic('C');
	}

	@Override
	protected JDialog createDialog(Component parent) throws HeadlessException {
		JDialog dialog = super.createDialog(parent);

		dialog.getContentPane().add(box, BorderLayout.SOUTH);
		return dialog;
	}
}
