package com.affymetrix.igb.quickload;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericSymRequest;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.general.FeatureLoading;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.QuickLoadServerModel;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 *
 * @author jnicol
 */
public class QuickLoadFeatureLoading extends GenericSymRequest {
	public QuickLoadFeatureLoading(GenericVersion version, String featureName) {
		super(determineQuickLoadURI(version, featureName));
	}

	public static String determineQuickLoadFileName(GenericVersion version, String featureName) {
		URL quickloadURL = null;
		try {
			quickloadURL = new URL((String) version.gServer.serverObj);
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
			return "";
		}

		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(GenometryModel.getGenometryModel(), quickloadURL);
		List<AnnotMapElt> annotsList = quickloadServer.getAnnotsMap(version.versionID);

		// Linear search, but over a very small list.
		for (AnnotMapElt annotMapElt : annotsList) {
			if (annotMapElt.title.equals(featureName)) {
				return annotMapElt.fileName;
			}
		}
		return "";
	}


	public static boolean loadQuickLoadAnnotations(final GenericFeature gFeature, SeqSpan overlapSpan)
			throws OutOfMemoryError {
		final String fileName = determineQuickLoadFileName(gFeature.gVersion, gFeature.featureName);
		if (fileName.length() == 0) {
			Application.getSingleton().removeNotLockedUpMsg("Loading feature " + gFeature.featureName);
			return false;
		}

		Executor vexec = ThreadUtils.getPrimaryExecutor(gFeature.gVersion.gServer);

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			public Void doInBackground() {
				try {
					loadQuickLoadFeature(fileName, gFeature);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
			@Override
			public void done() {
				Application.getSingleton().removeNotLockedUpMsg("Loading feature " + gFeature.featureName);
			}
		};

		vexec.execute(worker);
		return true;

	}


	private static boolean loadQuickLoadFeature(final String fileName, GenericFeature gFeature) throws OutOfMemoryError {
		InputStream istr = null;
		BufferedInputStream bis = null;
		final String annot_url = gFeature.gVersion.gServer.URL + "/" + gFeature.gVersion.versionID + "/" + fileName;

		try {
			istr = LocalUrlCacher.getInputStream(annot_url, true);
			if (istr == null) {
				return false;
			}
			bis = FeatureLoading.loadStreamFeature(fileName, gFeature.featureName, annot_url, istr, bis);
			return true;
		} catch (Exception ex) {
			System.out.println("Problem loading requested url:" + annot_url);
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(istr);
		}
		return false;
	}

	public static URI determineQuickLoadURI(GenericVersion version, String featureName) {
		URI uri = null;

		try {
			if (version.gServer.URL == null || version.gServer.URL.length() == 0) {
				int httpIndex = featureName.toLowerCase().indexOf("http:");
				if (httpIndex > -1) {
					// Strip off initial characters up to and including http:
					// Sometimes this is necessary, as URLs can start with invalid "http:/"
					featureName = GeneralUtils.convertStreamNameToValidURLName(featureName);
					uri = new URI(featureName);
				} else {
					uri = new URI("file://" + featureName);
				}
			} else {
				uri = new URI(
						version.gServer.URL + "/"
						+ version.versionID + "/"
						+ determineQuickLoadFileName(version, featureName));
			}
		} catch (URISyntaxException ex) {
			Logger.getLogger(QuickLoadFeatureLoading.class.getName()).log(Level.SEVERE, null, ex);
		}
		return uri;
	}

}
