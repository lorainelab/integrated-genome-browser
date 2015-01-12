package com.gene.findannotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.table.DefaultTableModel;

import org.apache.commons.lang3.ArrayUtils;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;

public class AnnotationsTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 1L;
    private static final String ID_SEARCH_TERM = "id";
    private static final int ID_EXACT_RANK = 1000000;
    private static final int ID_REGEX_RANK = 10000;
    private static final int PROPERTY_EXACT_RANK = 100;
    private static final int PROPERTY_REGEX_RANK = 1;
    private static final List<String> IGNORE_LIST = new ArrayList<>();

    static {
        IGNORE_LIST.add("method");
        IGNORE_LIST.add("type");
        IGNORE_LIST.add("source");
        IGNORE_LIST.add("seq");
        IGNORE_LIST.add("seq id");
        IGNORE_LIST.add("start");
        IGNORE_LIST.add("end");
    }
    private String searchText;
    private Pattern regex;
    private List<SeqSymmetry> results;
    private final List<String> columnNames;

    public AnnotationsTableModel() {
        super();
        this.results = new ArrayList<>();
        columnNames = new ArrayList<>();
    }

    public List<SeqSymmetry> getResults() {
        return results;
    }

    public synchronized void setResults(String searchText, List<SeqSymmetry> results) {
        this.searchText = searchText;
        this.regex = getRegex(searchText);
        this.results = new ArrayList<>(results);
        Collections.sort(results,
                new Comparator<SeqSymmetry>() {
                    @Override
                    public int compare(SeqSymmetry o1, SeqSymmetry o2) {
                        return rankSymmetry(o1) - rankSymmetry(o2);
                    }
                }
        );
        columnNames.clear();
        columnNames.add(FindAnnotationsView.BUNDLE.getString("findannotationsId"));
        columnNames.add(FindAnnotationsView.BUNDLE.getString("findannotationsRegion"));
        if (results != null) {
            for (SeqSymmetry sym : results) {
                if (sym instanceof SymWithProps) {
                    for (String key : ((SymWithProps) sym).getProperties().keySet()) {
                        if (!columnNames.contains(key) && !IGNORE_LIST.contains(key)) {
                            columnNames.add(key);
                        }
                    }
                }
            }
        }
    }

    private Pattern getRegex(String search_text) {
        if (search_text == null) {
            search_text = "";
        }
        String regexText = search_text;
        // Make sure this search is reasonable to do on a remote server.
        if (!(regexText.contains("*") || regexText.contains("^") || regexText.contains("$"))) {
            // Not much of a regular expression.  Assume the user wants to match at the start and end
            regexText = ".*" + regexText + ".*";
        }
        Pattern regex = null;
        try {
            regex = Pattern.compile(regexText, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "error with regular expression " + search_text, e);
            regex = null;
        }
        return regex;
    }

    private int rankSymmetry(SeqSymmetry sym) {
        Matcher matcher = regex.matcher("");
        Map<String, String> searchTerms = new HashMap<>();
        int ranking = 0;
        String match = sym.getID();
        if (match != null) {
            matcher.reset(match);
            if (matcher.matches()) {
                ranking = match.equals(searchText) ? ID_EXACT_RANK : ID_REGEX_RANK;
                searchTerms.put(ID_SEARCH_TERM, searchText);
            }
        }
        if (sym instanceof SymWithProps) {
            SymWithProps swp = (SymWithProps) sym;

            // Iterate through each properties.
            for (Map.Entry<String, Object> prop : swp.getProperties().entrySet()) {
                if (prop.getValue() != null) {
                    match = ArrayUtils.toString(prop.getValue());
                    matcher.reset(match);
                    if (matcher.matches()) {
                        ranking += match.equals(searchText) ? PROPERTY_EXACT_RANK : PROPERTY_REGEX_RANK;
                        searchTerms.put(prop.getKey(), searchText);
                    }
                }
            }
        }
        return ranking;
    }

    @Override
    public int getRowCount() {
        return (results == null) ? 0 : results.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    private String formatCell(String text) {
        String formattedCell = text;
        if (searchText != null) {
            if ("".equals(searchText)) {
                formattedCell = "<b>" + formattedCell + "</b>";
            } else {
//				formattedCell = formattedCell.replaceAll(searchText, "<b>" + searchText + "</b>");
                Matcher m = Pattern.compile(searchText, Pattern.CASE_INSENSITIVE).matcher(formattedCell);
                StringBuffer sb = new StringBuffer();

                while (m.find()) {
                    String replacement = "<b>" + m.group() + "</b>";
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                }
                m.appendTail(sb);

                formattedCell = sb.toString();
            }
        }
        int sortOrder = 3;
        if (searchText != null) {
            if (searchText.equals(text)) {
                sortOrder = 1;
            } else if (text.indexOf(searchText) > -1) {
                sortOrder = 2;
            }
        }
        return "<html>" + "<!-- " + sortOrder + " " + text + " -->" + formattedCell + "</html>";
    }

    private static final String BLANK_CELL = "<html><!-- 3 --></html>";

    @Override
    public Object getValueAt(int row, int column) {
        String valueAt;
        switch (column) {
            case 0:
                valueAt = BLANK_CELL;
                if (results != null) {
                    valueAt = formatCell(results.get(row).getID());
                }
                return valueAt;
            case 1:
                valueAt = BLANK_CELL;
                if (results != null) {
                    SeqSpan span = results.get(row).getSpan(0);
                    String region = (span.getBioSeq() == null ? "???" : span.getBioSeq().getID()) + ":" + span.getStart() + "-" + span.getEnd();
                    valueAt = formatCell(region);
                }
                return valueAt;
            default:
                valueAt = BLANK_CELL;
                if (results != null) {
                    SeqSymmetry sym = results.get(row);
                    if (sym instanceof SymWithProps) {
                        String key = columnNames.get(column);
                        Object value = ((SymWithProps) sym).getProperty(key);
                        if (value != null) {
                            valueAt = formatCell(ArrayUtils.toString(value));
                        }
                    }
                }
                return valueAt;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
