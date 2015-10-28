package com.affymetrix.genometry.util;

import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL_SCHEME;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Strings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 *
 * @author dcnorris
 */
public class UriUtils {

    public static boolean isValidRequest(URI uri) throws IOException {
        final String scheme = uri.getScheme();
        if (Strings.isNullOrEmpty(scheme)) {
            return false;
        }
        if (scheme.equalsIgnoreCase(FILE_PROTOCOL_SCHEME)) {
            File f = new File(uri);
            return f.exists();
        } else {
            int code = -1;
            try {
                final HttpRequest httpRequest = HttpRequest.get(uri.toURL())
                        .trustAllCerts()
                        .trustAllHosts()
                        .followRedirects(true)
                        .connectTimeout(5000);
                code = httpRequest.code();
            } catch (HttpRequest.HttpRequestException ex) {
            }
            return code == HttpURLConnection.HTTP_OK;
        }
    }

    public static InputStream getInputStream(URI uri) throws IOException {
        try {
            checkIsValidRequest(uri);
            if (checkIsLocalFile(uri)) {
                File f = new File(uri);
                InputStream inputStream = new FileInputStream(f);
                return inputStream;
            }
            final HttpRequest httpRequest = HttpRequest.get(uri.toURL())
                    .acceptGzipEncoding()
                    .uncompress(true)
                    .trustAllCerts()
                    .trustAllHosts()
                    .followRedirects(true);
            if (isGzipContentEncoding(httpRequest)) {
                return GeneralUtils.getGZipInputStream(uri.toString(), httpRequest.buffer());
            }
            if (httpRequest.code() == HttpURLConnection.HTTP_OK) {
                return httpRequest.buffer();
            } else {
                throw new IOException("HTTP response code " + httpRequest.code() + " recieved, unable to load file");
            }
        } catch (HttpRequest.HttpRequestException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    private static boolean isGzipContentEncoding(final HttpRequest httpRequest) {
        final String contentEncoding = httpRequest.contentEncoding();
        if (!Strings.isNullOrEmpty(contentEncoding)) {
            return contentEncoding.equals("gzip");
        }
        return false;
    }

    private static void checkIsValidRequest(URI uri) throws IOException {
        if (!isValidRequest(uri)) {
            throw new IOException();
        }
    }

    private static boolean checkIsLocalFile(URI uri) {
        return uri.getScheme().equalsIgnoreCase(FILE_PROTOCOL_SCHEME);
    }
}
