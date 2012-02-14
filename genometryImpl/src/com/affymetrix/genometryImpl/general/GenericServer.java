package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.genometryImpl.util.StringEncrypter;
import com.affymetrix.genometryImpl.util.StringEncrypter.EncryptionException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;

/**
 * A class that's useful for visualizing a generic server.
 *
 * @version $Id$
 */
public final class GenericServer implements Comparable<GenericServer>, PreferenceChangeListener {

	/**
	 * Stores this servers settings on Java Preferences
	 */
	public final Preferences node;
	/**
	 * Name of the server.
	 */
	public String serverName;
	/**
	 * URL/file that points to the server.
	 */
	public String URL;
	/**
	 * DAS, DAS2, QuickLoad, Unknown (local file)
	 */
	public ServerTypeI serverType;
	/**
	 * to be used by DAS/2 authentication
	 */
	private String login = "";
	/**
	 * to be used by DAS/2 authentication
	 */
	private String password = "";
	/**
	 * Is this server enabled?
	 */
	private boolean enabled = true;
	/**
	 * Is this only a reference (no annotations) server?
	 */
	//	private final boolean referenceOnly;				 
	/**
	 * Das2ServerInfo, DasServerInfo, ..., QuickLoad?
	 */
	public final Object serverObj;
	/**
	 * friendly URL that users may look at.
	 */
	public final URL friendlyURL;
	/**
	 * friendly icon that users may look at.
	 */
	private ImageIcon friendlyIcon = null;
	/**
	 * Don't keep on searching for friendlyIcon
	 */
	private boolean friendlyIconAttempted = false;
	/**
	 * Is this server initialized?
	 */
	private ServerStatus serverStatus = ServerStatus.NotInitialized;
	private final boolean primary;

	public GenericServer(String serverName, String URL, ServerTypeI serverType, boolean enabled, Object serverObj, boolean primary) {
		this(
				serverName,
				URL,
				serverType,
				enabled,
				false,
				serverType == null ? PreferenceUtils.getRepositoriesNode().node(getHash(URL))
				: PreferenceUtils.getServersNode().node(getHash(URL)),
				serverObj, primary);
	}

	public GenericServer(String serverName, String URL, ServerTypeI serverType, boolean enabled, Object serverObj) {
		this(
				serverName,
				URL,
				serverType,
				enabled,
				false,
				serverType == null ? PreferenceUtils.getRepositoriesNode().node(getHash(URL))
				: PreferenceUtils.getServersNode().node(getHash(URL)),
				serverObj, false);
	}

	public GenericServer(Preferences node, Object serverObj, ServerTypeI serverType) {
		this(
				node.get(GenericServerPref.NAME, "Unknown"),
				GeneralUtils.URLDecode(node.get(GenericServerPref.URL, "")),
				serverType,
				true,
				false,
				node,
				serverObj, false);
	}

	/**
	 * returns the positive has of the string.  For this class, the String is a 
	 * URI for a local or network path.
	 * @param str path on the network or local space
	 * @return a hash that should be unique enough to create a Preference node where the
	 *	servers preferences can be stored.
	 */
	public static String getHash(String str) {
		return Long.toString(((long) str.hashCode() + (long) Integer.MAX_VALUE));
	}

	private GenericServer(
			String serverName, String URL, ServerTypeI serverType, boolean enabled, boolean referenceOnly, Preferences node, Object serverObj, boolean primary) {
		this.serverName = serverName;
		this.URL = URL;
		this.serverType = serverType;
		this.enabled = enabled;
		this.node = node;
		this.serverObj = serverObj;
		this.friendlyURL = determineFriendlyURL(URL, serverType);
//		this.referenceOnly = referenceOnly;

		this.setEnabled(this.node.getBoolean(GenericServerPref.ENABLED, enabled));
		this.setLogin(this.node.get(GenericServerPref.LOGIN, ""));
		this.setPassword(decrypt(this.node.get(GenericServerPref.PASSWORD, "")));

		this.node.addPreferenceChangeListener(this);
		this.primary = primary;
	}

	public ImageIcon getFriendlyIcon() {
		if (friendlyIcon == null && !friendlyIconAttempted) {
			if (this.friendlyURL != null) {
				friendlyIconAttempted = true;
				this.friendlyIcon = GeneralUtils.determineFriendlyIcon(
						this.friendlyURL.toString() + "/favicon.ico");
			}
		}
		return friendlyIcon;
	}

