package com.affymetrix.genometry.data;

import static com.affymetrix.genometry.general.DataProviderPrefKeys.LOAD_PRIORITY;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.LOGIN;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.MIRROR_URL;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PASSWORD;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PRIMARY_URL;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PROVIDER_NAME;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.STATUS;
import com.affymetrix.genometry.util.LoadUtils.ResourceStatus;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Disabled;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Initialized;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.NotInitialized;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.StringEncrypter;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public abstract class BaseDataProvider implements DataProvider {

    private static final Logger logger = LoggerFactory.getLogger(BaseDataProvider.class);
    private final Preferences preferencesNode;
    private String url;
    protected String mirrorUrl;
    protected String name;
    protected String login;
    protected String password;
    protected int loadPriority;
    protected ResourceStatus status;
    private StringEncrypter encrypter;
    protected boolean useMirror;

    public BaseDataProvider(String url, String name, int loadPriority) {
        this.url = checkNotNull(url);
        this.name = checkNotNull(name);
        this.loadPriority = loadPriority;
        encrypter = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME);
        preferencesNode = PreferenceUtils.getDataProviderNode(url);
        loadPersistedConfiguration();
        initializePreferences();
    }

    public BaseDataProvider(String url, String name, String mirrorUrl, int loadPriority) {
        this.url = checkNotNull(url);
        this.name = checkNotNull(name);
        this.mirrorUrl = checkNotNull(mirrorUrl);
        this.loadPriority = loadPriority;
        encrypter = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME);
        preferencesNode = PreferenceUtils.getDataProviderNode(url);
        loadPersistedConfiguration();
        initializePreferences();
    }

    private void loadPersistedConfiguration() {
        Optional.ofNullable(preferencesNode.get(PROVIDER_NAME, null)).ifPresent(preferenceValue -> name = preferenceValue);
        Optional.ofNullable(preferencesNode.get(LOAD_PRIORITY, null)).ifPresent(preferenceValue -> loadPriority = Integer.parseInt(preferenceValue));
        Optional.ofNullable(preferencesNode.get(MIRROR_URL, null)).ifPresent(preferenceValue -> mirrorUrl = preferenceValue);
        Optional.ofNullable(preferencesNode.get(LOGIN, null)).ifPresent(preferenceValue -> login = preferenceValue);
        Optional.ofNullable(preferencesNode.get(PASSWORD, null)).ifPresent(preferenceValue -> password = encrypter.decrypt(preferenceValue));
        Optional.ofNullable(preferencesNode.get(STATUS, null)).ifPresent(preferenceValue -> {
            getMatchingResourceStatus(preferenceValue).ifPresent(matchingStatus -> status = matchingStatus);
            if (status == Initialized) {
                status = NotInitialized;
            }
        });
    }

    private void initializePreferences() {
        preferencesNode.put(PRIMARY_URL, url);
        preferencesNode.put(PROVIDER_NAME, name);
        preferencesNode.putInt(LOAD_PRIORITY, loadPriority);
        if (!Strings.isNullOrEmpty(mirrorUrl)) {
            preferencesNode.put(MIRROR_URL, mirrorUrl);
        }
    }

    protected abstract void disable();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        preferencesNode.put(PROVIDER_NAME, name);
    }

    @Override
    public int getLoadPriority() {
        return loadPriority;
    }

    @Override
    public void setLoadPriority(int loadPriority) {
        this.loadPriority = loadPriority;
        preferencesNode.putInt(LOAD_PRIORITY, loadPriority);
    }

    @Override
    public String getUrl() {
        if (useMirror) {
            return mirrorUrl;
        } else {
            return url;
        }
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
        preferencesNode.put(PRIMARY_URL, url);
    }

    @Override
    public Optional<String> getMirrorUrl() {
        return Optional.ofNullable(mirrorUrl);
    }

    @Override
    public void setMirrorUrl(String mirrorUrl) {
        this.mirrorUrl = mirrorUrl;
        preferencesNode.put(MIRROR_URL, mirrorUrl);
    }

    @Override
    public boolean useMirrorUrl() {
        return useMirror;
    }

    @Override
    public Optional<String> getLogin() {
        return Optional.ofNullable(login);
    }

    @Override
    public void setLogin(String login) {
        this.login = login;
        preferencesNode.put(LOGIN, login);
    }

    @Override
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
        preferencesNode.put(PASSWORD, encrypter.encrypt(password));

    }

    @Override
    public ResourceStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(ResourceStatus status) {
        this.status = status;
        preferencesNode.put(STATUS, status.toString());
        if (status == NotInitialized) {
            initialize();
        }
        if (status == Disabled) {
            useMirror = false;
            disable();
        }
    }

    @Override
    public abstract Set<String> getSupportedGenomeVersionNames();

    private Optional<ResourceStatus> getMatchingResourceStatus(String preferenceValue) {
        return Arrays.asList(ResourceStatus.values()).stream()
                .filter(resourceStatus -> resourceStatus.toString().equals(preferenceValue))
                .findFirst();
    }

}
