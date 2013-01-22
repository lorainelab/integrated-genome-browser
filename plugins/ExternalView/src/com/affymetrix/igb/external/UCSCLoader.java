package com.affymetrix.igb.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Returns location image string from the UCSC genome browser.
 * 
 * 
 * @author Ido M. Tamir
 */
public class UCSCLoader extends BrowserLoader {

	private static final Pattern fileNamePattern = Pattern.compile("(hgt_genome.*png)");
	private static final Pattern sideNamePattern = Pattern.compile("(side_genome.*png)");
	
	private static final String fileNameBaseUrl = "http://genome.ucsc.edu/trash/hgt/";
	private static final String sideNameBaseUrl = "http://genome.ucsc.edu/trash/hgtSide/";
	
	public String getUrlForView(Loc loc, int pixWidth) {
		String width = "pix=" + pixWidth + "&";
		return "http://genome.ucsc.edu/cgi-bin/hgTracks?" + width +"db="+loc.db+"&position="+loc.chr+":"+loc.start+"-"+loc.end;
	}

	public ImageError getImage(Loc loc, int pixWidth, Map<String, String> cookies) {
		String url = getUrlForView(loc, pixWidth);
		url = getImageUrl(url, UCSCView.UCSCUSERID + "=" + cookies.get(UCSCView.UCSCUSERID), new UCSCURLFinder(fileNameBaseUrl, fileNamePattern));
		if (url.startsWith("http")) {
			try {
				return new ImageError(ImageIO.read(new URL(url)),"");
			} catch (IOException e) {
				Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "url was : " + url, e);
			}
		}
		return new ImageError(createErrorImage(url, pixWidth),"Error");
	}

	class UCSCURLFinder implements URLFinder {
		final String base_url;
		final Pattern pattern;
		
		UCSCURLFinder(String base_url, Pattern pattern){
			this.base_url = base_url;
			this.pattern = pattern;
		}
		
		public String findUrl(BufferedReader reader, URL redirectedUrl) throws IOException {
			String inputLine;
			while ((inputLine = reader.readLine()) != null) {
				Matcher m = pattern.matcher(inputLine);
				if (m.find() && m.groupCount() == 1) {
					Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "found fileName {0}", inputLine);
					String fileName = m.group(1);
					return  base_url + fileName;
				}
			}
			return MessageFormat.format(ExternalViewer.BUNDLE.getString("findImageURLError"), "");
		}
	}
}
