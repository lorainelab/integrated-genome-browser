package com.affymetrix.igb.das;

import java.net.URL;

public final class DasType {
	private final URL serverURL;
  private final String type_id;
	private final String source;

  DasType(URL serverURL, String id, String source) {
		this.serverURL = serverURL;
    type_id = id;
		this.source = source;
  }

	public URL getServerURL() { return this.serverURL; }

  public String getID() { return type_id; }

	public String getSource() { return this.source; }

	public String getName() { return this.source + "/" + this.type_id; }
}
