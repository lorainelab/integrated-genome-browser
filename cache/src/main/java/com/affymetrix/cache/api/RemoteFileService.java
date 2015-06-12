/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.cache.api;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

/**
 *
 * @author jeckstei
 */
public interface RemoteFileService {
    public Optional<InputStream> getFilebyUrl(URL url);
}
