package com.affymetrix.igb.update;

import java.util.Date;
import org.osgi.framework.Version;

/**
 *
 * @author hiralv
 */
public class Update {
	public static String UPDATE_PREFIX = "Update_shown_";
	private final Version version;
	private final String link;
	private final Date release_date;
	
	public Update(Version version, Date release_date, String link) {
		this.version = version;
		this.release_date = release_date;
		this.link = link;
	}
	
	public Version getVersion(){
		return version;
	}
	
	public Date getReleaseDate(){
		return release_date;
	}
	
	public String getLink(){
		return link;
	}
}
