package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.igb.view.load.FeatureTreeView;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;

public class FeatureLoadAction extends GenericAction {

	private static final long serialVersionUID = 1L;
	private final GenericFeature feature;

	public FeatureLoadAction(GenericFeature feature) {
		super(null, null, null, null, KeyEvent.VK_UNDEFINED, null, false);
		this.feature = feature;
	}

	private boolean isURLReachable(URI uri) {
		try {
			if (LocalUrlCacher.getInputStream(uri.toURL()) == null) {
				return false;
			}
		} catch (IOException ex) {
			Logger.getLogger(FeatureTreeView.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}

		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Object source = (e == null) ? null : e.getSource();
		if (feature.gVersion.gServer.serverType == ServerTypeI.QuickLoad) {
			String extension = FileTypeHolder.getInstance().getExtensionForURI(feature.symL.uri.toString());
			FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandler(extension);
			if (fth == null) {
				ErrorHandler.errorPanel("Load error", MessageFormat.format(GenometryConstants.BUNDLE.getString("noHandler"), extension), Level.SEVERE);
				GenometryModel.getGenometryModel().setFeatureLoaded(source, feature, false);
				return;
			}
		}
		String message;
		// check whether the selected feature url is reachable or not
		if (feature.gVersion.gServer.serverType == ServerTypeI.QuickLoad
				&& !isURLReachable(feature.getURI())) {
			message = "The feature " + feature.getURI() + " is not reachable.";
			ErrorHandler.errorPanel("Cannot load feature", message, Level.SEVERE);
			GenometryModel.getGenometryModel().setFeatureLoaded(source, feature, false);
			return;
		}

		// prevent from adding duplicated features
		if (GeneralLoadUtils.getLoadedFeature(feature.getURI()) != null) {
			message = "The feature " + feature.getURI() + " has already been added.";
			ErrorHandler.errorPanel("Cannot add same feature", message, Level.WARNING);
			GenometryModel.getGenometryModel().setFeatureLoaded(source, feature, false);
		} else {
			GeneralLoadView.getLoadView().addFeature(feature);
		}
	}
}
