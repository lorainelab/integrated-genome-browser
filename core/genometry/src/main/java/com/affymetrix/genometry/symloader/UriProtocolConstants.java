/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.symloader;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 *
 * @author lorainelab
 */
public class UriProtocolConstants {

    private UriProtocolConstants() {
        //private constructor to prevent instantiation
    }
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String FILE_PROTOCOL = "file";
    public static final String FTP_PROTOCOL = "ftp";
    public static final String IGB_PROTOCOL = "igb";
    public static final List<String> SUPPORTED_PROTOCOLS;

    static {
        SUPPORTED_PROTOCOLS = ImmutableList.<String>builder()
                .add(HTTP_PROTOCOL)
                .add(HTTPS_PROTOCOL)
                .add(FILE_PROTOCOL)
                .add(FTP_PROTOCOL)
                .add(IGB_PROTOCOL).build();
    }

}
