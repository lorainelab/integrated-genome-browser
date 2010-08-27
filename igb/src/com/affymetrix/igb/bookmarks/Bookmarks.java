package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import java.util.ArrayList;
import java.util.List;

/**
 * Inserts/modifies data for a DAS2 bookmark
 * Stack like storage of urls/graph ids
 * 
 * @author Ido M. Tamir
 * @author hiralv
 * @version $Id$
 */
final public class Bookmarks {
    
    private final List<SymBookmark> syms = new ArrayList<SymBookmark>();

	public boolean add(GenericFeature feature){
		if(feature == null)
			return false;

		return addToSyms(feature);
	}
	
    /**
     * adds one id
     *
	 * this is a very complicated way to go from a graph to its server
	 * I am sure a simpler way exists, but I could not find it.
	 *
     * @param id the id of the graph
     */
    void addGraph(GraphSym graph){
		if(!checkServerMatch(graph)){
			syms.add(new SymBookmark());
		}
    }

	private boolean checkServerMatch(GraphSym graph) {
		BioSeq seq = graph.getGraphSeq();
	    AnnotatedSeqGroup as = seq.getSeqGroup();
		String gid = graph.getID();
		for(GenericVersion version : as.getEnabledVersions()){
			for( GenericFeature feature : version.getFeatures()){
				if(gid.endsWith(feature.featureName)){
					return addToSyms(feature);
				}
			}
		}
		return false;
	}

	private boolean addToSyms(GenericFeature feature){
		GenericVersion version = feature.gVersion;

		if(version.gServer.serverType != ServerType.LocalFiles){
			syms.add(new SymBookmark(version.gServer.URL, feature.getURI().toString(), version.gServer.serverType));
			return true;
		}

		return false;
	}

    /**
    * returns the current/last parser
    *
    */ 
    private SymBookmark getLast(){
        if(syms.size() > 0){
            return syms.get(syms.size() - 1);
        }
        throw new IndexOutOfBoundsException("No parsers in bookmark");
    }
    
    /**
    * checks if graph is from DAS2 source and sets source_url to the id of the graph
    * which is the path
    * 
    *
    */
    String getSource() {
        if(getLast().isValid()){
            return getLast().getServer();
        }
        return null;
    }
    
    /*
    * returns true if valid url can be constructed
    *
    */
    boolean isValid(){
        return getLast().isValid();
    }
    
	 
    /**
    * sets the das2 and quickload properties of the bookmark and deletes source_url.
    */ 
   public void set(SymWithProps mark_sym) {
		List<String> queries = new ArrayList<String>();
		List<String> servers = new ArrayList<String>();

        for(SymBookmark bookmark : this.syms){
            if(bookmark.isValid()){
				if(bookmark.getServerType() != ServerType.LocalFiles){
					servers.add(bookmark.getServer());
					queries.add(bookmark.getPath());
				}
			}
        }
		
		mark_sym.setProperty(Bookmark.QUERY_URL, queries.toArray(new String[queries.size()]));
	    mark_sym.setProperty(Bookmark.SERVER_URL, servers.toArray(new String[servers.size()]));

    }

   public List<SymBookmark> getSyms(){
	   return syms;
   }
}

