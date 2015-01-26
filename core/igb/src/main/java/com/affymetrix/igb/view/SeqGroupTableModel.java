package com.affymetrix.igb.view;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.igb.IGBConstants;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.table.AbstractTableModel;


final class SeqGroupTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	private final AnnotatedSeqGroup group;
	private static final NumberFormat nformat = NumberFormat.getIntegerInstance(Locale.ENGLISH);

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
			if (seq != null) {
				if (col == 0) {
					return seq.getID();
				} else if (col == 1) {
					if (IGBConstants.GENOME_SEQ_ID.equals(seq.getID())) {
						return "";	// don't show the "whole genome" size, because it disagrees with the chromosome total
					}
					return Long.toString((long) seq.getLengthDouble());
				}
			}
		}
		return null;
	}

	@Override
	public String getColumnName(int col) {
		if (col == 0) {
			int n = getRowCount();
			//Exclude 'genome'
			if (n >= 2) {
				n -= 1;
			}
			return MessageFormat.format(IGBConstants.BUNDLE.getString("sequenceColumnHeader"), nformat.format(n));
		} else if (col == 1) {
			return IGBConstants.BUNDLE.getString("lengthColumnHeader");
		} else {
			return null;
		}
	}

	@Override
	public Class<?> getColumnClass(int c) {
		return getValueAt(0, c) == null ? 
				String.class : getValueAt(0, c).getClass();
	}
	
}
