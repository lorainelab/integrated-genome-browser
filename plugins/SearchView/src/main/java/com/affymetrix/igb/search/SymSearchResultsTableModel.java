package com.affymetrix.igb.search;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.das2.SimpleDas2Feature;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.UcscPslSym;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.DESCRIPTION;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.TITLE;
import com.affymetrix.genometry.util.BioSeqUtils;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingConstants;

public class SymSearchResultsTableModel extends SearchResultsTableModel {

    private static final long serialVersionUID = 1L;
    private final int[] colWidth = {15, 15, 30, 20, 10, 10, 8, 6};
    private final int[] colAlign = {SwingConstants.LEFT, SwingConstants.LEFT,
        SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.RIGHT,
        SwingConstants.RIGHT, SwingConstants.CENTER, SwingConstants.CENTER};

    protected final List<SeqSymmetry> tableRows = new ArrayList<>(0);

    private final String[] column_names = {
        SearchView.BUNDLE.getString("searchTableID"),
        SearchView.BUNDLE.getString("searchTableTitle"),
        SearchView.BUNDLE.getString("searchTableDescription"),
        SearchView.BUNDLE.getString("searchTableTier"),
        SearchView.BUNDLE.getString("searchTableStart"),
        SearchView.BUNDLE.getString("searchTableEnd"),
        SearchView.BUNDLE.getString("searchTableChromosome"),
        SearchView.BUNDLE.getString("searchTableStrand")
    };
    private static final int ID_COLUMN = 0;
    private static final int GENE_NAME_COLUMN = 1;
    private static final int DESCRIPTION_COLUMN = 2;
    private static final int TIER_COLUMN = 3;
    private static final int START_COLUMN = 4;
    private static final int END_COLUMN = 5;
    private static final int CHROM_COLUMN = 6;
    private static final int STRAND_COLUMN = 7;

    public SymSearchResultsTableModel(List<SeqSymmetry> results) {
        super();
        if (results != null) {
            tableRows.addAll(results);
        }
    }

    @Override
    public Object getValueAt(int row, int col) {
        SeqSymmetry sym = tableRows.get(row);
        SeqSpan span = sym.getSpan(0);
        switch (col) {
            case ID_COLUMN:
                return sym.getID();
            case GENE_NAME_COLUMN:
                if (sym instanceof SimpleDas2Feature) {
                    String geneName = ((SimpleDas2Feature) sym).getName();
                    return geneName == null ? "" : geneName;
                }
                if (sym instanceof SymWithProps) {
                    String geneName = (String) ((SymWithProps) sym).getProperty(TITLE);
                    return geneName == null ? "" : geneName;
                }
                return "";
            case DESCRIPTION_COLUMN:
                if (sym instanceof SymWithProps) {
                    String description = (String) ((SymWithProps) sym).getProperty(DESCRIPTION);
                    return description == null ? "" : description;
                }
                return "";
            case TIER_COLUMN:
                String method = BioSeqUtils.determineMethod(sym);
                if (method != null) {
                    return DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method).getTrackName();
                }
                return "";
            case START_COLUMN:
                if (sym instanceof UcscPslSym) {
                    return (((UcscPslSym) sym).getSameOrientation())
                            ? ((UcscPslSym) sym).getTargetMin() : ((UcscPslSym) sym).getTargetMax();
                }
                return (span == null ? "" : span.getStart());
            case END_COLUMN:
                if (sym instanceof UcscPslSym) {
                    return (((UcscPslSym) sym).getSameOrientation())
                            ? ((UcscPslSym) sym).getTargetMax() : ((UcscPslSym) sym).getTargetMin();
                }
                return (span == null ? "" : span.getEnd());
            case CHROM_COLUMN:
                if (sym instanceof UcscPslSym) {
                    return ((UcscPslSym) sym).getTargetSeq().getID();
                }
                return ((span == null || span.getBioSeq() == null) ? "" : span.getBioSeq().getID());
            case STRAND_COLUMN:
                if (sym instanceof UcscPslSym) {
                    return ((((UcscPslSym) sym).getSameOrientation())
                            ? "+" : "-");
                }
                if (span == null) {
                    return "";
                }
                return (span.isForward() ? "+" : "-");
        }
        return "";
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public int getColumnCount() {
        return column_names.length;
    }

    @Override
    public String getColumnName(int col) {
        return column_names[col];
    }

    @Override
    public int getRowCount() {
        return tableRows.size();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if (column == START_COLUMN || column == END_COLUMN) {
            return Number.class;
        }
        return String.class;
    }

    @Override
    public SeqSymmetry get(int i) {
        return tableRows.get(i);
    }

    @Override
    public void clear() {
        tableRows.clear();
    }

    @Override
    public int[] getColumnWidth() {
        return colWidth;
    }

    @Override
    public int[] getColumnAlign() {
        return colAlign;
    }
}
