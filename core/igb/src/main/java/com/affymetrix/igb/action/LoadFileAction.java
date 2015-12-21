/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy of the license must be included with
 * any distribution of this source code. Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometry.GenomeVersion;
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
import com.lorainelab.igb.javafx.FileChooserUtil;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
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
    private final int TOOLBAR_INDEX = 0;

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
            GenomeVersion genomeVersion = getGenomeVersion();
            String speciesName = getSpeciesName();
            if (speciesName.startsWith(UNKNOWN_SPECIES_PREFIX)) {
                genomeVersion.setSpeciesName(speciesName);
            }
            UniFileFilter all_known_types = getAllSupportedExtensionsFilter();
            for (File f : files) {
                openURIOrRunScript(f.toURI(), genomeVersion, f.getName(), all_known_types);
                mergeSelected = true; // loadGroup will not be null at this point
            }
        }

        @Override
        public void openURLAction(String url) {
            try {
                UniFileFilter all_known_types = getAllSupportedExtensionsFilter();
                GenomeVersion loadGroup = getGenomeVersion();
                String speciesName = getSpeciesName();
                if (speciesName.startsWith(UNKNOWN_SPECIES_PREFIX)) {
                    loadGroup.setSpeciesName(speciesName);
                }
                openURIOrRunScript(new URI(url.trim()), loadGroup, url, all_known_types);
            } catch (URISyntaxException ex) {
                ex.printStackTrace();
                ErrorHandler.errorPanel("INVALID URL", url + "\n Url provided is not valid: ", Level.SEVERE);
            }
        }
    };

    private void openURIOrRunScript(URI uri, GenomeVersion loadGroup, String name, FileFilter all_known_types) {
        if (openURI(uri, loadGroup, all_known_types)) {
            return;
        }
        ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + name, Level.WARNING);
    }

    private boolean openURI(URI uri, GenomeVersion genomeVersion, FileFilter all_known_types) {
        String unzippedName = GeneralUtils.getUnzippedName(uri.getPath());
        String friendlyName = unzippedName.substring(unzippedName.lastIndexOf('/') + 1);
        if (!all_known_types.accept(new File(friendlyName))) {
            return false;
        }
        openURI(uri, friendlyName, mergeSelected, genomeVersion, genomeVersion.getSpeciesName(), false);//Always load as track

        return true;
    }

    private GenomeVersion getGenomeVersion() {
        GenomeVersion genomeVersion = gmodel.getSelectedGenomeVersion();
        if (genomeVersion == null) {
            mergeSelected = false;
            genomeVersion = gmodel.addGenomeVersion(UNKNOWN_GENOME_PREFIX + " " + CUSTOM_GENOME_COUNTER);
        } else {
            mergeSelected = true;
        }
        return genomeVersion;
    }

    private String getSpeciesName() {
        String speciesName = igbService.getSelectedSpecies();
        if (SELECT_SPECIES.equals(speciesName)) {
            speciesName = UNKNOWN_SPECIES_PREFIX + " " + CUSTOM_GENOME_COUNTER;
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
        String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();
        GenomeVersion loadGroup = GenometryModel.getInstance().getSelectedGenomeVersion();
        File currDir = load_dir_tracker.getFile();
        if (currDir == null) {
            currDir = new File(System.getProperty("user.home"));
        }
        List<File> files = null;
        Optional<List<File>> selectedFiles;
        if (!SELECT_SPECIES.equals(speciesName) && loadGroup != null) {
            selectedFiles = FileChooserUtil.build().setContext(currDir).retrieveFilesFromFxChooser();
            if (selectedFiles.isPresent()) {
                files = selectedFiles.get();
            }
        } else {
            ErrorHandler.errorPanel(BUNDLE.getString("noGenomeSelectedTitle"),
                    BUNDLE.getString("noGenomeSelectedMessage"), Level.INFO);
        }
        if (files == null || files.isEmpty()) {
            return;
        }
        load_dir_tracker.setFile(files.get(0).getParentFile());
        for (File file : files) {
            URI uri = file.toURI();
            openURI(uri, file.getName(), true, loadGroup, speciesName, false);
        }
    }

    @Override
    public boolean isToolbarDefault() {
        return true;
    }

    @Override
    public int getToolbarIndex() {
        return TOOLBAR_INDEX;
    }

}
