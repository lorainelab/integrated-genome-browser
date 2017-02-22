package com.affymetrix.igb.searchmodeidorprops;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.search.SearchUtils;
import com.affymetrix.genometry.symmetry.SupportsGeneName;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometry.util.Constants;
import com.affymetrix.genometry.util.ErrorHandler;
import org.lorainelab.igb.services.search.ISearchModeSym;
import org.lorainelab.igb.services.search.IStatus;
import org.lorainelab.igb.services.search.SearchResults;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.lang3.StringUtils;

public abstract class SearchModeIDOrProps implements ISearchModeSym {

    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("searchmodeidorprops");
    private static final int MAX_HITS = 100000;
    protected static final String FRIENDLY_PATTERN = BUNDLE.getString("friendlyPattern");

    protected static final IStatus DUMMY_STATUS = s -> {
    };

    public SearchModeIDOrProps() {

    }

    private Pattern getRegex(String search_text) throws Exception {
        Pattern regex = null;
        String regexText = search_text;
        // Make sure this search is reasonable to do on a remote server.
        if (!(regexText.contains("*") || regexText.contains("^") || regexText.contains("$"))) {
            // Not much of a regular expression.  Assume the user wants to match at the start and end
            regexText = ".*" + regexText + ".*";
        }
        regex = Pattern.compile(regexText, Pattern.CASE_INSENSITIVE);
        return regex;
    }

    @Override
    public String checkInput(String search_text, BioSeq vseq, String seq) {
        try {
            getRegex(search_text);
        } catch (PatternSyntaxException pse) {
            return MessageFormat.format(BUNDLE.getString("searchErrorSyntax"), pse.getMessage());
        } catch (Exception ex) {
            return MessageFormat.format(BUNDLE.getString("searchError"), ex.getMessage());
        }
        return null;
    }

    protected static List<SeqSymmetry> filterBySeq(List<SeqSymmetry> results, BioSeq seq) {

        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        int num_rows = results.size();

        List<SeqSymmetry> rows = new ArrayList<>(num_rows / 10);
        for (int j = 0; j < num_rows && rows.size() < MAX_HITS; j++) {
            SeqSymmetry result = results.get(j);

            SeqSpan span = null;
            if (seq != null) {
                span = result.getSpan(seq);
                if (span == null) {
                    // Special case when chromosomes are not equal, but have same ID (i.e., really they're equal)
                    SeqSpan tempSpan = result.getSpan(0);
                    if (tempSpan != null && tempSpan.getBioSeq() != null && seq.getId().equals(tempSpan.getBioSeq().getId())) {
                        span = tempSpan;
                    }
                }
            } else {
                span = result.getSpan(0);
            }
            if (span == null) {
                continue;
            }

            rows.add(result);
        }

        return rows;
    }

