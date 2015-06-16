/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.cache.disk;

import com.affymetrix.cache.api.RemoteFileService;
import com.google.common.base.Strings;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
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
    public static final String DATA_DIR = "/home/jeckstei/tmp/igb/cache/";
    public static final int FILENAME_SIZE = 100;
    public static final String FILENAME_EXT = "dat";
    public static final String FILENAME = "data";
    public static final long FILESIZE_MIN_BYTES = 4096L;
    public static final BigInteger MAX_CACHE_SIZE_MB = new BigInteger("100");
    public static final BigInteger CACHE_EXPIRE_MINUTES = new BigInteger("1440");

    @Override
    public Optional<InputStream> getFilebyUrl(URL url) {
        String path = getCacheFolderPath(generateKeyFromUrl(url));
        HttpHeader httpHeader = getHttpHeadersOnly(url.toString());
        if(httpHeader.getSize() < FILESIZE_MIN_BYTES) {
            try {
                return Optional.ofNullable(url.openStream());
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
                return Optional.empty();
            }
        }
        CacheStatus cacheStatus = getCacheStatus(path);
        if (isCacheValid(cacheStatus, httpHeader)) {
            try {
                return Optional.ofNullable(new FileInputStream(cacheStatus.getData()));
            } catch (FileNotFoundException ex) {
                LOG.error(ex.getMessage(), ex);
                return Optional.empty();
            }
        }
        cleanupCache(path);
        if (tryDownload(url, path)) {
            try {
                return Optional.ofNullable(new FileInputStream(new File(path + FILENAME + "." + FILENAME_EXT)));
            } catch (FileNotFoundException ex) {
                LOG.error(ex.getMessage(), ex);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private boolean tryDownload(URL url, String path) {
        String basePathToDataFile = path + FILENAME;
        String pathToDataFile = basePathToDataFile + "." + FILENAME_EXT;
        File tmpFile = new File(basePathToDataFile + ".tmp");
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
        try (
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
                BufferedInputStream bis = new BufferedInputStream( url.openStream());) {

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
            if (httpHeader.responseCode >= 400) {
                //If remote file is unavailable
                return true;
            }
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
    public BigInteger getCacheSize() {
        return FileUtils.sizeOfDirectoryAsBigInteger(new File(DATA_DIR));
    }
    
    private String getCacheBaseDirFromDat(String datPath) {
        return datPath.replaceAll(FILENAME + "." + FILENAME_EXT, "");
    }

    @Override
    public void enforceCacheSize() {
        BigInteger size = getCacheSize();
        size = size.divide(new BigInteger("1000000"));
        
        if(size.compareTo(MAX_CACHE_SIZE_MB) > 0) {
            BigInteger diff = size.subtract(MAX_CACHE_SIZE_MB);
            Map<String, BigInteger> files = new LinkedHashMap<>();
            Collection<File> listFiles = FileUtils.listFiles(new File(DATA_DIR), new String[]{FILENAME_EXT}, true);
            Iterator<File> it = listFiles.iterator();
            while(it.hasNext()) {
                File file = it.next();
                files.put(file.getAbsolutePath(), FileUtils.sizeOfAsBigInteger(file).divide(new BigInteger("1000000")));
            }
            //TODO: Figure out diff in size, determine number of files to delete
            files = sortByComparator(files);
            for(Map.Entry<String, BigInteger> entry : files.entrySet()) {
                String cacheBaseDir = getCacheBaseDirFromDat(entry.getKey());
                cleanupCache(cacheBaseDir);
                diff = diff.subtract(entry.getValue());
                if(diff.compareTo(BigInteger.ZERO) <= 0) {
                    break;
                }
            }
        }
    }
    
    /**
     * http://www.mkyong.com/java/how-to-sort-a-map-in-java/
     * @param unsortMap
     * @return 
     */
    private static Map<String, BigInteger> sortByComparator(Map<String, BigInteger> unsortMap) {
 
		// Convert Map to List
		List<Map.Entry<String, BigInteger>> list = 
			new LinkedList<>(unsortMap.entrySet());
 
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, 
                        (Map.Entry<String, BigInteger> o1,
                                Map.Entry<String, BigInteger> o2) -> 
                                (o1.getValue()).compareTo(o2.getValue()));
                   
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
        enforceCacheExpireEvictionPolicy();
    }
    
    public void enforceCacheExpireEvictionPolicy() {
        Collection<File> listFiles = FileUtils.listFiles(new File(DATA_DIR), new String[]{FILENAME_EXT}, true);
        Iterator<File> it = listFiles.iterator();
        while(it.hasNext()) {
            File file = it.next();
            String cacheBaseDir = getCacheBaseDirFromDat(file.getAbsolutePath());
            CacheStatus cacheStatus = getCacheStatus(cacheBaseDir);
            Date now = new Date();
            if(now.getTime() > (cacheStatus.getCacheLastUpdate() + CACHE_EXPIRE_MINUTES.longValue()*60000)) {
                cleanupCache(cacheBaseDir);
            }
        }
    }

    private class CacheStatus {

        private String md5;
        private long lastModified;
        private long cacheLastUpdate;
        private String etag;
        private boolean dataExists;
        private String url;
        private File data;

        public long getCacheLastUpdate() {
            return cacheLastUpdate;
        }

        public void setCacheLastUpdate(long cacheLastUpdate) {
            this.cacheLastUpdate = cacheLastUpdate;
        }
        
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
