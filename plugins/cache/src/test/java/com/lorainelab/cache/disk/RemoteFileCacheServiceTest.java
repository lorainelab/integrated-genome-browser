/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.cache.disk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.lorainelab.igb.cache.disk.RemoteFileDiskCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author jeckstei
 */
public class RemoteFileCacheServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteFileCacheServiceTest.class);

    RemoteFileDiskCacheService remoteFileService;

    public RemoteFileCacheServiceTest() {

    }

    @BeforeEach
    public void before() {
        try {
            remoteFileService = new RemoteFileDiskCacheService();
            RemoteFileDiskCacheService.DATA_DIR = "/home/jeckstei/.igb/fileCache/";
            FileUtils.forceMkdir(new File(RemoteFileDiskCacheService.DATA_DIR));
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Disabled
    @Test
    public void testEnforceEvictionPolicies() throws MalformedURLException {

        remoteFileService.clearAllCaches();
        remoteFileService.getFilebyUrl(new URL("http://igbquickload.org/A_gambiae_Feb_2003/A_gambiae_Feb_2003.2bit"), false);
        BigInteger size = remoteFileService.getCacheSizeInMB();
        assertTrue(size.compareTo(BigInteger.ZERO) >= 1);
        try {
            FileUtils.writeStringToFile(new File(RemoteFileDiskCacheService.DATA_DIR + "aWdicXVpY2tsb2FkLm9yZy9BX2dhbWJpYWVfRmViXzIwMDMvQV9nYW1iaWFlX0ZlYl8yMDAzLjJiaXQ=/cache/data.cacheLastUpdate"), "0");
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        remoteFileService.enforceEvictionPolicies();
        size = remoteFileService.getCacheSizeInMB();
        assertTrue(size.compareTo(BigInteger.ZERO) <= 0);
        
        remoteFileService.clearAllCaches();
        remoteFileService.getFilebyUrl(new URL("http://igbquickload.org/A_gambiae_Feb_2003/A_gambiae_Feb_2003.2bit"), false);
        remoteFileService.getFilebyUrl(new URL("http://igbquickload.org/A_lyrata_Apr_2011/A_lyrata_Apr_2011.2bit"), false);
        remoteFileService.getFilebyUrl(new URL("http://igbquickload.org/A_thaliana_Jan_2004/chr2.bnib"), false);
        size = remoteFileService.getCacheSizeInMB().divide(BigInteger.valueOf(1000000));
        assertFalse(size.compareTo(RemoteFileDiskCacheService.DEFAULT_MAX_CACHE_SIZE_MB) <= 0);
        remoteFileService.enforceEvictionPolicies();
        size = remoteFileService.getCacheSizeInMB().divide(BigInteger.valueOf(1000000));
        assertTrue(size.compareTo(RemoteFileDiskCacheService.DEFAULT_MAX_CACHE_SIZE_MB) <= 0);
    }

    @Disabled
    @Test
    public void testGetFilebyUrl() throws MalformedURLException {
        Optional<InputStream> is = remoteFileService.getFilebyUrl(new URL("http://igbquickload.org/A_gambiae_Feb_2003/A_gambiae_Feb_2003.2bit"), false);
        remoteFileService.getFilebyUrl(new URL("http://igbquickload.org/A_lyrata_Apr_2011/A_lyrata_Apr_2011.2bit"), false);
        remoteFileService.getFilebyUrl(new URL("http://igbquickload.org/A_thaliana_Jan_2004/chr2.bnib"), false);
        assertTrue(is.isPresent());
    }

    @Disabled
    @Test
    public void testFileSizeLimit() throws MalformedURLException {
        remoteFileService.clearAllCaches();
        Optional<InputStream> is = remoteFileService.getFilebyUrl(new URL("http://localhost/index.html"), false);
        assertTrue(is.isPresent());
        BigInteger size = remoteFileService.getCacheSizeInMB();
        assertTrue(size.compareTo(BigInteger.ZERO) == 0);
    }

    @Disabled
    @Test
    public void testClearAllCaches() {
        remoteFileService.clearAllCaches();
        assertTrue((new File(RemoteFileDiskCacheService.DATA_DIR)).exists());
    }

    @Disabled
    @Test
    public void testClearCacheByUrl() throws MalformedURLException {
        URL url = new URL("http://igbquickload.org/A_gambiae_Feb_2003/A_gambiae_Feb_2003.2bit");
        Optional<InputStream> is = remoteFileService.getFilebyUrl(url, false);
        assertTrue(remoteFileService.cacheExists(url));
        remoteFileService.clearCacheByUrl(url);
        assertFalse(remoteFileService.cacheExists(url));
    }

    @Disabled
    @Test
    public void testGetCacheSize() throws MalformedURLException {
        URL url = new URL("http://igbquickload.org/A_gambiae_Feb_2003/A_gambiae_Feb_2003.2bit");
        Optional<InputStream> is = remoteFileService.getFilebyUrl(url, false);
        BigInteger size = remoteFileService.getCacheSizeInMB();
        assertTrue(size.compareTo(BigInteger.ZERO) > 0);
        LOG.info("size: " + size.toString());
        remoteFileService.clearAllCaches();
        size = remoteFileService.getCacheSizeInMB();
        LOG.info("size: " + size.toString());
        assertTrue(size.compareTo(BigInteger.ZERO) == 0);

    }

    @Disabled
    @Test
    public void testGetCacheSizePerformance() throws MalformedURLException {
        long startTime = System.nanoTime();
        BigInteger size = remoteFileService.getCacheSizeInMB();
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        LOG.info("time (ms): " + duration / 1000000 + ", size (MB): " + size.divide(new BigInteger("1000000")));
        //10GB ~ 40ms on SSD
    }

    @Disabled
    @Test
    public void testMD5CalculationPerformance() {
        try {
            File file1 = new File("/home/jeckstei/tmp/igb/data1.dat");
            File file2 = new File("/home/jeckstei/tmp/igb/data2.dat");
            File file3 = new File("/home/jeckstei/tmp/igb/data3.dat");
            long startTime1 = System.nanoTime();
            String md5Calculated1 = convertByteArrayToHexString(
                    DigestUtils.md5(new FileInputStream(file1)));
            long endTime1 = System.nanoTime();
            long duration1 = (endTime1 - startTime1);
            LOG.info("hash: " + md5Calculated1 + "time (ms): " + duration1 / 1000000);

            long startTime2 = System.nanoTime();
            String md5Calculated2 = convertByteArrayToHexString(
                    DigestUtils.md5(new FileInputStream(file2)));
            long endTime2 = System.nanoTime();
            long duration2 = (endTime2 - startTime2);
            LOG.info("hash: " + md5Calculated2 + "time (ms): " + duration2 / 1000000);

            long startTime3 = System.nanoTime();
            String md5Calculated3 = convertByteArrayToHexString(
                    DigestUtils.md5(new FileInputStream(file3)));
            long endTime3 = System.nanoTime();
            long duration3 = (endTime3 - startTime3);
            LOG.info("hash: " + md5Calculated3 + "time (ms): " + duration3 / 1000000);

        } catch (Exception e) {
            LOG.error("Error calculating hash: " + e.getMessage(), e);
        }

        /*
         150 MB ~ 2015-06-16 09:32:01 INFO  RemoteFileCacheServiceTest:103 - hash: 50ae04f69743921dd6082dfe978672adtime (ms): 461
         700 MB ~ 2015-06-16 09:32:03 INFO  RemoteFileCacheServiceTest:110 - hash: b1035e2bd6751bf29260791d75798350time (ms): 1860
         1.4 GB ~ 2015-06-16 09:32:06 INFO  RemoteFileCacheServiceTest:117 - hash: dbb4b453aa37a81247182b0794f49827time (ms): 3750
         */
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrayBytes.length; i++) {
            sb.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}
