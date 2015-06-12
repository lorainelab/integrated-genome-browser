/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.cache.disk;

import com.affymetrix.cache.api.RemoteFileService;
import com.google.common.base.Strings;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
public class RemoteFileCacheService implements RemoteFileService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteFileCacheService.class);

    //TODO: Move to properties
    public static final String DATA_DIR = "/tmp/igb/cache/";
    public static final int FILENAME_SIZE = 100;
    public static final String FILENAME = "data";

    @Override
    public Optional<InputStream> getFilebyUrl(URL url) {
        String path = getCacheFolderPath(generateKeyFromUrl(url));
        HttpHeader httpHeader = getHttpHeadersOnly(url.toString());
        CacheStatus cacheStatus = getCacheStatus(path);
        if (isCacheValid(cacheStatus, httpHeader)) {
            try {
                return Optional.ofNullable(new FileInputStream(cacheStatus.getData()));
            } catch (FileNotFoundException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        cleanupCache(path);
        if(tryDownload(url, path, FILENAME)) {
            try {
                return Optional.ofNullable(new FileInputStream(new File(path + FILENAME)));
            } catch (FileNotFoundException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return Optional.empty();
    }

    private boolean tryDownload(URL url, String path, String dataFileName) {
        String pathToDataFile = path + dataFileName;
        File tmpFile = new File(pathToDataFile + ".tmp");
        File md5File = new File(pathToDataFile + ".md5");
        File lastModifiedFile = new File(pathToDataFile + ".lastModified");
        File etagFile = new File(pathToDataFile + ".etag");
        File urlFile = new File(pathToDataFile + ".url");
        try {
            FileUtils.forceMkdir(new File(path));
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
            return false;
        }
        try (
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
                InputStream is = url.openStream();) {

            String md5 = url.openConnection().getHeaderField("Content-MD5");
            if(!Strings.isNullOrEmpty(md5)) {
                md5 = md5.replaceAll("\"", "");
            }
            long lastModified = url.openConnection().getLastModified();
            String etag = url.openConnection().getHeaderField("ETag");
            if(!Strings.isNullOrEmpty(etag)) {
                etag = etag.replaceAll("\"", "");
            }
            FileUtils.writeStringToFile(md5File, md5);
            FileUtils.writeStringToFile(lastModifiedFile, Long.toString(lastModified));
            FileUtils.writeStringToFile(etagFile, etag);
            FileUtils.writeStringToFile(urlFile, url.toString());

            IOUtils.copy(is, bos);
            bos.flush();

            if (!Strings.isNullOrEmpty(md5) && verifyFile(md5, tmpFile)) {
                File finalFile = new File(pathToDataFile);
                FileUtils.moveFile(tmpFile, finalFile);
                return true;
            } else {
                File finalFile = new File(pathToDataFile);
                FileUtils.moveFile(tmpFile, finalFile);
                return true;
            }
        } catch (Exception e) {
            LOG.error("Error downloading: " + e.getMessage(), e);
            FileUtils.deleteQuietly(new File(path));
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
        FileUtils.deleteQuietly(new File(path));
    }

    private String generateKeyFromUrl(URL url) {
        return url.getHost() + url.getFile();
    }

    private String getCacheFolderPath(String key) {
        //TODO: Possible hash instead
        //byte[] sha256 = DigestUtils.sha256(key);
        String base64Key = Base64.encode(key.getBytes());
        List<String> folders = new ArrayList<>();
        int index = 0;
        while (index < base64Key.length()) {
            folders.add(base64Key.substring(index, Math.min(index + FILENAME_SIZE, base64Key.length())));
            index += FILENAME_SIZE;
        }
        StringBuilder path = new StringBuilder(DATA_DIR);
        for (String folder : folders) {
            //TODO:Windows considerations
            path.append(folder).append("/");
        }
        path.append("cache/");
        return path.toString();
    }

    private boolean isCacheValid(CacheStatus cacheStatus, HttpHeader httpHeader) {
        if (cacheStatus.isDataExists()) {
            if (!Strings.isNullOrEmpty(httpHeader.getMd5())
                    && !Strings.isNullOrEmpty(cacheStatus.getMd5())) {
                if (cacheStatus.getMd5().equals(httpHeader.getMd5())) {
                    return true;
                }
                return false;
            } else if (httpHeader.getLastModified() > 0
                    && cacheStatus.getLastModified() > 0) {
                if (httpHeader.getLastModified() == cacheStatus.getLastModified()) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private CacheStatus getCacheStatus(String path) {
        CacheStatus cacheStatus = new CacheStatus();
        File data = new File(path + "data");
        File md5 = new File(path + "data.md5");
        File lastModified = new File(path + "data.lastModified");
        File etag = new File(path + "data.etag");
        File url = new File(path + "data.url");
        if (data.exists() && (md5.exists() || lastModified.exists() || etag.exists())) {
            try {
                cacheStatus.setMd5(FileUtils.readFileToString(md5));
                cacheStatus.setLastModified(Long.parseLong(FileUtils.readFileToString(lastModified)));
                cacheStatus.setEtag(FileUtils.readFileToString(etag));
                cacheStatus.setUrl(FileUtils.readFileToString(url));
                cacheStatus.setData(data);
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
            return httpHeader;
        } catch (Exception e) {
            return new HttpHeader(500);
        }
    }

    private class CacheStatus {

        private String md5;
        private long lastModified;
        private String etag;
        private boolean dataExists;
        private String url;
        private File data;

        public File getData() {
            return data;
        }

        public void setData(File data) {
            this.data = data;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        public String getEtag() {
            return etag;
        }

        public void setEtag(String etag) {
            this.etag = etag;
        }

        public boolean isDataExists() {
            return dataExists;
        }

        public void setDataExists(boolean dataExists) {
            this.dataExists = dataExists;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }

    private class HttpHeader {

        private long lastModified;
        private int responseCode;
        private String eTag;
        private String md5;

        public HttpHeader() {

        }

        public HttpHeader(int responseCode) {
            this.responseCode = responseCode;
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
