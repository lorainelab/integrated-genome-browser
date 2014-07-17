
package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.Application;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JComponent;

/**
 * This will instantiate a custom trust manager to handle untrusted
 * certificates when connecting to a DAS/2 server over HTTPS.  (In
 * normal situations where the server has a trusted certificate,
 * this code is not invoked.)
 */
public class IGBTrustManager implements X509TrustManager {

	public static void installTrustManager() {
		// Install the all-trusting trust manager
		try {
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
            for (X509Certificate cert : certs) {
                certificates.append(cert.getIssuerX500Principal().getName()).append(",").append("\n");
            }
		Application app = Application.getSingleton();
		JComponent comp = (app == null) ? null : app.getFrame().getRootPane();
		boolean response = Application.confirmPanel(comp, "Trust following certificate? " + certificates.toString(),
				PreferenceUtils.getCertificatePrefsNode(), certificates.toString(), true, "Do not show this again for the publisher above");

		if (!response) {
			throw new RuntimeException("Untrusted certificate.");
		}
	}
}