    protected SearchResults<SeqSymmetry> search(final String search_text, final BioSeq chrFilter, IStatus statusHolder, boolean remote, final boolean search_props) {
        GenometryModel gmodel = GenometryModel.getInstance();
        GenomeVersion genomeVersion = gmodel.getSelectedGenomeVersion();
        String text = search_text;

        String seq = chrFilter == null ? Constants.GENOME_SEQ_ID : chrFilter.getId();
        List<SeqSymmetry> localSymList = findLocalSyms(search_text, chrFilter, seq, search_props, statusHolder);
        List<SeqSymmetry> remoteSymList = null;

        // Make sure this search is reasonable to do on a remote server.
        if (!(text.contains("*") || text.contains("^") || text.contains("$"))) {
            // Not much of a regular expression.  Assume the user wants to match at the start and end
            text = "*" + text + "*";
        }
        String friendlySearchStr = MessageFormat.format(FRIENDLY_PATTERN, text, seq);
        int actualChars = text.length();
        if (text.startsWith(".*")) {
            actualChars -= 2;
        } else if (text.startsWith("*")) {
            actualChars -= 1;
        }
        if (text.endsWith(".*")) {
            text = text.substring(0, text.length() - 2) + "*";	// hack for bug in DAS/2 server
            actualChars -= 2;
        } else if (text.endsWith("*")) {
            actualChars -= 1;
        }

        String statusStr;
        if (remote) {
            if (actualChars < 3) {
                statusStr = MessageFormat.format(BUNDLE.getString("searchErrorShort"), friendlySearchStr);
                ErrorHandler.errorPanel(statusStr);
                return new SearchResults<>(getName(), search_text, chrFilter != null ? chrFilter.getId() : "genome", statusStr, null);
            }

            //remoteSearches
            remoteSymList = new ArrayList<>();
        }

        if (localSymList.isEmpty() && (remoteSymList == null || remoteSymList.isEmpty())) {
            statusStr = BUNDLE.getString("searchNoResults");
            statusHolder.setStatus(statusStr);
            return new SearchResults<>(getName(), search_text, chrFilter != null ? chrFilter.getId() : "genome", statusStr, null);
        }

        statusStr = MessageFormat.format(BUNDLE.getString("searchSummary"),
                (localSymList == null ? "0" : "" + localSymList.size()),
                BUNDLE.getString("forLocalSearch"));

        if (remote && actualChars >= 3) {
            statusStr += MessageFormat.format(BUNDLE.getString("searchSummary"),
                    (remoteSymList == null ? "0" : "" + remoteSymList.size()),
                    BUNDLE.getString("forRemoteSearch"));
        }
        statusHolder.setStatus(statusStr);

        if (remoteSymList != null) {
            localSymList.addAll(remoteSymList);
        }

        List<SeqSymmetry> tableRows = filterBySeq(localSymList, chrFilter);

        return new SearchResults<>(getName(), search_text, chrFilter != null ? chrFilter.getId() : "genome", statusStr, tableRows);
    }

    protected List<SeqSymmetry> findLocalSyms(String search_text, final BioSeq chrFilter, final String seq, final boolean search_props, final IStatus statusHolder) {
        GenometryModel gmodel = GenometryModel.getInstance();
        GenomeVersion genomeVersion = gmodel.getSelectedGenomeVersion();
        String text = search_text;
        Pattern regex = null;
        try {
            regex = getRegex(search_text);
        } catch (Exception e) {
        } // should not happen, already checked above

        String friendlySearchStr = MessageFormat.format(FRIENDLY_PATTERN, text, seq);
        statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchSearchingLocal"), friendlySearchStr));
        List<SeqSymmetry> localSymList = SearchUtils.findLocalSyms(genomeVersion, chrFilter, regex, search_props);
        //added sorting based on ID, Name and proprties if properties are searched
        if (!search_props) {
            Collections.sort(localSymList, (SeqSymmetry o1, SeqSymmetry o2) -> {
                String search = search_text.toLowerCase();
                Integer i1 = StringUtils.getLevenshteinDistance(o1.getID().toLowerCase(), search.toLowerCase());
                Integer i2 = StringUtils.getLevenshteinDistance(o2.getID().toLowerCase(), search.toLowerCase());
                if (o1 instanceof SupportsGeneName && o2 instanceof SupportsGeneName) {
                    int j1 = StringUtils.getLevenshteinDistance(((SupportsGeneName) o1).getGeneName().toLowerCase(), search.toLowerCase());
                    int j2 = StringUtils.getLevenshteinDistance(((SupportsGeneName) o2).getGeneName().toLowerCase(), search.toLowerCase());
                    i1 = i1 < j1 ? i1 : j1;
                    i2 = i2 < j2 ? i2 : j2;
                }
                return (i2 < i1) ? 1 : -1;
            });
        } else {
            Collections.sort(localSymList, (SeqSymmetry o1, SeqSymmetry o2) -> {
                //SymWithProps
                if (o1 instanceof SymWithProps && o2 instanceof SymWithProps) {
                    int[] dist = {0, 0};
                    ((SymWithProps) o1).getProperties().values().stream().mapToInt(val -> StringUtils.getLevenshteinDistance(val.toString(), text)).min().ifPresent(min -> dist[0] = min);
                    ((SymWithProps) o2).getProperties().values().stream().mapToInt(val -> StringUtils.getLevenshteinDistance(val.toString(), text)).min().ifPresent(min -> dist[1] = min);
                    return dist[0] - dist[1];
                }
                return 0;
            });
        }
        return localSymList;
    }

    @Override
    public List<SeqSymmetry> searchTrack(String search_text, TypeContainerAnnot contSym) {
        return null;
    }
}
