package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import javax.swing.table.AbstractTableModel;

final class SeqGroupTableModel extends AbstractTableModel {

	AnnotatedSeqGroup group;

	public SeqGroupTableModel(AnnotatedSeqGroup seq_group) {
		group = seq_group;
	}

	public int getRowCount() {
		return (group == null ? 0 : group.getSeqCount());
	}

	public int getColumnCount() {
		return 2;
	}

	public Object getValueAt(int row, int col) {
		if (group != null) {
			BioSeq seq = group.getSeq(row);
			if (col == 0) {
				return seq.getID();
			} else if (col == 1) {
				return Long.toString((long) seq.getLengthDouble());
			}
		}
		return null;
	}

	@Override
	public String getColumnName(int col) {
		if (col == 0) {
			return "Sequence";
		} else if (col == 1) {
			return "Length";
		} else {
			return null;
		}
	}

	@Override
	public Class<?> getColumnClass(int c) {
		switch (c) {
			case 1:
				return Integer.class;
			default:
				return super.getColumnClass(c);
		}
	} 
}
