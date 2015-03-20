/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.FileDropHandler;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.UniFileFilter;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @version $Id: LoadFileAction.java 11360 2012-05-02 14:41:01Z anuj4159 $
 */
public final class LoadFileAction extends OpenURIAction {

    private static final Logger logger = LoggerFactory.getLogger(LoadFileAction.class);

    private static final long serialVersionUID = 1L;
    private static final LoadFileAction ACTION = new LoadFileAction();

    private boolean mergeSelected = false;
    private final FileTracker load_dir_tracker;

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static LoadFileAction getAction() {
        return ACTION;
    }

    private final TransferHandler fdh = new FileDropHandler() {

        private static final long serialVersionUID = 1L;

        @Override
        public void openFileAction(List<File> files) {
            AnnotatedSeqGroup loadGroup = getloadGroup();
            String speciesName = getSpeciesName();
            UniFileFilter all_known_types = getAllKnowFilter();
            for (File f : files) {
                openURIOrRunScript(f.toURI(), loadGroup, speciesName, f.getName(), all_known_types);
                mergeSelected = true; // loadGroup will not be null at this point
            }
        }

        @Override
        public void openURLAction(String url) {
            try {
                UniFileFilter all_known_types = getAllKnowFilter();
                openURIOrRunScript(new URI(url.trim()), getloadGroup(), getSpeciesName(), url, all_known_types);
            } catch (URISyntaxException ex) {
                ex.printStackTrace();
                ErrorHandler.errorPanel("INVALID URL", url + "\n Url provided is not valid: ", Level.SEVERE);
            }
        }
    };

    private void openURIOrRunScript(URI uri, AnnotatedSeqGroup loadGroup, String speciesName, String name, FileFilter all_known_types) {
        if (openURI(uri, loadGroup, speciesName, all_known_types)) {
            return;
        }
        ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + name, Level.WARNING);
    }

    private boolean openURI(URI uri, AnnotatedSeqGroup loadGroup, String speciesName, FileFilter all_known_types) {
        String unzippedName = GeneralUtils.getUnzippedName(uri.getPath());
        String friendlyName = unzippedName.substring(unzippedName.lastIndexOf('/') + 1);
        if (!all_known_types.accept(new File(friendlyName))) {
            return false;
        }
        openURI(uri, friendlyName, mergeSelected, loadGroup, speciesName, false);//Always load as track

        return true;
    }

    private AnnotatedSeqGroup getloadGroup() {
        AnnotatedSeqGroup loadGroup = gmodel.getSelectedSeqGroup();
        if (loadGroup == null) {
            mergeSelected = false;
            loadGroup = gmodel.addSeqGroup(UNKNOWN_GENOME_PREFIX + " " + unknown_group_count);
        } else {
            mergeSelected = true;
        }
        return loadGroup;
    }

    private String getSpeciesName() {
        String speciesName = igbService.getSelectedSpecies();
        if (SELECT_SPECIES.equals(speciesName)) {
            speciesName = UNKNOWN_SPECIES_PREFIX + " " + unknown_group_count;
        }
        return speciesName;
    }

    private LoadFileAction() {
        super(BUNDLE.getString("openFile"), BUNDLE.getString("openFileTooltip"),
                "16x16/actions/document-open.png",
                "22x22/actions/document-open.png",
                KeyEvent.VK_O, null, true);
        this.ordinal = -9009000;
        load_dir_tracker = FileTracker.DATA_DIR_TRACKER;
        this.igbService.getApplicationFrame().setTransferHandler(fdh);
        setKeyStrokeBinding("ctrl O");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);

        FileDialog fileChooser = new FileDialog(igbService.getApplicationFrame());
        File currDir = load_dir_tracker.getFile();
        if (currDir == null) {
            currDir = new File(System.getProperty("user.home"));
        }
        fileChooser.setDirectory(currDir.getAbsolutePath());
        fileChooser.setMultipleMode(true);

        String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();
        AnnotatedSeqGroup loadGroup = GenometryModel.getInstance().getSelectedSeqGroup();
        File[] files = null;

        if (!SELECT_SPECIES.equals(speciesName) && loadGroup != null) {
            fileChooser.setVisible(true);
            files = fileChooser.getFiles();
        } else {
            ErrorHandler.errorPanel(BUNDLE.getString("noGenomeSelectedTitle"),
                    BUNDLE.getString("noGenomeSelectedMessage"), Level.INFO);
        }

        if (files == null || files.length == 0) {
            return;
        }

        load_dir_tracker.setFile(new File(fileChooser.getDirectory()));

        for (File file : files) {
            URI uri = file.toURI();
            openURI(uri, file.getName(), true, loadGroup, speciesName, false);
        }
    }
}