	private static URL determineFriendlyURL(String URL, ServerTypeI serverType) {
		if (URL == null) {
			return null;
		}
		String tempURL = URL;
		URL tempFriendlyURL = null;
		if (tempURL.endsWith("/")) {
			tempURL = tempURL.substring(0, tempURL.length() - 1);
		}
		if (serverType != null) {
			tempURL = serverType.adjustURL(tempURL);
		}
		try {
			tempFriendlyURL = new URL(tempURL);
		} catch (Exception ex) {
			// Ignore an exception here, since this is only for making a pretty UI.
		}
		return tempFriendlyURL;
	}

	public void setServerStatus(ServerStatus serverStatus) {
		this.serverStatus = serverStatus;
	}

	public ServerStatus getServerStatus() {
		return this.serverStatus;
	}

	public void setEnabled(boolean enabled) {
		node.putBoolean(GenericServerPref.ENABLED, enabled);
		this.enabled = enabled;
	}

	public void setName(String name) {
		node.put(GenericServerPref.NAME, name);
		this.serverName = name;
	}

//	public void setServerType(String type) {
//			node.put(GenericServerPref.TYPE, type);
//		for (ServerTypeI serverTypeI : ServerUtils.getServerTypes()) {
//			if (type.equalsIgnoreCase(serverTypeI.getName())) {
//				this.serverType = serverTypeI;
//			}
//		}
//	}

	public void enableForSession() {
		this.enabled = true;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setLogin(String login) {
		node.put(GenericServerPref.LOGIN, login);
		this.login = login;
	}

	public String getLogin() {
		return this.login;
	}

	public void setEncryptedPassword(String password) {
		node.put(GenericServerPref.PASSWORD, password);
		this.password = decrypt(password);
	}

	public void setPassword(String password) {
		node.put(GenericServerPref.PASSWORD, encrypt(password));
		this.password = password;
	}

	public String getPassword() {
		return this.password;
	}

	public boolean isPrimary() {
		return this.primary;
	}

	@Override
	public String toString() {
		return serverName;
	}

	/**
	 * Order by:
	 * enabled/disabled,
	 * then server name,
	 * then DAS2, DAS, Quickload.
	 * @param gServer
	 * @return comparison integer
	 */
	public int compareTo(GenericServer gServer) {
		if (this.isEnabled() != gServer.isEnabled()) {
			return Boolean.valueOf(this.isEnabled()).compareTo(Boolean.valueOf(gServer.isEnabled()));
		}
		if (!(this.serverName.equals(gServer.serverName))) {
			return this.serverName.compareTo(gServer.serverName);
		}
		return this.serverType.compareTo(gServer.serverType);
	}

	/**
	 * React to modifications of the Java preferences.  This should probably
	 * fire an event notifying listeners that this generic server has changed.
	 *
	 * @param evt
	 */
	public void preferenceChange(PreferenceChangeEvent evt) {
		final String key = evt.getKey();

		if (key.equals(GenericServerPref.NAME) || key.equals(GenericServerPref.TYPE)) {
			/* Ignore */
		} else if (key.equals(GenericServerPref.LOGIN)) {
			this.login = evt.getNewValue() == null ? "" : evt.getNewValue();
		} else if (key.equals(GenericServerPref.PASSWORD)) {
			this.password = evt.getNewValue() == null ? "" : decrypt(evt.getNewValue());
		} else if (key.equals(GenericServerPref.ENABLED)) {
			this.enabled = evt.getNewValue() == null ? true : Boolean.valueOf(evt.getNewValue());
		}
	}

	/**
	 * Decrypt the given password.
	 *
	 * @param encrypted encrypted representation of the password
	 * @return string representation of the password
	 */
	private static String decrypt(String encrypted) {
		if (!encrypted.isEmpty()) {
			try {
				StringEncrypter encrypter = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME);
				return encrypter.decrypt(encrypted);
			} catch (EncryptionException ex) {
				Logger.getLogger(GenericServer.class.getName()).log(Level.SEVERE, null, ex);
				throw new IllegalArgumentException(ex);
			}
		}
		return "";
	}

	/**
	 * Encrypt the given password.
	 *
	 * @param password unencrypted password string
	 * @return the encrypted representation of the password
	 */
	private static String encrypt(String password) {
		if (!password.isEmpty()) {
			try {
				StringEncrypter encrypter = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME);
				return encrypter.encrypt(password);
			} catch (Exception ex) {
				Logger.getLogger(GenericServer.class.getName()).log(Level.SEVERE, null, ex);
				throw new IllegalArgumentException(ex);
			}
		}
		return "";
	}
}
