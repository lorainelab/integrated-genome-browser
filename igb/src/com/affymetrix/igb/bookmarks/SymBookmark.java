package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;


/**
 * @version $Id$
 */
final class SymBookmark {
      private final String server;
      private final String path;
	  private final ServerType serverType;
      private final boolean valid;

      SymBookmark(String server, String path, ServerType serverType){
			this.server = server;
			this.path = path;
			this.serverType = serverType;
			this.valid = true;
      }

	  SymBookmark(){
			this.server = "";
			this.path = "";
			this.serverType = null;
			this.valid = false;
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
        
	  boolean isValid(){
		  return valid;
	  }


}

