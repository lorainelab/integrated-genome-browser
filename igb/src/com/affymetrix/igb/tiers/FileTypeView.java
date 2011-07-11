package com.affymetrix.igb.tiers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import com.jidesoft.combobox.ColorComboBox;
import com.jidesoft.grid.ColorCellEditor;

import com.affymetrix.genoviz.swing.BooleanTableCellRenderer;
import com.affymetrix.genoviz.swing.ColorTableCellRenderer;
import com.affymetrix.genoviz.util.ErrorHandler;

import com.affymetrix.igb.prefs.IPrefEditorComponent;
import com.affymetrix.igb.stylesheet.AssociationElement;
import com.affymetrix.igb.stylesheet.FileTypePrefTableModel;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;

/**
 *
 * @author hiralv
 */
public class FileTypeView extends IPrefEditorComponent {

	private final JTable table = new JTable();
	private final FileTypePrefTableModel model;

	public FileTypeView() {
		super();

		setName("File Types");
		setToolTipText("Set Track Colors and Properties");
		setLayout(new BorderLayout());

		JScrollPane table_scroll_pane = new JScrollPane(table);

		JButton addButton = new JButton("Add");
		addButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				String inputValue = JOptionPane.showInputDialog("Enter file type");
				if (inputValue == null) {
					return;
				}

				if (XmlStylesheetParser.getUserFileTypeAssociation().get(inputValue) != null) {
					ErrorHandler.errorPanel("Duplicate Entry", "File type " + inputValue + " exists");
					return;
				}
				XmlStylesheetParser.getUserFileTypeAssociation().put(inputValue, AssociationElement.getFileTypeAssocation(inputValue));
				model.setElements(XmlStylesheetParser.getUserFileTypeAssociation());
			}
		});

		add(addButton, BorderLayout.SOUTH);
		add(table_scroll_pane, BorderLayout.CENTER);

		model = new FileTypePrefTableModel();
		model.setElements(XmlStylesheetParser.getUserFileTypeAssociation());

		table.setModel(model);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);
		table.setRowSelectionAllowed(true);
		table.setEnabled(true); // doesn't do anything ?

		ColorCellEditor cellEditor = new ColorCellEditor() {

			private static final long serialVersionUID = 1L;

			@Override
			protected ColorComboBox createColorComboBox() {
				final ColorComboBox combobox = new ColorComboBox();
				combobox.setColorValueVisible(false);
				combobox.setCrossBackGroundStyle(false);
				combobox.setButtonVisible(false);
				combobox.setStretchToFit(true);
				return combobox;
			}
		};
		table.setDefaultRenderer(Color.class, new ColorTableCellRenderer());
		table.setDefaultEditor(Color.class, cellEditor);
		table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
		table.setDefaultEditor(Float.class, new DefaultCellEditor(new JComboBox(TrackConstants.SUPPORTED_SIZE)));
		table.setDefaultEditor(TrackConstants.DIRECTION_TYPE.class, new DefaultCellEditor(new JComboBox(TrackConstants.DIRECTION_TYPE.values())));

		validate();
	}

	@Override
	public void refresh() {
	}
}
