package com.affymetrix.igb.view;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.igb.IGBConstants;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.table.AbstractTableModel;

final class SeqGroupTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private GenomeVersion genomeVersion;
    private static final NumberFormat nformat = NumberFormat.getIntegerInstance(Locale.ENGLISH);

    public SeqGroupTableModel(GenomeVersion genomeVersion) {
        this.genomeVersion = genomeVersion;
    }

    public void setGenomeVersion(GenomeVersion genomeVersion) {
        this.genomeVersion = genomeVersion;
    }

    @Override
    public int getRowCount() {
        return (genomeVersion == null ? 0 : genomeVersion.getSeqCount());
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (genomeVersion != null) {
            BioSeq seq = genomeVersion.getSeq(row);
            if (seq != null) {
                if (col == 0) {
                    return seq.getId();
                } else if (col == 1) {
                    if (IGBConstants.GENOME_SEQ_ID.equals(seq.getId())) {
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
        return getValueAt(0, c) == null
                ? String.class : getValueAt(0, c).getClass();
    }

}
