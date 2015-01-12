package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.parsers.AnnotationWriter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.ExportFileModel;
import com.affymetrix.genometryImpl.util.GFileChooser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genoviz.bioviews.Glyph;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.shared.FileTracker;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public abstract class AbstractExportFileAction
        extends GenericAction implements SymSelectionListener {

    private static final long serialVersionUID = 1l;
    private static final GenometryModel gmodel = GenometryModel.getInstance();
    private final ExportFileModel model;
    private final Map<FileTypeCategory, UniFileFilter> preferredFilters;

    protected AbstractExportFileAction(
            String text,
            String tooltip,
            String iconPath, String largeIconPath,
            int mnemonic,
            Object extraInfo,
            boolean popup) {
        super(text, tooltip, iconPath, largeIconPath, mnemonic, extraInfo, popup);
        model = new ExportFileModel();
        preferredFilters = new EnumMap<>(FileTypeCategory.class);
    }

    /**
     * Override to enable or disable self based on tracks selected. Note that
     * this must match {@link #actionPerformed(ActionEvent)} which only works
     * when one track is selected.
     */
    @Override
    public void symSelectionChanged(SymSelectionEvent evt) {
        List<Glyph> answer = IGBServiceImpl.getInstance().getSelectedTierGlyphs();
        setEnabled(1 == answer.size() && answer.get(0).getInfo() != null && isExportable(((TierGlyph) answer.get(0)).getFileTypeCategory()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        List<Glyph> current_tiers = IGBServiceImpl.getInstance().getSelectedTierGlyphs();
        if (current_tiers.size() > 1) {
            ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
        } else if (current_tiers.isEmpty()) {
            ErrorHandler.errorPanel(BUNDLE.getString("noTrackError"));
        } else {
            TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
            saveAsFile(current_tier);
        }
    }

    private void saveAsFile(TierGlyph atier) {
        RootSeqSymmetry rootSym = (RootSeqSymmetry) atier.getInfo();
        Map<UniFileFilter, AnnotationWriter> filter2writers = model.getFilterToWriters(rootSym.getCategory());
        if (filter2writers != null && !filter2writers.isEmpty()) {
            JFileChooser chooser = new GFileChooser();
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setMultiSelectionEnabled(false);
            chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
            filter2writers.keySet().forEach(chooser::addChoosableFileFilter);
            UniFileFilter preferredFilter = preferredFilters.get(rootSym.getCategory());
            if (preferredFilter == null) {
                chooser.setFileFilter(chooser.getChoosableFileFilters()[0]);
            } else {
                for (FileFilter filter : chooser.getChoosableFileFilters()) {
                    if (filter.getDescription().equals(preferredFilter.getDescription())) {
                        chooser.setFileFilter(filter);
                        break;
                    }
                }
            }

            int option = chooser.showSaveDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
                BioSeq aseq = gmodel.getSelectedSeq();
                DataOutputStream dos = null;
                try {
                    File fil = chooser.getSelectedFile();
                    dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fil)));
                    UniFileFilter selectedFilter = (UniFileFilter) chooser.getFileFilter();
                    preferredFilters.put(rootSym.getCategory(), selectedFilter);
                    exportFile(filter2writers.get(selectedFilter), dos, aseq, atier);
                } catch (Exception ex) {
                    ErrorHandler.errorPanel("Problem saving file", ex, Level.SEVERE);
                } finally {
                    GeneralUtils.safeClose(dos);
                }
            }
        } else {
            ErrorHandler.errorPanel("not supported yet", "cannot export files of type "
                    + rootSym.getCategory().toString(), Level.WARNING);
        }
    }

    public boolean isExportable(FileTypeCategory category) {
        Map<UniFileFilter, AnnotationWriter> filter2writers = model.getFilterToWriters(category);
        return filter2writers != null && !filter2writers.isEmpty();
    }

    protected abstract void exportFile(
            AnnotationWriter annotationWriter,
            DataOutputStream dos,
            BioSeq aseq,
            TierGlyph atier
    ) throws java.io.IOException;

}
