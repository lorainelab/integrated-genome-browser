package com.affymetrix.genometry.event;

import com.affymetrix.genometry.GenomeVersion;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

public final class GenomeVersionSelectionEvent extends EventObject {

    private List<GenomeVersion> selected_groups;
    private GenomeVersion primary_selection = null;
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param src The source of the event
     * @param groups a List of GenomeVersion's that have been selected.
 (If null, will default to {@link Collections#EMPTY_LIST}.)
     */
    public GenomeVersionSelectionEvent(Object src, List<GenomeVersion> groups) {
        super(src);
        this.selected_groups = groups;
        this.primary_selection = null;
        if (selected_groups == null) {
            selected_groups = Collections.<GenomeVersion>emptyList();
        } else if (!selected_groups.isEmpty()) {
            primary_selection = groups.get(0);
        }
    }

    /**
     * Gets the first entry in the list {@link #getSelectedGroup()}.
     *
     * @return an GenomeVersion or null.
     */
    public GenomeVersion getSelectedGroup() {
        return primary_selection;
    }

    @Override
    public String toString() {
        return "GroupSelectionEvent: group count: " + selected_groups.size()
                + " first group: '" + (primary_selection == null ? "null" : primary_selection.getName())
                + "' source: " + this.getSource();
    }
}
