package com.affymetrix.igb.search;

import com.affymetrix.genometry.symmetry.SymWithProps;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.ColorTableCellRenderer;

public class GlyphSearchResultsTableModel extends SearchResultsTableModel {

    private static final long serialVersionUID = 1L;
    private final int[] colWidth = {20, 8, 10, 10, 10, 15, 50};
    private final int[] colAlign = {SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.RIGHT, SwingConstants.RIGHT, SwingConstants.CENTER, SwingConstants.CENTER, SwingConstants.LEFT};

    private final List<GlyphI> tableRows = new ArrayList<>(0);
    protected final String seq;

    public GlyphSearchResultsTableModel(List<GlyphI> results, String seq) {
        super();
        if (results != null) {
            tableRows.addAll(results);
        }
        this.seq = seq;
    }

    private final String[] column_names = {
        SearchView.BUNDLE.getString("searchTablePattern"),
        SearchView.BUNDLE.getString("searchTableColor"),
        SearchView.BUNDLE.getString("searchTableStart"),
        SearchView.BUNDLE.getString("searchTableEnd"),
        SearchView.BUNDLE.getString("searchTableStrand"),
        SearchView.BUNDLE.getString("searchTableChromosome"),
        SearchView.BUNDLE.getString("searchTableMatch")
    };

    private static final int PATTERN_COLUMN = 0;
    private static final int COLOR_COLUMN = 1;
    private static final int START_COLUMN = 2;
    private static final int END_COLUMN = 3;
    private static final int STRAND_COLUMN = 4;
    private static final int CHROM_COLUMN = 5;
    private static final int MATCH_COLUMN = 6;

    @Override
    public GlyphI get(int i) {
        return tableRows.get(i);
    }

    @Override
    public void clear() {
        tableRows.clear();
    }

    @Override
    public int getRowCount() {
        return tableRows.size();
    }

    @Override
    public int getColumnCount() {
        return column_names.length;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getValueAt(int row, int col) {
        GlyphI glyph = tableRows.get(row);
        SymWithProps info = (SymWithProps) glyph.getInfo();

        switch (col) {

            case PATTERN_COLUMN:
                Object pattern = info.getProperty("pattern");
                if (pattern != null) {
                    return pattern.toString();
                }
                return "";

            case COLOR_COLUMN:
                return glyph.getColor();

            case START_COLUMN:
                return (int) glyph.getCoordBox().x;

            case END_COLUMN:
                return (int) (glyph.getCoordBox().x + glyph.getCoordBox().width);

            case STRAND_COLUMN:
                Object direction = info.getProperty("direction");
                if (direction != null) {
                    if (direction.toString().equalsIgnoreCase("forward")) {
                        return "+";
                    } else if (direction.toString().equalsIgnoreCase("reverse")) {
                        return "-";
                    }
                }
                return "";

            case CHROM_COLUMN:
                return seq;

            case MATCH_COLUMN:
                Object match = info.getProperty("match");
                if (match != null) {
                    return match.toString();
                }
                return "";
        }

        return "";
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public String getColumnName(int col) {
        return column_names[col];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if (column == START_COLUMN || column == END_COLUMN) {
            return Number.class;
        }
        if (column == COLOR_COLUMN) {
            return Color.class;
        }
        return String.class;
    }

    @Override
    public int[] getColumnWidth() {
        return colWidth;
    }

    @Override
    public int[] getColumnAlign() {
        return colAlign;
    }

    @Override
    public DefaultTableCellRenderer getColumnRenderer(int column) {
        if (column == COLOR_COLUMN) {
            return new ColorTableCellRenderer();
        }
        return super.getColumnRenderer(column);
    }
}
