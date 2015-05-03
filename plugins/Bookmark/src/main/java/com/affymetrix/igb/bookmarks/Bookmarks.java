package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.igb.bookmarks.model.Bookmark;
import java.util.ArrayList;
import java.util.List;

/**
 * Inserts/modifies data for a DAS2 bookmark Stack like storage of urls/graph
 * ids
 *
 * @author Ido M. Tamir
 * @author hiralv
 * @version $Id$
 */
final public class Bookmarks {

    private final List<SymBookmark> syms = new ArrayList<>();

    public boolean add(DataSet feature, boolean isGraph) {
        if (feature == null) {
            return false;
        }

        addToSyms(feature, isGraph);

        return true;
    }

    private void addToSyms(DataSet feature, boolean isGraph) {
        DataContainer dataContainer = feature.getDataContainer();
        syms.add(new SymBookmark(dataContainer.getDataProvider().getUrl(), feature.getURI().toString(), isGraph));
    }

    /**
     * returns the current/last parser
     *
     */
    private SymBookmark getLast() {
        if (syms.size() > 0) {
            return syms.get(syms.size() - 1);
        }
        throw new IndexOutOfBoundsException("No parsers in bookmark");
    }

    /**
     * checks if graph is from DAS2 source and sets source_url to the id of the
     * graph which is the path
     *
     *
     */
    String getSource() {
        if (getLast().isGraph()) {
            return getLast().getPath();
        }
        return null;
    }

    /*
     * returns true if valid url can be constructed
     *
     */
    boolean isGraph() {
        return getLast().isGraph();
    }

    /**
     * sets the das2 and quickload properties of the bookmark and deletes
     * source_url.
     */
    public void set(SymWithProps mark_sym) {
        List<String> queries = new ArrayList<>();
        List<String> servers = new ArrayList<>();

        this.syms.stream().filter(bookmark -> !bookmark.isGraph()).forEach(bookmark -> {
            servers.add(bookmark.getServer());
            queries.add(bookmark.getPath());
        });

        mark_sym.setProperty(Bookmark.QUERY_URL, queries);
        mark_sym.setProperty(Bookmark.SERVER_URL, servers);

    }

    public List<SymBookmark> getSyms() {
        return syms;
    }
}
