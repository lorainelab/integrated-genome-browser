/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL_SCHEME;
import com.affymetrix.genometry.util.LocalUrlCacher;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class BamIndexValidator {

    private static final Logger logger = LoggerFactory.getLogger(BamIndexValidator.class);

    public static boolean bamFileHasIndex(URI uri) throws IOException {
        String scheme = uri.getScheme().toLowerCase();
        if (StringUtils.equals(scheme, FILE_PROTOCOL_SCHEME)) {
            File f = findIndexFile(new File(uri));
            return f != null;
        } else if (StringUtils.equals(scheme, HTTP_PROTOCOL_SCHEME) || StringUtils.equals(scheme, HTTPS_PROTOCOL_SCHEME) || StringUtils.equals(scheme, FTP_PROTOCOL_SCHEME)) {
            String uriStr = findIndexFile(uri.toString());
            return uriStr != null;
        }

        return false;
    }

    public static File findIndexFile(File bamfile) throws IOException {
        // Guess at the location of the .bai URL as BAM URL + ".bai"
        try {
            String path = bamfile.getPath();
            File f = new File(path + ".bai");
            if (f.exists()) {
                return f;
            }

            //look for xxx.bai
            path = path.substring(0, path.length() - 3) + "bai";
            f = new File(path);
            if (f.exists()) {
                return f;
            }
        } catch (Exception ex) {
            if (!(ex instanceof IOException)) {
                logger.error(ex.getMessage(), ex);
            }
        }
        throw new IOException("Bam Index Not Found");
    }

    public static String findIndexFile(String bamfile) throws IOException {
        // Guess at the location of the .bai URL as BAM URL + ".bai"
        try {
            String baiUriStr = bamfile + ".bai";
            if (LocalUrlCacher.isValidURL(baiUriStr)) {
                return baiUriStr;
            }

            baiUriStr = bamfile.substring(0, bamfile.length() - 3) + "bai";

            //look for xxx.bai
            if (LocalUrlCacher.isValidURL(baiUriStr)) {
                return baiUriStr;
            }
        } catch (Exception ex) {
            if (!(ex instanceof IOException)) {
                logger.error(ex.getMessage(), ex);
            }
        }
        throw new IOException("Bam Index Not Found");
    }
}
