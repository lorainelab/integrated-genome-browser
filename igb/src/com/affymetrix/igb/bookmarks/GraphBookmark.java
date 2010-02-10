package com.affymetrix.igb.bookmarks;



public class GraphBookmark {
      private final String server;
      private final String mapping;
      private final String path;
	  private final String format;
      private final boolean valid;

      public GraphBookmark(String server, String mapping, String path, String format){
			this.server = server;
			this.mapping = mapping;
			this.path = path;
			this.format = format;
			this.valid = true;
      }

	  public GraphBookmark(){
			this.server = "";
			this.mapping = "";
			this.path = "";
			this.format = "";
			this.valid = false;
      }
      
      public String getServer(){
          return server;
      }
      
      public String getMapping(){
          return mapping;
      }
         
      public String getPath(){
          return path;
      }
        
	  public String getCapability(){
          return server + "/" + mapping + "/features";
      }

      public String getType(){
        return server + "/" + mapping + "/" + path;
      }

	  public String getFormat(){
		  return format;
	  }

	  public boolean isValid(){
		  return valid;
	  }


}

