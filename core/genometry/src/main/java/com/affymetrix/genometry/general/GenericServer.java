package com.affymetrix.genometry.general;

import static com.affymetrix.genometry.general.GenericServerPrefKeys.ENABLE_IF_AVAILABLE;
import static com.affymetrix.genometry.general.GenericServerPrefKeys.IS_SERVER_ENABLED;
import static com.affymetrix.genometry.general.GenericServerPrefKeys.SERVER_LOGIN;
import static com.affymetrix.genometry.general.GenericServerPrefKeys.SERVER_NAME;
import static com.affymetrix.genometry.general.GenericServerPrefKeys.SERVER_PASSWORD;
import static com.affymetrix.genometry.general.GenericServerPrefKeys.SERVER_TYPE;
import static com.affymetrix.genometry.general.GenericServerPrefKeys.SERVER_URL;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.ServerStatus;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.ServerTypeI;
import com.affymetrix.genometry.util.StringEncrypter;
import com.affymetrix.genometry.util.StringEncrypter.EncryptionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import org.slf4j.LoggerFactory;

public final class GenericServer implements PreferenceChangeListener {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GenericServer.class);
    private final Preferences node;
    private String serverName;
    private String urlString;
    private String mirrorUrl; //qlmirror
    private ServerTypeI serverType;
    private String login = "";
    private String password = "";
    private boolean enabled = true;

    /**
     * Is this only a reference (no annotations) server?
     */
    //	private final boolean referenceOnly;
    /**
     * Das2ServerInfo, DasServerInfo, ..., QuickLoad?
     */
    private Object serverObj; //qlmirror
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
    private ServerStatus serverStatus = ServerStatus.NotInitialized;
    private final boolean isDefault;

    public GenericServer(String serverName, String urlString, ServerTypeI serverType,
            boolean enabled, Object serverObj, boolean isDefault, String mirrorURL) { //qlmirror
        this(
                serverName,
                urlString,
                serverType,
                enabled,
                serverType == null ? PreferenceUtils.getRepositoriesNode().node(getHash(urlString))
                        : serverType.isSaveServersInPrefs() ? PreferenceUtils.getServersNode().node(getHash(urlString)) : null,
                serverObj, isDefault, mirrorURL);
    }

    public GenericServer(Preferences node, Object serverObj,
            ServerTypeI serverType, boolean isDefault, String mirrorURL) { //qlmirror
        this(
                node.get(SERVER_NAME, "Unknown"),
                GeneralUtils.URLDecode(node.get(SERVER_URL, "")),
                serverType,
                true,
                node,
                serverObj, isDefault, mirrorURL);
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

    private GenericServer(String serverName, String urlString, ServerTypeI serverType,
            boolean enabled, Preferences node,
            Object serverObj, boolean isDefault, String mirrorURL) { //qlmirror
        this.serverName = serverName;
        this.urlString = urlString;
        this.mirrorUrl = mirrorURL; //qlmirror
        this.serverType = serverType;
        this.enabled = enabled;
        this.node = node;
        this.serverObj = serverObj;
        this.friendlyURL = urlString;
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

    public boolean isDefault() {
        return this.isDefault;
    }

    public String getFriendlyURL() {
        return getServerType().getFriendlyURL(this);
    }

    public boolean useMirrorSite() {
        return getServerType().useMirrorSite(this);
    }

    @Override
    public String toString() {
        return getServerName();
    }

    public void clean() {
        if (getServerType() != null) {
            getServerType().removeServer(this);
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

        switch (key) {
            case SERVER_NAME:
            case SERVER_TYPE:
                break;
            case SERVER_LOGIN:
                this.login = evt.getNewValue() == null ? "" : evt.getNewValue();
                break;
            case SERVER_PASSWORD:
                this.password = evt.getNewValue() == null ? "" : decrypt(evt.getNewValue());
                break;
            case IS_SERVER_ENABLED:
                this.enabled = evt.getNewValue() == null ? true : Boolean.valueOf(evt.getNewValue());
                break;
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

    public String getServerName() {
        return serverName;
    }

    public String getUrlString() {
        return urlString;
    }

    public URL getURL() {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return url;
    }

    public String getMirrorUrl() {
        return mirrorUrl;
    }

    public ServerTypeI getServerType() {
        return serverType;
    }

    public Object getServerObj() {
        return serverObj;
    }

    public void setServerObj(Object serverObj) {
        this.serverObj = serverObj;
    }
}
