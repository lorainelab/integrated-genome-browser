package com.affymetrix.igb.view.external;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Helper class for getting genomic images from ENSEMBL
 *
 * @author Ido M. Tamir
 */
class ENSEMBLoader extends BrowserLoader {

	private static final Map<String, String> dbmap = new HashMap<String, String>();
	private static final Pattern ucscPattern = Pattern.compile("db=(\\w+)&position=(\\w+):(\\d+)-(\\d+)");

	static {
		dbmap.put("mm8", "http://aug2007.archive.ensembl.org/Mus_musculus");
		dbmap.put("mm9", "http://sep2009.archive.ensembl.org/Mus_musculus");
		dbmap.put("hg19", "http://sep2009.archive.ensembl.org/Homo_sapiens");
		dbmap.put("hg18", "http://may2009.archive.ensembl.org/Homo_sapiens");
	}

	public static String getUrlForView(String query, int pixWidth) {
		if (query.startsWith("db")) {
			System.err.println(query);
			Matcher m = ucscPattern.matcher(query);
			if (m.find()) {
				String db = m.group(1);
				String chr = m.group(2);
				chr = chr.replaceAll("chr", "");
				String sstart = m.group(3);
				String send = m.group(4);
				if (!dbmap.containsKey(db)) {
					return "Error: could not transpose the genome " + db + " for ENSEMBL";
				}
				String url = dbmap.get(db) + "/Location/View?r=" + chr + ":" + sstart + "-" + send;
				Logger.getLogger(ENSEMBLoader.class.getName()).log(Level.FINE, "url was : " + url);
				return url;
			}
			return "Error: Could not translate UCSC query for ENSEMBL: " + query;
		} else {
			return query;
		}
	}

	/**
	 *
	 * @param query comes from the IGB as UCSC query string
	 * @param pixWidth
	 * @param cookies
	 * @return
	 */
	@Override
	public Image getImage(String query, int pixWidth, Map<String, String> cookies) {
		String url = getUrlForView(query, pixWidth);
		if (url.startsWith("http")) {
			String cookie = EnsemblView.ENSEMBLSESSION + "=" + cookies.get(EnsemblView.ENSEMBLSESSION) + ";" + EnsemblView.ENSEMBLWIDTH + "=" + cookies.get(EnsemblView.ENSEMBLWIDTH);
			url = getImageUrl(url, cookie, new ENSEMBLURLFinder());
			if (url.startsWith("http")) {
				try {
					return ImageIO.read(new URL(url));
				} catch (IOException e) {
					Logger.getLogger(BrowserView.class.getName()).log(Level.FINE, "url was : " + url, e);
				}
			}
		}
		return createErrorImage(url, pixWidth);
	}
}

class ENSEMBLURLFinder implements URLFinder {

	private final static Pattern panelPattern = Pattern.compile("id=\"ViewBottomPanel\"");
	private final static Pattern imagePattern = Pattern.compile("img-tmp(.*png)");

	public String findUrl(BufferedReader reader) throws IOException {
		String inputLine = "";
		boolean panel = false;
		while ((inputLine = reader.readLine()) != null) {
			if (!panel) {
				Matcher m = panelPattern.matcher(inputLine);
				panel = m.find();
			} else {
				Matcher m = imagePattern.matcher(inputLine);
				if (m.find()) {
					Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "found fileName " + inputLine);
					String fileName = m.group(1);
					return "http://www.ensembl.org/img-tmp" + fileName;
				}
			}
		}
		return "Error: could not find image URL in page";
	}
}
