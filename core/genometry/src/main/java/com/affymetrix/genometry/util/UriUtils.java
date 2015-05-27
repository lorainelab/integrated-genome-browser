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

    public static HttpRequest getRemoteHttpRequest(URI uri) throws IOException, IllegalArgumentException {
        checkIsValidRequest(uri);
        if (checkIsLocalFile(uri)) {
            throw new IllegalArgumentException("URI is referencing a local file, this method is only meant for remote sources.");
        }
        HttpRequest httpRequest = HttpRequest.get(uri.toURL());
        httpRequest = httpRequest.acceptGzipEncoding()
                .uncompress(true)
                .trustAllCerts()
                .trustAllHosts()
                .followRedirects(true);
        return httpRequest;
    }

    //TODO could easily be modified to use cache, but this is not required for now since cacheing needs to be updated before it will be useful
    public static InputStream getInputStream(URI uri) throws IOException {
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
        return httpRequest.buffer();
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
