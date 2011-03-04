package com.affymetrix.igb.util;

import com.affymetrix.igb.IGB;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

/**
 *
 * @author hiralv
 */
public class IGBUtils {

	/** Returns the icon stored in the jar file.
	 *  It is expected to be at com.affymetrix.igb
	 *  @return null if the image file is not found or can't be opened.
	 */
	public static Image getIcon(String name) {
		Image icon = null;
		try {
			URL url = IGB.class.getResource(name);
			if (url != null) {
				icon = Toolkit.getDefaultToolkit().getImage(url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// It isn't a big deal if we can't find the icon, just return null
		}
		return icon;
	}
}
