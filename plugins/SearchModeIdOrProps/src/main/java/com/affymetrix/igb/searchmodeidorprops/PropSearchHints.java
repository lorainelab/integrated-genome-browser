package com.affymetrix.igb.searchmodeidorprops;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.search.SearchUtils;
import com.lorainelab.igb.service.search.ISearchHints;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author hiralv
 */
@Component
public class PropSearchHints implements ISearchHints {

    @Override
    public Set<String> search(String search_term) {
        String regexText = search_term;
        if (!(regexText.contains("*") || regexText.contains("^") || regexText.contains("$"))) {
            // Not much of a regular expression.  Assume the user wants to match at the start and end
            regexText = ".*" + regexText + ".*";
        }
        Pattern regex;
        try {
            regex = Pattern.compile(regexText, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            regex = Pattern.compile("");
        }
        return SearchUtils.findLocalSyms(GenometryModel.getInstance().getSelectedSeqGroup(), regex, true, 20);
    }
}
