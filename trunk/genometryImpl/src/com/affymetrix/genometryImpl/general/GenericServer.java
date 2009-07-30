package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometry.util.LoadUtils.ServerType;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.swing.ImageIcon;
import net.sf.image4j.codec.ico.ICODecoder;
import net.sf.image4j.codec.ico.ICOImage;

/**
 * A class that's useful for visualizing a generic server.
 */
public final class GenericServer {

	public String serverName;   // name of the server.
	public String URL;          // URL/file that points to the server.
	public ServerType serverType;
	public String login;				// Defaults to ""
	public String password;			// Defaults to ""
	public boolean enabled;			// Is this server enabled?
	public final Object serverObj;    // Das2ServerInfo, DasServerInfo, ..., QuickLoad?
	public final URL friendlyURL;			// friendly URL that users may look at.
	public ImageIcon friendlyIcon;		// friendly icon that users may look at.
	public int  loginAttempts = 0;

	/**
	 * @param serverName
	 * @param URL
	 * @param serverType
	 * @param serverObj
	 */
	public GenericServer(String serverName, String URL, ServerType serverType, Object serverObj) {
		this(serverName, URL, serverType, true, "", "", serverObj);
	}

	public GenericServer(String serverName, String URL, ServerType serverType, boolean enabled, String login, String password, Object serverObj) {
		this.serverName = serverName;
		this.URL = URL;
		this.serverType = serverType;
		this.serverObj = serverObj;
		this.enabled = enabled;
		this.login = login;				// to be used by DAS/2 authentication
		this.password = password;			// to be used by DAS/2 authentication

		this.friendlyURL = determineFriendlyURL(URL, serverType);

		this.friendlyIcon = determineFriendlyIcon(this.friendlyURL);
	}

	@Override
	public String toString() {
		return serverName;
	}

	private static URL determineFriendlyURL(String URL, ServerType serverType) {
		if (URL == null) {
			return null;
		}
		String tempURL = URL;
		URL tempFriendlyURL = null;
		if (tempURL.endsWith("/")) {
			tempURL = tempURL.substring(0, tempURL.length() - 1);
		}
		if (serverType.equals(ServerType.DAS)) {
			if (tempURL.endsWith("/dsn")) {
				tempURL = tempURL.substring(0, tempURL.length() - 4);
			}
		} else if (serverType.equals(ServerType.DAS2)) {
			// Remove the last section (e.g., "/genome" or "/das2")
			tempURL = tempURL.substring(0, tempURL.lastIndexOf("/"));
		}
		try {
			tempFriendlyURL = new URL(tempURL);
		} catch (Exception ex) {
			// Ignore an exception here, since this is only for making a pretty UI.
		}
		return tempFriendlyURL;
	}

	private static ImageIcon determineFriendlyIcon(URL friendlyURL) {
		if (friendlyURL == null) {
			return null;
		}

		// Step 1. getting IconURL
		URL iconURL = null;
		try {
			String iconString = friendlyURL.toString() + "/favicon.ico";
			iconURL = new URL(iconString);
		} catch (Exception ex) {
			// Ignore an exception here, since this is only for making a pretty UI.
		}
		if (iconURL == null) {
			return null;
		}

		// Step 2. loading the icon and find a proper icon
		InputStream iconStream = null;
		BufferedImage icon = null;
		try {
			iconStream = iconURL.openStream();
			if (iconStream == null) {
				return null;
			}
			List<ICOImage> icoImages = ICODecoder.readExt(iconStream);
			int maxColorDepth = 0;
			for (ICOImage icoImage : icoImages) {
				int colorDepth = icoImage.getColourDepth();
				int width = icoImage.getWidth();
				if (width == 16 && maxColorDepth < colorDepth) {
					icon = icoImage.getImage();
					maxColorDepth = colorDepth;
				}
			}
			if (icon == null && !icoImages.isEmpty()) {
				icon = icoImages.get(0).getImage();
			}

		} catch (IOException ex) {
			return null;
		} finally {
			if (iconStream != null) {
				try {
					iconStream.close();
				} catch (IOException ex) {
					// Ignore an exception here, since this is only for making a pretty UI.
				}
			}
		}

		// step 3. create the imageIcon instance
		ImageIcon friendlyIcon = null;
		try {
			if (icon != null) {
				friendlyIcon = new ImageIcon(icon);
			}
		} catch (Exception ex) {
			// Ignore an exception here, since this is only for making a pretty UI.
		}
		return friendlyIcon;
	}
}
