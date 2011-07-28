package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
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

	private final Preferences node;
	public final String serverName;						// name of the server.
	public final String URL;							// URL/file that points to the server.
	public final ServerType serverType;					// DAS, DAS2, QuickLoad, Unknown (local file)
	private String login = "";							// to be used by DAS/2 authentication
	private String password = "";						// to be used by DAS/2 authentication
	private boolean enabled = true;						// Is this server enabled?
	private final boolean referenceOnly;				// Is this only a reference (no annotations) server?
	public final Object serverObj;						// Das2ServerInfo, DasServerInfo, ..., QuickLoad?
	public final URL friendlyURL;						// friendly URL that users may look at.
	private ImageIcon friendlyIcon = null;				// friendly icon that users may look at.
	private boolean friendlyIconAttempted = false;		// Don't keep on searching for friendlyIcon
	private ServerStatus serverStatus = 
			ServerStatus.NotInitialized;				// Is this server initialized?
	private final boolean primary;

	public GenericServer(String serverName, String URL, ServerType serverType, boolean enabled, Object serverObj, Preferences node, boolean primary) {
		this(
				serverName,
				URL,
				serverType,
				enabled,
				false,
				node,
				serverObj, primary);
	}

	public GenericServer(String serverName, String URL, ServerType serverType, boolean enabled, Object serverObj, Preferences node) {
		this(
				serverName,
				URL,
				serverType,
				enabled,
				false,
				node,
				serverObj, false);
	}

	public GenericServer(Preferences node, Object serverObj, ServerType serverType) {
		this(
				node.get("name", "Unknown"),
				GeneralUtils.URLDecode(node.name()),
				serverType,
				true,
				false,
				node,
				serverObj, false);
	}

	private GenericServer(
			String serverName, String URL, ServerType serverType, boolean enabled, boolean referenceOnly, Preferences node, Object serverObj, boolean primary) {
		this.serverName = serverName;
		this.URL = URL;
		this.serverType = serverType;
		this.enabled = enabled;
		this.node = node == null ? PreferenceUtils.getServersNode().node(GeneralUtils.URLEncode(URL)) : node;
		this.serverObj = serverObj;
		this.friendlyURL = determineFriendlyURL(URL, serverType);
		this.referenceOnly = referenceOnly;

		this.setEnabled(this.node.getBoolean("enabled", enabled));
		this.setLogin(this.node.get("login", ""));
		this.setPassword(decrypt(this.node.get("password", "")));

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

	private static URL determineFriendlyURL(String URL, ServerType serverType) {
		if (URL == null) {
			return null;
		}
		String tempURL = URL;
		URL tempFriendlyURL = null;
		if (tempURL.endsWith("/")) {
			tempURL = tempURL.substring(0, tempURL.length() - 1);
		}
		if (ServerType.DAS.equals(serverType)) {
			if (tempURL.endsWith("/dsn")) {
				tempURL = tempURL.substring(0, tempURL.length() - 4);
			}
		} else if (ServerType.DAS2.equals(serverType)) {
			if (tempURL.endsWith("/genome")) {
				tempURL = tempURL.substring(0, tempURL.length() - 7);
			} 
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
		node.putBoolean("enabled", enabled);
		this.enabled = enabled;
	}

	public void enableForSession(){
		this.enabled = true;
	}
	
	public boolean isEnabled() {
		return this.enabled;
	}

	public void setLogin(String login) {
		node.put("login", login);
		this.login = login;
	}

	public String getLogin() {
		return this.login;
	}

	public void setEncryptedPassword(String password) {
		node.put("password", password);
		this.password = decrypt(password);
	}

	public void setPassword(String password) {
		node.put("password", encrypt(password));
		this.password = password;
	}

	public String getPassword() {
		return this.password;
	}

	public boolean isPrimary(){
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

		if (key.equals("name") || key.equals("type")) {
			/* Ignore */
		} else if (key.equals("login")) {
			this.login = evt.getNewValue() == null ? "" : evt.getNewValue();
		} else if (key.equals("password")) {
			this.password = evt.getNewValue() == null ? "" : decrypt(evt.getNewValue());
		} else if (key.equals("enabled")) {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((URL == null) ? 0 : URL.hashCode());
		result = prime * result
				+ ((serverName == null) ? 0 : serverName.hashCode());
		result = prime * result
				+ ((serverType == null) ? 0 : serverType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericServer other = (GenericServer) obj;
		if (URL == null) {
			if (other.URL != null)
				return false;
		} else if (!URL.equals(other.URL))
			return false;
		if (serverName == null) {
			if (other.serverName != null)
				return false;
		} else if (!serverName.equals(other.serverName))
			return false;
		if (serverType != other.serverType)
			return false;
		return true;
	}
}
