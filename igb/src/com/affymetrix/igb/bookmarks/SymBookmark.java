package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;



final class SymBookmark {
      private final String server;
      private final String mapping;
      private final String path;
	  private final String format;
	  private final ServerType serverType;
      private final boolean valid;

      SymBookmark(String server, String mapping, String path, String format, ServerType serverType){
			this.server = server;
			this.mapping = mapping;
			this.path = path;
			this.format = format;
			this.serverType = serverType;
			this.valid = true;
      }

	  SymBookmark(){
			this.server = "";
			this.mapping = "";
			this.path = "";
			this.format = "";
			this.serverType = null;
			this.valid = false;
      }

	  public ServerType getServerType(){
		  return serverType;
	  }
	  
      String getServer(){
          return server;
      }
      
      String getMapping(){
          return mapping;
      }
         
      String getPath(){
          return path;
      }
        
	  String getCapability(){
          return server + "/" + mapping + "/features";
      }

      String getType(){
        return server + "/" + mapping + "/" + path;
      }

	  String getFormat(){
		  return format;
	  }

	  boolean isValid(){
		  return valid;
	  }


}

