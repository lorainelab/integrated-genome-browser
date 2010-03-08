package com.affymetrix.igb.view.external;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for browser loaders
 * returns image upon query with:
 * query(genome + region)
 * pixwidth (width of image)
 * cookies
 *
 * @author Ido M. Tamir
 */
public abstract class BrowserLoader {

	public Image createErrorImage(String error, int pixWidth) {
		final BufferedImage image = new BufferedImage(pixWidth, 70, BufferedImage.TYPE_3BYTE_BGR);
		image.createGraphics();
		final Graphics2D g = (Graphics2D) image.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, pixWidth, 70);
		final Font font = new Font("Serif", Font.PLAIN, 12);
		g.setFont(font);
		g.setColor(Color.BLACK);
		g.setFont(font);
		g.drawString(error, 30, 20);
		return image;
	}

	abstract public Image getImage(String query, int pixWidth, Map<String, String> cookies);

	/**
	 *
	 * @param url the UCSC genome/region url
	 * @param userId the UCSC userId (hguid cookie value)
	 * @return url of the image of the region
	 */
	public String getImageUrl(String url, String cookie, URLFinder urlfinder) {
		URL request_url = null;
		HttpURLConnection request_con = null;
		InputStream input_stream = null;
		BufferedReader in = null;
		try {
			request_url = new URL(url);
			request_con = (HttpURLConnection) request_url.openConnection();
			request_con.setConnectTimeout(120 * 1000); //2min
			request_con.setUseCaches(false);
			request_con.addRequestProperty("Cookie", cookie);
			input_stream = request_con.getInputStream();
			in = new BufferedReader(new InputStreamReader(input_stream));
			return urlfinder.findUrl(in);

		} catch (SocketException e) {
			Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, null, e);
			return ("Error: the UCSC Browser was not able to return the answer in the appropriate time");
		} catch (IOException e) {
			Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, null, e);
			return ("Error: " + e.getMessage());
		} finally {
			GeneralUtils.safeClose(input_stream);
			GeneralUtils.safeClose(in);
			if (request_con != null) {
				try {
					request_con.disconnect();
				} catch (Exception e) {
				}
			}
		}
	}
}
