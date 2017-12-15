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

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.swing.script.ScriptManager;
import com.affymetrix.igb.swing.script.ScriptProcessorHolder;
import com.google.common.collect.Lists;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ListIterator;
import java.util.Optional;
import java.util.logging.Level;
import javafx.stage.FileChooser;
import javax.swing.SwingWorker;
import org.lorainelab.igb.javafx.FileChooserUtil;

public final class RunScriptAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final RunScriptAction ACTION = new RunScriptAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static RunScriptAction getAction() {
        return ACTION;
    }
    
    private RunScriptAction() {
        super(BUNDLE.getString("runScript"), null,
                "16x16/actions/run_script.png",
                "22x22/actions/run_script.png",
                KeyEvent.VK_R, null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        loadFile();
    }
    
    // IGBF-1182: Change file chooser UI to OS native style for 'Run Script' command. 
    protected static File getSelectedFile() {
        // IGBF-1182: Currently we support only .igb files 
        // (hence for loop is not required), but if in future we start
        // supporting other file types, this structure would be useful.
        java.util.List<String> var = ScriptProcessorHolder.getInstance().getScriptExtensions();
        for (final ListIterator<String> i = var.listIterator(); i.hasNext();) {
            final String ext = i.next();
            i.set("*." + ext);
        }
        
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Script File(.igb)",var);
        Optional<File> selectedFile = FileChooserUtil.build()
                .setContext(FileTracker.DATA_DIR_TRACKER.getFile())
                .setTitle("Choose File")
                .setFileExtensionFilters(Lists.newArrayList(extFilter))
                .retrieveFileFromFxChooser();
        
        if (selectedFile.isPresent() && selectedFile.get()!= null) {
            FileTracker.DATA_DIR_TRACKER.setFile(selectedFile.get());
            return selectedFile.get();
        }
        return null;
    }
    
    /**
     * Load a file into the global singleton genometry model.
     */
    private void loadFile() {
        final File file = getSelectedFile();
        if (file == null)
            return;
        
        if (ScriptManager.getInstance().isScript(file.getAbsolutePath())) {
            runScript(file.getAbsolutePath());
        } else {
            ErrorHandler.errorPanel("script error", file.getAbsolutePath() + " is not a valid script file", Level.SEVERE);
        }
    }

    public void runScript(final String filePath) {
        final IGB igb = ((IGB) IGB.getInstance());
        synchronized (igb) {
            if (igb.getScriptWorker() != null) {
                ErrorHandler.errorPanel("script error", "another script is running, only one can run at a time", Level.SEVERE);
            } else {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

                    @Override
                    protected Void doInBackground() {
                        ScriptManager.getInstance().runScript(filePath);
                        return null;
                    } 

                    @Override
                    protected void done() {
                        igb.setScriptWorker(null);
                    }
                };
                igb.setScriptWorker(worker);
                ThreadUtils.getPrimaryExecutor(ScriptManager.SCRIPTING).execute(worker);
            }
        }
    }
}