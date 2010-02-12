package com.affymetrix.igb.das;

import java.net.URL;

public final class DasType {
	private final URL serverURL;
  private final String type_id;
	private final String source;
	private final String name;

  DasType(URL serverURL, String id, String source, String name) {
		this.serverURL = serverURL;
    type_id = id;
		this.source = source;
		this.name = name;
  }

	public URL getServerURL() { return this.serverURL; }

  public String getID() { return type_id; }

	public String getSource() { return this.source; }

	public String getName() { return name == null ? type_id : name; }
}
