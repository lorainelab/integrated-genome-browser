/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.cache.disk;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.ModalUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.lorainelab.cache.api.CacheStatus;
import com.lorainelab.cache.api.RemoteFileCacheService;
import com.lorainelab.cache.configuration.panel.CacheConfigurationPanel;
import com.lorainelab.igb.services.IgbService;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
@Component(provide = RemoteFileCacheService.class)
public class RemoteFileDiskCacheService implements RemoteFileCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteFileDiskCacheService.class);

    //TODO: Move to properties
    public static String DATA_DIR;
    public static final int FILENAME_SIZE = 100;
    public static final String FILENAME_EXT = "dat";
    public static final String FILENAME_TEMP_EXT = "tmp";
    public static final String FILENAME = "data";
    public static final BigInteger DEFAULT_FILESIZE_MIN_BYTES = BigInteger.ZERO;
    public static final BigInteger DEFAULT_MAX_CACHE_SIZE_MB = new BigInteger("2048");
    public static final BigInteger DEFAULT_CACHE_EXPIRE_MINUTES = new BigInteger("1440");
    public static final boolean CACHE_ENABLED = true;
    private volatile BigInteger currentCacheSize;
    private final Preferences cachePrefsNode;
    private final Preferences cacheRequestNode;
    private volatile Collection<String> backgroundCaching;
    private IgbService igbService;
    private volatile boolean isPromptingUser;
    private volatile boolean delayPrompt;
    private volatile Date lastPrompt;

    public RemoteFileDiskCacheService() {
        cachePrefsNode = PreferenceUtils.getCachePrefsNode();
        cacheRequestNode = PreferenceUtils.getCacheRequestNode();
        backgroundCaching = Sets.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        isPromptingUser = false;
    }

    @Activate
    public void activate() {
        DATA_DIR = CommonUtils.getInstance().getAppDataDirectory() + "fileCache" + File.separator;
        try {
            FileUtils.forceMkdir(new File(DATA_DIR));
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        setCurrentCacheSize(getCacheSizeInMB());
        enforceEvictionPolicies();
    }

    private enum CacheConfig {

        MAX_CACHE_SIZE_MB("max.cache.size.mb"),
        FILESIZE_MIN_BYTES("min.filesize.bytes"),
        CACHE_EXPIRE_MINUTES("cache.expire.min"),
        CACHE_ENABLED("cache.enabled");

        private String key;

        private CacheConfig(String key) {
            this.key = key;
        }

    }

    private boolean promptUserAboutCacheSize(BigInteger requestSizeInMB) {
        JPanel parentPanel = new JPanel(new MigLayout(new LC().maxWidth("200").width("200")));
        JPanel textPanel = new JPanel(new MigLayout("", "[][][][]", "[][]"));
        JPanel inputPanel = new JPanel(new MigLayout());

        BigInteger incrementSizeInMB = new BigInteger("1024");
        if (incrementSizeInMB.compareTo(requestSizeInMB) == -1) {
            incrementSizeInMB = requestSizeInMB.add(requestSizeInMB.mod(new BigInteger("1024")));
        }
        Integer value = incrementSizeInMB.intValue();
        Integer min = incrementSizeInMB.intValue();
        Integer step = 1;
        SpinnerNumberModel model = new SpinnerNumberModel(value, min, null, step);
        JSpinner incrementBy = new JSpinner(model);
        textPanel.add(new JLabel("You have reached the maximum allowed space for caching.  "
                + "Would you like to increase the max cache size?  Alternatively, you can manage the cache manually."));

        inputPanel.add(new JLabel("Increment By:"), "left");
        inputPanel.add(incrementBy, "width :75:");
        inputPanel.add(new JLabel("MB"), "left");

        parentPanel.add(textPanel, "wrap");
        parentPanel.add(inputPanel);
        final JComponent[] inputs = new JComponent[]{
            parentPanel
        };
        Object[] options = {"Yes",
            "No",
            "Disable All Caching",
            "Manage Cache"};

        int optionChosen = JOptionPane.showOptionDialog(null, inputs, "Cache Size Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        switch (optionChosen) {
            case 0:
                try {
                    setMaxCacheSizeMB(getMaxCacheSizeMB().add(new BigInteger(incrementBy.getValue().toString())));
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    setMaxCacheSizeMB(getMaxCacheSizeMB().add(incrementSizeInMB));
                }
                return true;
            case 1:
                return false;
            case 2:
                setCacheEnabled(false);
                return false;
            case 3:
                delayPrompt = true;
                lastPrompt = new Date();
                igbService.openPreferencesPanelTab(CacheConfigurationPanel.class);
                return false;
            default:
                return false;
        }
    }

    private void cacheInBackground(URL url, String path) {
        CThreadWorker< Void, Void> worker = new CThreadWorker<Void, Void>("caching remote file") {
            @Override
            protected Void runInBackground() {
                if (!getCacheEnabled()) {
                    return null;
                }
                if (backgroundCaching.add(url.toString())) {
                    try {
                        HttpHeader httpHeader = getHttpHeadersOnly(url.toString());
                        BigInteger requestSizeInMB = BigInteger.valueOf(httpHeader.getSize()).divide(new BigInteger("1000000"));
                        boolean doDownload = true;
                        if ((getCurrentCacheSize().add(requestSizeInMB)).compareTo(getMaxCacheSizeMB()) >= 0) {

                            synchronized (RemoteFileDiskCacheService.this) {
                                Date now = new Date();
                                if (isPromptingUser) {
                                    return null;
                                }
                                if (delayPrompt && (lastPrompt.getTime() < (now.getTime() - 60000))) {
                                    delayPrompt = false;
                                } else if (delayPrompt) {
                                    return null;
                                }
                                isPromptingUser = true;
                            }
                            doDownload = promptUserAboutCacheSize(requestSizeInMB);
                            isPromptingUser = false;
                        }
                        if (doDownload) {
                            tryDownload(url, path);
                        }
                    } finally {
                        backgroundCaching.remove(url.toString());
                    }
                }
                return null;
            }

            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance().execute(this, worker);
    }

    @Override
    public void promptToCacheInBackground(URL url) {
        if (getCacheEnabled()) {
            boolean confirm = ModalUtils.confirmPanel("Would you like to cache this sequence data file for this "
                    + "genome version for faster access in future requests?");
            if (confirm) {
                String path = getCacheFolderPath(generateKeyFromUrl(url));
                cacheInBackground(url, path);
            }
        }
    }

    @Override
    public boolean isCachingInBackground(URL url) {
        return backgroundCaching.contains(url.toString());
    }

    private void setCurrentCacheSize(BigInteger currentCacheSize) {
        if (currentCacheSize.compareTo(BigInteger.ZERO) == 1) {
            this.currentCacheSize = currentCacheSize;
        } else {
            this.currentCacheSize = BigInteger.ZERO;
        }
    }

    private BigInteger getCurrentCacheSize() {
        return currentCacheSize;
    }

    @Override
    public BigInteger getMaxCacheSizeMB() {
        return new BigInteger(cachePrefsNode.get(CacheConfig.MAX_CACHE_SIZE_MB.key, DEFAULT_MAX_CACHE_SIZE_MB.toString()));
    }

    @Override
    public void setMaxCacheSizeMB(BigInteger value) {
        cachePrefsNode.put(CacheConfig.MAX_CACHE_SIZE_MB.key, value.toString());
    }

    @Override
    public BigInteger getMinFileSizeBytes() {
        return new BigInteger(cachePrefsNode.get(CacheConfig.FILESIZE_MIN_BYTES.key, DEFAULT_FILESIZE_MIN_BYTES.toString()));
    }

    @Override
    public BigInteger getCacheExpireMin() {
        return new BigInteger(cachePrefsNode.get(CacheConfig.CACHE_EXPIRE_MINUTES.key, DEFAULT_CACHE_EXPIRE_MINUTES.toString()));
    }

    @Override
    public void setMinFileSizeBytes(BigInteger value) {
        cachePrefsNode.put(CacheConfig.FILESIZE_MIN_BYTES.key, value.toString());
    }

    @Override
    public void setCacheExpireMin(BigInteger value) {
        cachePrefsNode.put(CacheConfig.CACHE_EXPIRE_MINUTES.key, value.toString());
    }

    @Override
    public boolean getCacheEnabled() {
        return cachePrefsNode.getBoolean(CacheConfig.CACHE_ENABLED.key, CACHE_ENABLED);
    }

    @Override
    public void setCacheEnabled(boolean value) {
        cachePrefsNode.putBoolean(CacheConfig.CACHE_ENABLED.key, value);
    }

    @Override
    public Optional<InputStream> getFilebyUrl(URL url) {

        if (!getCacheEnabled()) {
            return Optional.empty();
        }

        String path = getCacheFolderPath(generateKeyFromUrl(url));
        HttpHeader httpHeader = getHttpHeadersOnly(url.toString());
        if (httpHeader.getSize() < getMinFileSizeBytes().longValue()) {
            try {
                return Optional.ofNullable(url.openStream());
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
                return Optional.empty();
            }
        }
        CacheStatus cacheStatus = getCacheStatus(path);
        if (isCacheValid(cacheStatus, httpHeader, url)) {
            try {
                updateLastRequestDate(url);
                return Optional.ofNullable(new FileInputStream(cacheStatus.getData()));
            } catch (FileNotFoundException ex) {
                LOG.error(ex.getMessage(), ex);
                return Optional.empty();
            }
        }
        cacheInBackground(url, path);
        return Optional.empty();
    }

    private void updateLastRequestDate(URL url) {
        cacheRequestNode.putLong(PreferenceUtils.shortNodeName(url.toString(), false), new Date().getTime());
    }

    @Override
    public Date getLastRequestDate(URL url) {
        return new Date(cacheRequestNode.getLong(PreferenceUtils.shortNodeName(url.toString(), false), new Date().getTime()));
    }

    private boolean tryDownload(URL url, String path) {
        String basePathToDataFile = path + FILENAME;
        String pathToDataFile = basePathToDataFile + "." + FILENAME_EXT;
        File finalFile = new File(pathToDataFile);
        File lockFile = new File(pathToDataFile + ".lock");
        String uuid = UUID.randomUUID().toString();
        File tmpFile = new File(basePathToDataFile + "_" + uuid + "." + FILENAME_TEMP_EXT);
        File md5File = new File(basePathToDataFile + ".md5");
        File lastModifiedFile = new File(basePathToDataFile + ".lastModified");
        File cacheLastUpdateFile = new File(basePathToDataFile + ".cacheLastUpdate");
        File etagFile = new File(basePathToDataFile + ".etag");
        File urlFile = new File(basePathToDataFile + ".url");

        try {
            FileUtils.forceMkdir(new File(path));
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
            return false;
        }
        synchronized (this) {
            try {
                if (finalFile.exists() || tmpFile.exists() || !lockFile.createNewFile()) {
                    LOG.warn("Aborting download since it may already by in progress on another thread");
                    return false;
                }
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
                return false;
            }
        }

        try (
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
                BufferedInputStream bis = new BufferedInputStream(url.openStream());) {

            String md5 = url.openConnection().getHeaderField("Content-MD5");
            if (!Strings.isNullOrEmpty(md5)) {
                md5 = md5.replaceAll("\"", "");
            }
            long lastModified = url.openConnection().getLastModified();
            String etag = url.openConnection().getHeaderField("ETag");
            if (!Strings.isNullOrEmpty(etag)) {
                etag = etag.replaceAll("\"", "");
            }
            FileUtils.writeStringToFile(md5File, md5);
            FileUtils.writeStringToFile(lastModifiedFile, Long.toString(lastModified));
            Date now = new Date();
            FileUtils.writeStringToFile(cacheLastUpdateFile, Long.toString(now.getTime()));
            FileUtils.writeStringToFile(etagFile, etag);
            FileUtils.writeStringToFile(urlFile, url.toString());

            IOUtils.copy(bis, bos);
            bos.flush();

            if (!Strings.isNullOrEmpty(md5) && verifyFile(md5, tmpFile)) {

                FileUtils.moveFile(tmpFile, finalFile);
                FileUtils.deleteQuietly(lockFile);
                synchronized (this) {
                    setCurrentCacheSize(getCurrentCacheSize().add(getCacheEntrySizeInMB(finalFile)));
                }
                return true;
            } else if (Strings.isNullOrEmpty(md5)) {
                FileUtils.moveFile(tmpFile, finalFile);
                FileUtils.deleteQuietly(lockFile);
                synchronized (this) {
                    setCurrentCacheSize(getCurrentCacheSize().add(getCacheEntrySizeInMB(finalFile)));
                }
                return true;
            }
        } catch (Exception e) {
            LOG.error("Error downloading: " + e.getMessage(), e);
            FileUtils.deleteQuietly(lockFile);
            FileUtils.deleteQuietly(finalFile);
            FileUtils.deleteQuietly(tmpFile);
        }
        return false;
    }

    private boolean verifyFile(String md5fromHeader, File file) {
        try {
            String md5Calculated = convertByteArrayToHexString(
                    DigestUtils.md5(new FileInputStream(file)));
            if (md5Calculated.equals(md5fromHeader)) {
                LOG.debug("Correct hash - original(" + md5fromHeader + ") - downloaded(" + md5Calculated + ")");
                return true;
            } else {
                LOG.error("Incorrect hash - original(" + md5fromHeader + ") - downloaded(" + md5Calculated + ")");
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error calculating hash: " + e.getMessage(), e);
            return false;
        }
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrayBytes.length; i++) {
            sb.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    private void cleanupCache(String path) {
        CacheStatus cacheStatus = getCacheStatus(path);
        if (cacheStatus.isDataExists()) {
            synchronized (this) {
                setCurrentCacheSize(getCurrentCacheSize().subtract(getCacheEntrySizeInMB(cacheStatus.getData())));
            }
        }
        FileUtils.deleteQuietly(new File(path));
    }

    private String generateKeyFromUrl(URL url) {
        return url.getHost() + url.getFile();
    }

    private String getCacheFolderPath(String key) {
        //TODO: Possible hash instead
        //byte[] sha256 = DigestUtils.sha256(key);
        String base64Key = Base64.encodeBase64String(key.getBytes());
        List<String> folders = new ArrayList<>();
        int index = 0;
        while (index < base64Key.length()) {
            folders.add(base64Key.substring(index, Math.min(index + FILENAME_SIZE, base64Key.length())));
            index += FILENAME_SIZE;
        }
        StringBuilder path = new StringBuilder(DATA_DIR);
        for (String folder : folders) {
            path.append(folder).append(File.separator);
        }
        path.append("cache").append(File.separator);
        return path.toString();
    }

    private boolean isCacheValid(CacheStatus cacheStatus, HttpHeader httpHeader, URL url) {
        if (cacheStatus.isDataExists()) {
            if (httpHeader.responseCode >= 400) {
                //If remote file is unavailable
                return true;
            }
            //TODO:Mark invalid cache for deletion
            if (!Strings.isNullOrEmpty(httpHeader.getMd5())
                    && !Strings.isNullOrEmpty(cacheStatus.getMd5())) {
                if (cacheStatus.getMd5().equals(httpHeader.getMd5())) {
                    return true;
                }
                clearCacheByUrl(url);
                return false;
            } else if (httpHeader.getLastModified() > 0
                    && cacheStatus.getLastModified() > 0) {
                if (httpHeader.getLastModified() == cacheStatus.getLastModified()) {
                    return true;
                }
                clearCacheByUrl(url);
                return false;
            }
        }
        return false;
    }

    private CacheStatus getCacheStatus(String path) {
        CacheStatus cacheStatus = new CacheStatus();
        String pathToDataFile = path + FILENAME;
        File data = new File(pathToDataFile + "." + FILENAME_EXT);
        File md5 = new File(pathToDataFile + ".md5");
        File lastModified = new File(pathToDataFile + ".lastModified");
        File cacheLastUpdate = new File(pathToDataFile + ".cacheLastUpdate");
        File etag = new File(pathToDataFile + ".etag");
        File url = new File(pathToDataFile + ".url");

        if (data.exists() && (md5.exists() || lastModified.exists() || etag.exists())) {
            try {
                cacheStatus.setMd5(removeLineEndings(FileUtils.readFileToString(md5)));
                cacheStatus.setLastModified(Long.parseLong(removeLineEndings(FileUtils.readFileToString(lastModified))));
                cacheStatus.setCacheLastUpdate(Long.parseLong(removeLineEndings(FileUtils.readFileToString(cacheLastUpdate))));
                cacheStatus.setEtag(removeLineEndings(FileUtils.readFileToString(etag)));
                cacheStatus.setUrl(removeLineEndings(FileUtils.readFileToString(url)));
                cacheStatus.setData(data);
                cacheStatus.setSize(getCacheEntrySizeInMB(data));
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
                cacheStatus.setDataExists(false);
                return cacheStatus;
            }
            cacheStatus.setDataExists(true);
            return cacheStatus;
        }
        cacheStatus.setDataExists(false);
        return cacheStatus;
    }

    private String removeLineEndings(String in) {
        return in.replace("\n", "").replace("\r", "");
    }

    private HttpHeader getHttpHeadersOnly(String url) {
        try {
            HttpURLConnection con
                    = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            HttpHeader httpHeader = new HttpHeader();
            httpHeader.setLastModified(con.getLastModified());
            httpHeader.setResponseCode(con.getResponseCode());
            httpHeader.seteTag(con.getHeaderField("ETag"));
            httpHeader.setMd5(con.getHeaderField("Content-MD5"));
            httpHeader.setSize(con.getContentLengthLong());
            return httpHeader;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new HttpHeader(500);
        }
    }

    @Override
    public void clearAllCaches() {
        cleanupCache(DATA_DIR);
        try {
            FileUtils.forceMkdir(new File(DATA_DIR));
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        setCurrentCacheSize(BigInteger.ZERO);
    }

    @Override
    public void clearCacheByUrl(URL url) {
        String path = getCacheFolderPath(generateKeyFromUrl(url));
        cleanupCache(path);
    }

    @Override
    public boolean cacheExists(URL url) {
        CacheStatus cacheStatus = getCacheStatus(getCacheFolderPath(generateKeyFromUrl(url)));
        return cacheStatus.isDataExists();
    }

    @Override
    public BigInteger getCacheSizeInMB() {
        return FileUtils.sizeOfDirectoryAsBigInteger(new File(DATA_DIR)).divide(new BigInteger("1000000"));
    }

    private String getCacheBaseDirFromDat(String datPath) {
        return datPath.replaceAll(FILENAME + "." + FILENAME_EXT, "");
    }

    private BigInteger getCacheEntrySizeInMB(File file) {
        return FileUtils.sizeOfAsBigInteger(file).divide(new BigInteger("1000000"));
    }

    private void cleanUpTempFiles() {
        //TODO: remove temp files on startup
        Collection<File> listFiles = FileUtils.listFiles(new File(DATA_DIR), new String[]{FILENAME_TEMP_EXT}, true);
        Iterator<File> it = listFiles.iterator();
        while (it.hasNext()) {
            File file = it.next();
            FileUtils.deleteQuietly(file);
        }
    }

    private void enforceCacheSize() {
        BigInteger size = getCacheSizeInMB();

        if (size.compareTo(getMaxCacheSizeMB()) > 0) {
            BigInteger diff = size.subtract(getMaxCacheSizeMB());
            Map<String, BigInteger> files = new LinkedHashMap<>();
            Collection<File> listFiles = FileUtils.listFiles(new File(DATA_DIR), new String[]{FILENAME_EXT}, true);
            Iterator<File> it = listFiles.iterator();
            while (it.hasNext()) {
                File file = it.next();
                files.put(file.getAbsolutePath(), getCacheEntrySizeInMB(file));
            }
            files = sortByComparator(files);
            for (Map.Entry<String, BigInteger> entry : files.entrySet()) {
                String cacheBaseDir = getCacheBaseDirFromDat(entry.getKey());
                cleanupCache(cacheBaseDir);
                diff = diff.subtract(entry.getValue());
                if (diff.compareTo(BigInteger.ZERO) <= 0) {
                    break;
                }
            }
        }
    }

    /**
     * http://www.mkyong.com/java/how-to-sort-a-map-in-java/
     *
     * @param unsortMap
     * @return
     */
    private static Map<String, BigInteger> sortByComparator(Map<String, BigInteger> unsortMap) {

        // Convert Map to List
        List<Map.Entry<String, BigInteger>> list
                = new LinkedList<>(unsortMap.entrySet());

        // Sort list with comparator, to compare the Map values
        Collections.sort(list,
                (Map.Entry<String, BigInteger> o1,
                        Map.Entry<String, BigInteger> o2)
                -> (o1.getValue()).compareTo(o2.getValue()));

        // Convert sorted map back to a Map
        Map<String, BigInteger> sortedMap = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, BigInteger>> it = list.iterator(); it.hasNext();) {
            Map.Entry<String, BigInteger> entry = it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    @Override
    public void enforceEvictionPolicies() {
        cleanUpTempFiles();
        //enforceCacheSize();
        //enforceCacheExpireEvictionPolicy();
    }

    private void enforceCacheExpireEvictionPolicy() {
        Collection<File> listFiles = FileUtils.listFiles(new File(DATA_DIR), new String[]{FILENAME_EXT}, true);
        Iterator<File> it = listFiles.iterator();
        while (it.hasNext()) {
            File file = it.next();
            String cacheBaseDir = getCacheBaseDirFromDat(file.getAbsolutePath());
            CacheStatus cacheStatus = getCacheStatus(cacheBaseDir);
            Date now = new Date();
            if (now.getTime() > (cacheStatus.getCacheLastUpdate() + getCacheExpireMin().longValue() * 60000)) {
                cleanupCache(cacheBaseDir);
            }
        }
    }

    @Override
    public List<CacheStatus> getCacheEntries() {
        List<CacheStatus> cacheStatuses = Lists.newArrayList();
        Collection<File> listFiles = FileUtils.listFiles(new File(DATA_DIR), new String[]{FILENAME_EXT}, true);
        Iterator<File> it = listFiles.iterator();
        while (it.hasNext()) {
            File file = it.next();
            String cacheBaseDir = getCacheBaseDirFromDat(file.getAbsolutePath());
            CacheStatus cacheStatus = getCacheStatus(cacheBaseDir);
            cacheStatuses.add(cacheStatus);
        }
        return cacheStatuses;
    }

    @Override
    public CacheStatus getCacheStatus(URL url) {
        String path = getCacheFolderPath(generateKeyFromUrl(url));
        return getCacheStatus(path);
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    private class HttpHeader {

        private long lastModified;
        private int responseCode;
        private String eTag;
        private String md5;
        private long size;

        public HttpHeader() {

        }

        public HttpHeader(int responseCode) {
            this.responseCode = responseCode;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        public String geteTag() {
            return eTag;
        }

        public void seteTag(String eTag) {
            this.eTag = eTag;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

    }

}
