
package com.affymetrix.igb.util;

import java.security.*;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Custom class to handle trust manager
 * @author hiralv
 * Ref : http://stackoverflow.com/questions/2893819/telling-java-to-accept-self-signed-ssl-certificate
 */
public class IGBTrustManager implements X509TrustManager {

	public static void installTrustManager() {
		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, new TrustManager[]{new IGBTrustManager()}, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	// Create a trust manager that does not validate certificate chains
	// For now accept all certificate
	// TODO: Implementation to allow user to accept/reject certificate
	public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		return null;
	}

	public void checkClientTrusted(
			java.security.cert.X509Certificate[] certs, String authType) {
	}

	public void checkServerTrusted(
			java.security.cert.X509Certificate[] certs, String authType) {
	}
}
