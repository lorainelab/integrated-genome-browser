package com.affymetrix.genometry.data;

import static com.affymetrix.genometry.general.DataProviderPrefKeys.LOAD_PRIORITY;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.LOGIN;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.MIRROR_URL;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.NAME;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PASSWORD;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.STATUS;
import com.affymetrix.genometry.util.LoadUtils.ResourceStatus;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Initialized;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.NotInitialized;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.StringEncrypter;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 *
 * @author dcnorris
 */
public abstract class BaseDataProvider implements DataProvider {

    private static final int DEFAULT_LOAD_PRIORITY = -1;
    private final Preferences preferencesNode;
    protected final String url;
    protected String mirrorUrl;
    protected String name;
    protected String login;
    protected String password;
    protected int loadPriority;
    protected ResourceStatus status;
    private StringEncrypter encrypter;

    public BaseDataProvider(String url, String name) {
        this.url = url;
        this.name = name;
        loadPriority = DEFAULT_LOAD_PRIORITY;
        preferencesNode = PreferenceUtils.getDataProvidersNode().node(convertUrlToHash(url));
        loadPersistedConfiguration();
    }

    private void loadPersistedConfiguration() {
        Optional.ofNullable(preferencesNode.get(NAME, null)).ifPresent(preferenceValue -> name = preferenceValue);
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

    private String convertUrlToHash(String url) {
        HashFunction hf = Hashing.md5();
        HashCode hc = hf.newHasher().putString(url, Charsets.UTF_8).hash();
        return hc.toString();
    }

    protected abstract void initialize();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        preferencesNode.put(NAME, name);
    }

    @Override
    public int getLoadPriority() {
        return loadPriority;
    }

    @Override
    public void setLoadPriority(int loadPriority) {
        this.loadPriority = loadPriority;
        preferencesNode.put(LOAD_PRIORITY, Integer.toString(loadPriority));
    }

    @Override
    public String getUrl() {
        return url;
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

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
        preferencesNode.put(LOGIN, login);
    }

    public String getPassword() {
        return password;
    }

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
    }

    @Override
    public abstract Set<String> getSupportedGenomeVersionNames();

    private Optional<ResourceStatus> getMatchingResourceStatus(String preferenceValue) {
        return Arrays.asList(ResourceStatus.values()).stream()
                .filter(resourceStatus -> resourceStatus.toString().equals(preferenceValue))
                .findFirst();
    }

}
