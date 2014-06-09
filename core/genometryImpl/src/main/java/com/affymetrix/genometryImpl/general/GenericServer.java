package com.affymetrix.genometryImpl.general;

import static com.affymetrix.genometryImpl.general.GenericServerPrefKeys.ENABLE_IF_AVAILABLE;
import static com.affymetrix.genometryImpl.general.GenericServerPrefKeys.IS_SERVER_ENABLED;
import static com.affymetrix.genometryImpl.general.GenericServerPrefKeys.SERVER_LOGIN;
import static com.affymetrix.genometryImpl.general.GenericServerPrefKeys.SERVER_NAME;
import static com.affymetrix.genometryImpl.general.GenericServerPrefKeys.SERVER_PASSWORD;
import static com.affymetrix.genometryImpl.general.GenericServerPrefKeys.SERVER_TYPE;
import static com.affymetrix.genometryImpl.general.GenericServerPrefKeys.SERVER_URL;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.StringEncrypter;
import com.affymetrix.genometryImpl.util.StringEncrypter.EncryptionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;

/**
 * A class that's useful for visualizing a generic server.
 *
 * @version $Id: GenericServer.java 10498 2012-02-28 15:49:34Z imnick $
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
     * Mirror site url
     */
    public String mirrorURL; //qlmirror
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
    public Object serverObj; //qlmirror
    /**
     * friendly SERVER_URL that users may look at.
     */
    private final String friendlyURL;
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
    private final boolean isDefault;

    public GenericServer(String serverName, String URL, ServerTypeI serverType,
            boolean enabled, Object serverObj, boolean primary, boolean isDefault, String mirrorURL) { //qlmirror
        this(
                serverName,
                URL,
                serverType,
                enabled,
                false,
                serverType == null ? PreferenceUtils.getRepositoriesNode().node(getHash(URL))
                : serverType.isSaveServersInPrefs() ? PreferenceUtils.getServersNode().node(getHash(URL)) : null,
                serverObj, primary, isDefault, mirrorURL);
    }

    public GenericServer(String serverName, String URL, ServerTypeI serverType,
            boolean enabled, Object serverObj, boolean isDefault, String mirrorURL) { //qlmirror
        this(
                serverName,
                URL,
                serverType,
                enabled,
                false,
                serverType == null ? PreferenceUtils.getRepositoriesNode().node(getHash(URL))
                : PreferenceUtils.getServersNode().node(getHash(URL)),
                serverObj, false, isDefault, mirrorURL);
    }

    public GenericServer(Preferences node, Object serverObj,
            ServerTypeI serverType, boolean isDefault, String mirrorURL) { //qlmirror
        this(
                node.get(SERVER_NAME, "Unknown"),
                GeneralUtils.URLDecode(node.get(SERVER_URL, "")),
                serverType,
                true,
                false,
                node,
                serverObj, false, isDefault, mirrorURL);
    }

    /**
     * returns the positive has of the string. For this class, the String is a
     * URI for a local or network path.
     *
     * @param str path on the network or local space
     * @return a hash that should be unique enough to create a Preference node
     * where the servers preferences can be stored.
     */
    public static String getHash(String str) {
        return Long.toString(((long) str.hashCode() + (long) Integer.MAX_VALUE));
    }

    private GenericServer(
            String serverName, String URL, ServerTypeI serverType,
            boolean enabled, boolean referenceOnly, Preferences node,
            Object serverObj, boolean primary, boolean isDefault, String mirrorURL) { //qlmirror
        this.serverName = serverName;
        this.URL = URL;
        this.mirrorURL = mirrorURL; //qlmirror
        this.serverType = serverType;
        this.enabled = enabled;
        this.node = node;
        this.serverObj = serverObj;
        this.friendlyURL = URL;
//		this.referenceOnly = referenceOnly;

        if (this.node != null) {
            if (this.node.getBoolean(ENABLE_IF_AVAILABLE, false)) {
                this.setEnabled(true);
            } else {
                this.setEnabled(this.node.getBoolean(IS_SERVER_ENABLED, enabled));
            }
            this.setLogin(this.node.get(SERVER_LOGIN, ""));
            this.setPassword(decrypt(this.node.get(SERVER_PASSWORD, "")));
            this.node.addPreferenceChangeListener(this);
        }
        this.primary = primary;
        this.isDefault = isDefault;
    }

    public ImageIcon getFriendlyIcon() {
        if (friendlyIcon == null && !friendlyIconAttempted) {
            if (this.friendlyURL != null) {
                friendlyIconAttempted = true;
                this.friendlyIcon = GeneralUtils.determineFriendlyIcon(
                        this.friendlyURL + "/favicon.ico");
            }
        }
        return friendlyIcon;
    }

    public void setServerStatus(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    public ServerStatus getServerStatus() {
        return this.serverStatus;
    }

    public void setEnabled(boolean enabled) {
        if (node != null) {
            node.putBoolean(IS_SERVER_ENABLED, enabled);
        }
        this.enabled = enabled;
    }

    public void setName(String name) {
        if (node != null) {
            node.put(SERVER_NAME, name);
        }
        this.serverName = name;
    }

    public void enableForSession() {
        this.enabled = true;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setLogin(String login) {
        if (node != null) {
            node.put(SERVER_LOGIN, login);
        }
        this.login = login;
    }

    public String getLogin() {
        return this.login;
    }

    public void setEncryptedPassword(String password) {
        if (node != null) {
            node.put(SERVER_PASSWORD, password);
        }
        this.password = decrypt(password);
    }

    public void setPassword(String password) {
        if (node != null) {
            node.put(SERVER_PASSWORD, encrypt(password));
        }
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean isPrimary() {
        return this.primary;
    }

    public boolean isDefault() {
        return this.isDefault;
    }

    public String getFriendlyURL() {
        return serverType.getFriendlyURL(this);
    }

    public boolean useMirrorSite() {
        return serverType.useMirrorSite(this);
    }

    @Override
    public String toString() {
        return serverName;
    }

    /**
     * Order by: enabled/disabled, then server name, then DAS2, DAS, Quickload.
     *
     * @param gServer
     * @return comparison integer
     */
    @Override
    public int compareTo(GenericServer gServer) {
        if (this.isEnabled() != gServer.isEnabled()) {
            return Boolean.valueOf(this.isEnabled()).compareTo(gServer.isEnabled());
        }
        if (!(this.serverName.equals(gServer.serverName))) {
            return this.serverName.compareTo(gServer.serverName);
        }
        return this.serverType.compareTo(gServer.serverType);
    }

    public void clean() {
        if (serverType != null) {
            serverType.removeServer(this);
        }
        setEnabled(false);
    }

    /**
     * React to modifications of the Java preferences. This should probably fire
     * an event notifying listeners that this generic server has changed.
     *
     * @param evt
     */
    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        final String key = evt.getKey();

        if (key.equals(SERVER_NAME) || key.equals(SERVER_TYPE)) {
            /*
             * Ignore
             */
        } else if (key.equals(SERVER_LOGIN)) {
            this.login = evt.getNewValue() == null ? "" : evt.getNewValue();
        } else if (key.equals(SERVER_PASSWORD)) {
            this.password = evt.getNewValue() == null ? "" : decrypt(evt.getNewValue());
        } else if (key.equals(IS_SERVER_ENABLED)) {
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
