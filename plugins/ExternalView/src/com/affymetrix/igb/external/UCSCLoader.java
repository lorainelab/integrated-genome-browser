package com.affymetrix.igb.external;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
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
		String base_url = getUrlForView(loc, pixWidth);
		String file_name_url = getImageUrl(base_url, UCSCView.UCSCUSERID + "=" + cookies.get(UCSCView.UCSCUSERID), new UCSCURLFinder(fileNameBaseUrl, fileNamePattern));
		if (file_name_url.startsWith("http")) {
			BufferedImage finalImage = null;
			BufferedImage fileImage = null;
			try {
				fileImage = ImageIO.read(new URL(file_name_url));
				finalImage = fileImage;
			} catch (IOException e) {
				Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "url was : " + file_name_url, e);
			}
			String side_name_url = getImageUrl(base_url, UCSCView.UCSCUSERID + "=" + cookies.get(UCSCView.UCSCUSERID), new UCSCURLFinder(sideNameBaseUrl, sideNamePattern));
			if(fileImage != null && side_name_url.startsWith("http")){
				BufferedImage sideImage = null;
				try {
					sideImage = ImageIO.read(new URL(side_name_url));
				} catch (IOException e) {
					Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "url was : " + side_name_url, e);
				}
				if(sideImage != null){
					BufferedImage tempImage = new BufferedImage(fileImage.getWidth(),  fileImage.getHeight(), fileImage.getType());
					Graphics graphics = tempImage.getGraphics();
					graphics.drawImage(sideImage, 0, 0, sideImage.getWidth(), sideImage.getHeight(), null);
					graphics.drawImage(fileImage, 0, 0, fileImage.getWidth(), fileImage.getHeight(), null);
					finalImage = tempImage;
				}
			}
			return new ImageError(finalImage,"");
		}
		
		return new ImageError(createErrorImage(file_name_url, pixWidth),"Error");
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
