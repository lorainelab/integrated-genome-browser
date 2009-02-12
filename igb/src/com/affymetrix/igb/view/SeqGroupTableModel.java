package com.affymetrix.igb.view;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.seq.CompositeNegSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
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
			MutableAnnotatedBioSeq seq = group.getSeq(row);
			if (col == 0) {
				return seq.getID();
			} else if (col == 1) {
				if (seq instanceof CompositeNegSeq) {
					return Long.toString((long) ((CompositeNegSeq) seq).getLengthDouble());
				} else {
					return Integer.toString(seq.getLength());
				}
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
}
