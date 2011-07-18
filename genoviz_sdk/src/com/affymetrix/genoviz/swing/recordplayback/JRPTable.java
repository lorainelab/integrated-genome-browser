package com.affymetrix.genoviz.swing.recordplayback;

import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class JRPTable extends JTable implements JRPWidget {
	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPTable(String id) {
		super();
		this.id = id;
		init();
	}
	public JRPTable(String id, int numRows, int numColumns) {
		super(numRows, numColumns);
		this.id = id;
		init();
	}
	public JRPTable(String id, Object[][] rowData, Object[] columnNames) {
		super(rowData, columnNames);
		this.id = id;
		init();
	}
	public JRPTable(String id, TableModel dm) {
		super(dm);
		this.id = id;
		init();
	}
	public JRPTable(String id, TableModel dm, TableColumnModel cm) {
		super(dm, cm);
		this.id = id;
		init();
	}
	public JRPTable(String id, TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
		super(dm, cm, sm);
		this.id = id;
		init();
	}
	@SuppressWarnings("rawtypes")
	public JRPTable(String id, Vector rowData, Vector columnNames) {
		super(rowData, columnNames);
		this.id = id;
		init();
	}
    private void init() {
		RecordPlaybackHolder.getInstance().addWidget(this);
//		addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				RecordPlaybackHolder.getInstance().recordOperation(new Operation(JRPTable.this));
//			}
//		});
    }

	@Override
	public String getId() {
		return id;
	}
}
