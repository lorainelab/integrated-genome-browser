package com.affymetrix.igb.bookmarks;



final class GraphBookmark {
      private final String server;
      private final String mapping;
      private final String path;
	  private final String format;
      private final boolean valid;

      GraphBookmark(String server, String mapping, String path, String format){
			this.server = server;
			this.mapping = mapping;
			this.path = path;
			this.format = format;
			this.valid = true;
      }

	  GraphBookmark(){
			this.server = "";
			this.mapping = "";
			this.path = "";
			this.format = "";
			this.valid = false;
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

