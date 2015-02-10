package com.lorainelab.igb.preferences.weblink.model;

import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.CdsSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.BioSeqUtils;
import com.affymetrix.genometry.util.SpeciesLookup;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author hiralv
 */
public class WebLinkList {

    private final String name;
    private final boolean allowDuplicates;
    private final List<WebLink> weblink_list;

    public WebLinkList(String name, boolean allowDuplicates) {
        this.name = name;
        this.allowDuplicates = allowDuplicates;
        this.weblink_list = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<WebLink> getWebLinkList() {
        return weblink_list;
    }

    public void addWebLink(WebLink wl) {
        if (wl.getName() == null || wl.getName().trim().length() == 0) {
            return;
        }
        //TODO: Allow duplicates for local
        if (allowDuplicates) {
            weblink_list.add(wl);
        } else if (!isContained(weblink_list, wl)) {
            weblink_list.add(wl);
        }
    }

    /**
     * Remove a WebLink from the static list.
     */
    public void removeWebLink(WebLink link) {
        Iterator<WebLink> it = weblink_list.iterator();
        while (it.hasNext()) {
            WebLink item = it.next();
            if (link == item) {
                it.remove();
            }
        }
    }

    /**
     * Get all web-link patterns for the given method name.
     * These can come from regular-expression matching from the semi-obsolete
     * XML-based preferences file, or from UCSC-style track lines in the
     * input files. It is entirely possible that some of the WebLinks in the
     * array will have the same regular expression or point to the same URL.
     * You may want to filter-out such duplicate results.
     */
    public List<WebLink> getWebLinks(SeqSymmetry sym) {
        if (sym instanceof CdsSeqSymmetry) {
            sym = ((CdsSeqSymmetry) sym).getPropertySymmetry();
        }

        // Most links come from matching the tier name (i.e. method)
        // to a regular expression.
        String method = BioSeqUtils.determineMethod(sym);
        if (method == null) { // rarely happens, but can
            return Collections.<WebLink>emptyList();
        }

        List<WebLink> results = new ArrayList<>();

        // If the method name has already been used, then the annotStyle must have already been created
        ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method);
        String style_url = style.getUrl();
        if (style_url != null && style_url.length() > 0) {
            WebLink link = new WebLink("Track Line URL", null, style_url, WebLink.RegexType.ANNOTATION_NAME);
            results.add(link);
        }

        Set<WebLink> webLinks = new HashSet<>();
        webLinks.addAll(getWebLink(sym, method));

        //	if (webLinks.isEmpty()) {
        if (style.getFeature() != null) {
            webLinks.addAll(getWebLink(sym, style.getFeature().featureName));
        }
        //	}

        results.addAll(webLinks);

        Collections.sort(results);
        return results;
    }

    private List<WebLink> getWebLink(SeqSymmetry sym, String method) {
        List<WebLink> results = new ArrayList<>();

        for (WebLink link : weblink_list) {
            if (link.getUrl() == null) {
                continue;
            }
            if (!Strings.isNullOrEmpty(link.getSpeciesName())) {
                String current_version = GenometryModel.getInstance().getSelectedSeqGroup().getID();
                String current_species = SpeciesLookup.getSpeciesName(current_version);
                boolean isSynonym = SpeciesLookup.isSynonym(current_species, link.getSpeciesName());
                if (!isSynonym) {
                    continue;
                }
            }
            if (link.getIDField() != null && link.getIDField().trim().length() > 0) {
                // Allow matching of arbitrary id_field

                if (!(sym instanceof SymWithProps)) {
                    continue;
                }
                String property = (String) ((SymWithProps) sym).getProperty(link.getIDField());
                if (property != null && link.matches(property)) {
                    results.add(link);
                }
                continue;
            }

            if (link.getRegexType() == WebLink.RegexType.ANNOTATION_NAME && link.matches(method)) {
                results.add(link);
            } else if (link.getRegexType() == WebLink.RegexType.ANNOTATION_ID && link.matches(sym.getID())) {
                results.add(link);
            }
        }

        return results;
    }

    private static boolean isContained(List<WebLink> list, WebLink link) {
        for (WebLink l : list) {
            if (l.getName().equals(link.getName())
                    && l.getUrl().equals(link.getUrl())
                    && l.getRegex().equals(link.getRegex())) {
                return true;
            }
        }

        return false;
    }

}
