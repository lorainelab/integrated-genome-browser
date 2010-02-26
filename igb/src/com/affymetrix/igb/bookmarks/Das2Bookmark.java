package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import java.util.ArrayList;
import java.util.List;

/**
 * Inserts/modifies data for a DAS2 bookmark
 * Stack like storage of urls/graph ids
 * 
 * @author Ido M. Tamir
 */
public final class Das2Bookmark {
    
    private final List<GraphBookmark> graphs = new ArrayList<GraphBookmark>();
    
    /**
     * adds one id
     *
	 * this is a very complicated way to go from a graph to its server
	 * I am sure a simpler way exists, but I could not find it.
	 *
     * @param id the id of the graph
     */
    public void add(GraphSym graph){
		if(!checkServerMatch(graph)){
			graphs.add(new GraphBookmark());
		}
    }

	private boolean checkServerMatch(GraphSym graph) {
		BioSeq seq = graph.getGraphSeq();
	    AnnotatedSeqGroup as = seq.getSeqGroup();
		String gid = graph.getID();
		for(GenericVersion version : as.getEnabledVersions()){
			for( GenericFeature feature : version.getFeatures()){
				if(gid.endsWith(feature.featureName)){
					GenericServer server = version.gServer;
					if(server.serverType == ServerType.DAS2){
						graphs.add(new GraphBookmark(server.URL, version.versionID, feature.featureName, "bar"));
						return true;
					}
				}
			}
		}
		return false;
	}
    
    /**
    * returns the current/last parser
    *
    */ 
    private GraphBookmark getLast(){
        if(graphs.size() > 0){
            return graphs.get(graphs.size() - 1);
        }
        throw new IndexOutOfBoundsException("No parsers in bookmark");
    }
    
    /**
    * checks if graph is from DAS2 source and sets source_url to the id of the graph
    * which is the path
    * 
    *
    */
    public String getSource() {
        if(getLast().isValid()){
            return getLast().getServer();
        }
        return null;
    }
    
    /*
    * returns true if valid url can be constructed
    *
    */
    public boolean isValid(){
        return getLast().isValid();
    }
    
    /**
    * from url + region to das2 query
    * 
    *
    */
     public static String getDas2Query(GraphBookmark bookmark, int start, int end, String chr){
         String cap = bookmark.getCapability() + "?";
         String seg = "segment=" + bookmark.getServer() + "/" + bookmark.getMapping() + "/" + chr + ";";
         String over = "overlaps=" + start + ":" + end + ";";
         String type = "type=" +  bookmark.getType() + ";" + "format=" + bookmark.getFormat();
         return cap + seg + over + type;
     }

    /**
    * sets the das2 properties of the bookmark and deletes source_url
    * 
    *
    */ 
   public void set(SymWithProps mark_sym) {
        List<String> queries = new ArrayList<String>();
        List<String> servers = new ArrayList<String>();
        for(GraphBookmark bookmark : this.graphs){
            if(bookmark.isValid()){
                String server = bookmark.getServer();
                int start = (Integer) mark_sym.getProperty("start");
                int end = (Integer) mark_sym.getProperty("end");
                String chr = (String) mark_sym.getProperty("seqid");
                String query = getDas2Query(bookmark, start, end, chr);
                servers.add(server);
                queries.add(query);
            }
        }
        mark_sym.setProperty(Bookmark.DAS2_QUERY_URL, queries.toArray(new String[queries.size()]));
        mark_sym.setProperty(Bookmark.DAS2_SERVER_URL, servers.toArray(new String[servers.size()]));
    }
    
}

