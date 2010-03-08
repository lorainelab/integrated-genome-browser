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

	private static final Map<String, EnsemblURL> dbmap = new HashMap<String, EnsemblURL>();
	

	static {
		dbmap.put("mm8", new EnsemblURL("http://aug2007.archive.ensembl.org/Mus_musculus","http://aug2007.archive.ensembl.org"));
		dbmap.put("mm9", new EnsemblURL("http://sep2009.archive.ensembl.org/Mus_musculus","http://sep2009.archive.ensembl.org"));
		dbmap.put("hg19",new EnsemblURL("http://sep2009.archive.ensembl.org/Homo_sapiens","http://sep2009.archive.ensembl.org"));
		dbmap.put("hg18",new EnsemblURL("http://may2009.archive.ensembl.org/Homo_sapiens","http://may2009.archive.ensembl.org"));
		dbmap.put("dm3", new EnsemblURL("http://sep2009.archive.ensembl.org/Drosophila_melanogaster","http://sep2009.archive.ensembl.org"));
	}

	public static String getUrlForView(Loc loc, int pixWidth) {
		if (!dbmap.containsKey(loc.db)) {
			return "Error: could not transpose the genome " + loc.db + " for ENSEMBL";
		}
		String url = dbmap.get(loc.db).url + "/Location/View?r=" + loc.chr + ":" + loc.start + "-" + loc.end;
		Logger.getLogger(ENSEMBLoader.class.getName()).log(Level.FINE, "url was : " + url);
		return url;
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
		String url = "";
		Loc loc = null;
		try{
			loc = new Loc(query);
			url = getUrlForView(loc, pixWidth);
		}
		catch(Exception e){
			url = "Error: Could not translate UCSC query for ENSEMBL: " + query;
		}
		if(url.startsWith("http")){
			String cookie = EnsemblView.ENSEMBLWIDTH + "=" + cookies.get(EnsemblView.ENSEMBLWIDTH);
			String session = cookies.get(EnsemblView.ENSEMBLSESSION);
			if(session != null && !session.equals("")){
				cookie += ";" + EnsemblView.ENSEMBLSESSION + "=" + cookies.get(EnsemblView.ENSEMBLSESSION);
			}
			url = getImageUrl(url, cookie, new ENSEMBLURLFinder(dbmap.get(loc.db).host));
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
	private final String url;
	public ENSEMBLURLFinder(String url){
		this.url = url;
	}

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
					return url + "/img-tmp" + fileName;
				}
			}
		}
		return "Error: could not find image URL in page";
	}
}

class EnsemblURL {
	final String url;
	final String host;
	EnsemblURL(String url, String host){
		this.url = url;
		this.host = host;
	}
}


class Loc {
	private static final Pattern ucscPattern = Pattern.compile("db=(\\w+)&position=(\\w+):(\\d+)-(\\d+)");
	final String db;
	final String chr;
	final String start;
	final String end;
	Loc(String location){
		Matcher m = ucscPattern.matcher(location);
		if (m.find()) {
			db = m.group(1);
			String chrs = m.group(2);
			chr = chrs.replaceAll("chr", "");
			start = m.group(3);
			end = m.group(4);
		}
		else{
			throw new RuntimeException("Could not parse location from UCSC");
		}
	}
}