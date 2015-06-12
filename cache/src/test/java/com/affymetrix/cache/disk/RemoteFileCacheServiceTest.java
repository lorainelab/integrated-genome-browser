/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.cache.disk;

import com.affymetrix.cache.api.RemoteFileService;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author jeckstei
 */
public class RemoteFileCacheServiceTest {
    
    public RemoteFileCacheServiceTest() {
    }

    @Test
    public void testGetFilebyUrl() throws MalformedURLException {
        RemoteFileService remoteFileService = new RemoteFileCacheService();
        Optional<InputStream> is = remoteFileService.getFilebyUrl(new URL("http://igbquickload.org/A_gambiae_Feb_2003/A_gambiae_Feb_2003.2bit"));
        Assert.assertTrue(is.isPresent());
    }
    
}
