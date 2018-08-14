package com.affymetrix.igb.util;

import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.igb.IGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JComponent;

/**
 * This will instantiate a custom trust manager to handle untrusted
 * certificates when connecting to a DAS/2 server over HTTPS. (In
 * normal situations where the server has a trusted certificate,
 * this code is not invoked.)
 */
public class IGBTrustManager implements X509TrustManager {

    static X509TrustManager defaultTm = null;
    private static final Logger logger = LoggerFactory.getLogger(IGBTrustManager.class);
    public static void installTrustManager() {
        // Install the all-trusting trust manager
        try {
            //kiran:IGBF-1362: Added default trust store also as the trust manager
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            // Using null here initialises the TMF with the default trust store.
            tmf.init((KeyStore) null);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    defaultTm = (X509TrustManager) tm;
                    break;
                }
            }
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new IGBTrustManager()}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    public void checkClientTrusted(
            java.security.cert.X509Certificate[] certs, String authType) {
    }

    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {

        StringBuilder certificates = new StringBuilder("\n\n");
        IGB app = IGB.getInstance();
        for (X509Certificate cert : certs) {
            certificates.append(cert.getIssuerX500Principal().getName()).append(",").append("\n");
        }
        JComponent comp = (app == null) ? null : app.getFrame().getRootPane();
        try {
            //kiran:IGBF-1362: First try to validate the certificate using the default trust store
            defaultTm.checkServerTrusted(certs,authType);
            logger.info("Authenticated {} certificates using default trust store",certificates.toString().replace("\n", "").replace("\r", ""));
        } catch (CertificateException e) {
            //if certificate not found then ask the user to validate the certificate
            boolean response = ModalUtils.confirmPanel(comp, "Trust following certificate? " + certificates.toString(),
                    PreferenceUtils.getCertificatePrefsNode(), certificates.toString(), true, "Do not show this again for the publisher above");

            if (!response) {
                throw new RuntimeException("Untrusted certificate.");
            }
        }
    }
}
