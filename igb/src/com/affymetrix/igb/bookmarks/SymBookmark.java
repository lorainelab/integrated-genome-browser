package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;


/**
 * @version $Id$
 */
final class SymBookmark {
      private final String server;
      private final String path;
	  private final ServerType serverType;
      private final boolean isgraph;

      SymBookmark(String server, String path, ServerType serverType, boolean isgraph){
			this.server = server;
			this.path = path;
			this.serverType = serverType;
			this.isgraph = isgraph;
      }

	  public ServerType getServerType(){
		  return serverType;
	  }
	  
      String getServer(){
          return server;
      }
            
      String getPath(){
          return path;
      }
        
	  boolean isGraph(){
		  return isgraph;
	  }
}

