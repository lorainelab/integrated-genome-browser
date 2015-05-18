package com.lorainelab.image.exporter;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.util.DisplayUtils;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.genometry.util.GeneralUtils;
import com.google.common.collect.ImmutableMap;
import com.lorainelab.igb.services.IgbService;
import static com.lorainelab.image.exporter.ExportDialogGui.UNIT;
import com.lorainelab.image.exporter.service.ImageExportService;
import java.awt.Color;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Optional;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Export Image class for IGB. It is designed to export different part of IGB
 * views to image file. Support format: SVG, PNG and JPG
 *
 * TODO: - Preview image size should changed automatically basis on real
 * percentile. - Support PDF format. - Action performed to the view(not export
 * view), this should not trigger to activate refresh button. Only current
 * export view changed should activate refresh button.
 *
 * @author nick
 */
@aQute.bnd.annotation.component.Component(name = ExportDialog.COMPONENT_NAME, immediate = true, provide = {ExportDialog.class, ImageExportService.class})
public class ExportDialog extends HeadLessExport implements ImageExportService {

    public static final String COMPONENT_NAME = "ExportDialog";
    private static final Logger logger = LoggerFactory.getLogger(ExportDialog.class);
    private static final double SPINNER_MIN = 1;
    private static final double SPINNER_MAX = 10_000;
    private static final int DEFAULT_SPINNER_HEIGHT = 800;
    private static final int DEFAULT_SPINNER_WIDTH = 600;
    private static final int DEFAULT_RESOLUTION = 300;
    public static float FONT_SIZE = 13.0f;
    private static final String DEFAULT_FILE = "igb.png";

    private static final ExportFileType SVG = new ExportFileType(EXTENSION[0], DESCRIPTION[0]);
    private static final ExportFileType PNG = new ExportFileType(EXTENSION[1], DESCRIPTION[1]);
    private static final ExportFileType JPEG = new ExportFileType(EXTENSION[2], DESCRIPTION[2]);
    private IgbService igbService;

    private String currentUnit;
    private final File defaultDir;
    private Component wholeFrame;
    private Component mainView;
    private Component mainViewWithLabels;
    private Component slicedViewWithLabels;
    private Component exportComponent;
    private boolean isWidthSpinner; // Prevent multiple triggering each other
    private boolean isHeightSpinner;
    private File exportFile;
    private String selectedExt;
    private final Map<ExportFileType, ExportFileFilter> FILTER_LIST;

    private final ExportDialogGui exportDialogGui;

    public ExportDialog() {
        this.FILTER_LIST = ImmutableMap.<ExportFileType, ExportFileFilter>of(
                SVG, new ExportFileFilter(SVG),
                PNG, new ExportFileFilter(PNG),
                JPEG, new ExportFileFilter(JPEG)
        );
        exportDialogGui = new ExportDialogGui(this);
        defaultDir = new File(exportNode.get(PREF_DIR, FileTracker.EXPORT_DIR_TRACKER.getFile().getAbsolutePath()));
        currentUnit = exportNode.get(PREF_UNIT, (String) UNIT[0]);
        isWidthSpinner = false;
        isHeightSpinner = false;
        imageInfo = new ExportImageInfo();
        imageInfo.setResolution(exportNode.getInt(PREF_RESOLUTION, DEFAULT_RESOLUTION));
        selectedExt = exportNode.get(PREF_EXT, EXTENSION[1]);
    }

    @Activate
    public void activate() {
        wholeFrame = igbService.getApplicationFrame();
        mainView = igbService.getMainViewComponent();
        mainViewWithLabels = igbService.getMainViewComponentWithLabels();
        slicedViewWithLabels = igbService.getSpliceViewComponentWithLabels();
        setDefaultComponent();
        initImageInfo();
        initFrame();
    }

    private void setDefaultComponent() {
        exportComponent = mainViewWithLabels;
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    /**
     * Display the export panel and initialize all related objects for export
     * process.
     *
     * @param isSequenceViewer
     */
    public void display(boolean isSequenceViewer) {
        initRadioButton(isSequenceViewer);
        initSpinner((String) exportDialogGui.getUnitComboBox().getSelectedItem());
        bringDialogFrameToFront();
        previewImage();
    }

    private void bringDialogFrameToFront() {
        JFrame topFrame = igbService.getApplicationFrame();
        Point location = topFrame.getLocation();
        exportDialogGui.getExportDialogFrame().setLocation(location.x + topFrame.getWidth() / 2 - exportDialogGui.getExportDialogFrame().getWidth() / 2,
                location.y + exportDialogGui.getExportDialogFrame().getHeight() / 2 - exportDialogGui.getExportDialogFrame().getHeight() / 2);
        DisplayUtils.bringFrameToFront(exportDialogGui.getExportDialogFrame());
    }

    /**
     * Set export exportComponent by determined which radio button is selected.
     * If the
     * method is triggered by sequence viewer, radio buttons panel will be set
     * to invisible and set sequence viewer as export exportComponent.
     *
     * @param isCustomComponent
     */
    private void initRadioButton(boolean isCustomComponent) {
        if (!isCustomComponent) {
            if (exportDialogGui.getSvRadioButton().isSelected()) {
                exportComponent = slicedViewWithLabels;
            } else if (exportDialogGui.getMvRadioButton().isSelected()) {
                exportComponent = mainView;
            } else if (exportDialogGui.getMvlRadioButton().isSelected()) {
                exportComponent = mainViewWithLabels;
            } else {
                exportComponent = wholeFrame;
                exportDialogGui.getWfRadioButton().setSelected(true);
            }
        }
        initImageInfo();
        exportDialogGui.getMvRadioButton().setEnabled(!igbService.getAllTierGlyphs().isEmpty());
        exportDialogGui.getMvlRadioButton().setEnabled(!igbService.getAllTierGlyphs().isEmpty());
        exportDialogGui.getSvRadioButton().setEnabled(!igbService.getSeqMapView().getSelectedSyms().isEmpty());
        hideRadioBtns(isCustomComponent);
    }

    /**
     * Initialize export panel.
     */
    private void initFrame() {
        initializeDefaultExportFile();
        initializeExtComboBox();
        initializeResolutionComboBox();
        initSpinner(currentUnit);
        initializeUnitComboBox();
        updateExportDialogComponents();
    }

    private void initializeUnitComboBox() {
        exportDialogGui.getUnitComboBox().setSelectedItem(currentUnit);
        exportDialogGui.getUnitComboBox().addActionListener((ActionEvent evt) -> {
            unitComboBoxActionPerformed();
        });
    }

    private void initializeResolutionComboBox() {
        exportDialogGui.getResolutionComboBox().setSelectedItem(imageInfo.getResolution());
        exportDialogGui.getResolutionComboBox().addActionListener((ActionEvent evt) -> {
            resolutionComboBoxActionPerformed();
        });
    }

    private void initializeExtComboBox() {
        exportDialogGui.getExtComboBox().setModel(new DefaultComboBoxModel(FILTER_LIST.keySet().toArray()));
        exportDialogGui.getExtComboBox().setSelectedItem(getType(getDescription(selectedExt)));
        exportDialogGui.getExtComboBox().addActionListener((java.awt.event.ActionEvent evt) -> {
            extComboBoxActionPerformed();
        });
    }

    private void initializeDefaultExportFile() {
        String file = exportNode.get(PREF_FILE, DEFAULT_FILE);
        if (file.equals(DEFAULT_FILE)) {
            exportFile = new File(defaultDir, file);
        } else {
            exportFile = new File(file);
        }
        exportDialogGui.getFilePathTextField().setText(exportFile.getAbsolutePath());
    }

    public FileFilter[] getAllExportFileFilters() {
        return FILTER_LIST.values().toArray(new FileFilter[FILTER_LIST.size()]);
    }

    /**
     * Initialize height and width Spinner. Support Unit: pixels and Inches
     *
     * @param unit
     */
    public void initSpinner(String unit) {
        double width = imageInfo.getWidth();
        double height = imageInfo.getHeight();

        if (unit.equals(UNIT[1])) {
            width /= imageInfo.getResolution();
            height /= imageInfo.getResolution();
        }
        if (!isValidSpinnerValue(width)) {
            width = DEFAULT_SPINNER_WIDTH;
            logger.warn("Error detected, invalid image width, reverting to default width.");
        }
        SpinnerModel sm = new SpinnerNumberModel(width, SPINNER_MIN, SPINNER_MAX, 1);
        exportDialogGui.getWidthSpinner().setModel(sm);
        if (!isValidSpinnerValue(height)) {
            height = DEFAULT_SPINNER_HEIGHT;
            logger.warn("Error detected, invalid image height, reverting to default height.");
        }
        sm = new SpinnerNumberModel(height, SPINNER_MIN, SPINNER_MAX, 1);
        exportDialogGui.getHeightSpinner().setModel(sm);

        resetWidthHeight(width, height);
    }

    private boolean isValidSpinnerValue(double value) {
        return SPINNER_MIN <= value && value <= SPINNER_MAX;
    }

    /**
     * Set exportComponent to export: Whole frame, main view, main view with
     * label,
     * sliced view and seq view.
     *
     * @param c
     */
    public void setComponent(Component c) {
        exportComponent = c;
    }

    /**
     * Saved image information basis on exportComponent.
     */
    public void initImageInfo() {
        imageInfo.setWidth(exportComponent.getWidth());
        imageInfo.setHeight(exportComponent.getHeight());
    }

    /**
     * Passed file and changed its extension.
     *
     * @param file
     * @param extension
     * @return file with new extension
     */
    public File changeFileExtension(File file, String extension) {
        if ((file == null) || (extension == null) || extension.trim().isEmpty()) {
            return null;
        }

        String path = file.getAbsolutePath();
        String ext = GeneralUtils.getExtension(path);
        if (ext.equalsIgnoreCase(extension)) {
            return file;
        }

        String filename = file.getName().trim();

        if (filename != null && !filename.isEmpty()) {

            int periodIndex = path.lastIndexOf('.');

            if (periodIndex > 0) {
                path = path.substring(0, periodIndex) + extension;
            } else {
                path += extension;
            }
        }

        return new File(path);
    }

    public void cancelButtonActionPerformed() {
        exportDialogGui.setFrameVisible(false);
    }

    /**
     * Display a file chooser panel and let user choose output path.
     *
     * @param panel
     */
    public void saveAsButtonActionPerformed() {
        String fileName = "igb";
        File directory = defaultDir;

        if (StringUtils.isNotBlank(exportFile.getAbsolutePath())) {
            fileName = exportFile.getAbsolutePath();
            try {
                String tempDir = fileName.substring(0, fileName.lastIndexOf("/"));
                File f = new File(tempDir);
                if (f.exists()) {
                    directory = f;
                } else {
                    ErrorHandler.errorPanel("The output path is invalid.");
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            if (fileName.contains("/")) {
                if (fileName.length() > fileName.lastIndexOf("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
            }
        }
        showFileDialog(directory.getAbsolutePath(), fileName);
        exportDialogGui.getExportDialogFrame().setVisible(false);
    }

    private void showFileDialog(String directory, String defaultFileName) {
        String ext = GeneralUtils.getExtension(defaultFileName);
        if (StringUtils.isBlank(ext)) {
            defaultFileName += selectedExt;
        }
        FileDialog dialog = new FileDialog(exportDialogGui.getExportDialogFrame(), "Save Image", FileDialog.SAVE);
        dialog.setDirectory(directory);
        dialog.setFile(defaultFileName);
        centerAndShowSaveDialog(dialog);
        Optional.ofNullable(dialog.getFile()).ifPresent(fileName -> {
            String currentExt = GeneralUtils.getExtension(fileName);
            if (!ArrayUtils.contains(EXTENSION, currentExt)) {
                fileName += EXTENSION[1];
                currentExt = EXTENSION[1];
            }
            selectedExt = currentExt;
            File imageFile = new File(dialog.getDirectory(), fileName);
            completeSaveButtonAction(imageFile);
        });
    }

    public void saveButtonActionPerformed() {
        String previousPath = exportFile.getAbsolutePath();
        String newPath = exportDialogGui.getFilePathTextField().getText();
        exportFile = new File(newPath);

        if (!isValidExportFile(previousPath)) {
            return;
        }
        String path = exportFile.getAbsolutePath();
        String ext = GeneralUtils.getExtension(path);
        if (!isExt(ext)) {
            ext = selectedExt;
            int index = path.lastIndexOf('.');
            int length = 0;
            if (index > 0) {
                length = path.substring(index).length();
            }
            if (length < 2) {
                if (length == 1) {
                    path = path.substring(0, index);
                }
                path += selectedExt;
                exportFile = new File(path);
            }
        }
        if (exportFile.exists()) {
            if (!isOverwrite()) {
                return;
            }
        }
        headlessComponentExport(exportComponent, exportFile, selectedExt, false);
        exportDialogGui.getExtComboBox().setSelectedItem(getType(getDescription(ext)));
        exportNode.put(PREF_FILE, exportFile.getAbsolutePath());
        exportNode.put(PREF_EXT, selectedExt);
        exportNode.put(PREF_DIR, defaultDir.getAbsolutePath());
        exportNode.putInt(PREF_RESOLUTION, imageInfo.getResolution());
        exportNode.put(PREF_UNIT, currentUnit);
        exportDialogGui.getExportDialogFrame().setVisible(false);
    }

    /**
     * Give the user the choice to overwrite the existing file or not.
     */
    private boolean isOverwrite() {
        String[] options = {"Yes", "No"};
        return JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
                null, "Overwrite Existing File?", "File Exists",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                options, options[1]);
    }

    /**
     * Test whether the current export path is valid or not. If not, reset
     * current export path to previous export path and return false. Also, test
     * whether the image size is valid or not.
     */
    private boolean isValidExportFile(String previousPath) {
        if (exportFile.getParentFile() == null || !exportFile.getParentFile().isDirectory()) {
            // if output path is invalid, reset to previous correct path
            ErrorHandler.errorPanel("The output path is invalid.");
            resetPath(previousPath);
            exportDialogGui.getFilePathTextField().grabFocus();
            return false;
        }

        // if image size is too large...
        long heapFreeSize = Runtime.getRuntime().freeMemory();
        long size = (long) imageInfo.getWidth() * (long) imageInfo.getHeight();
        if (size > heapFreeSize) {
            ErrorHandler.errorPanel("The image size is invalid.");
            return false;
        }

        return true;
    }

    private void centerAndShowSaveDialog(FileDialog dialog) {
        /* Lots of code to be able to center an awt.FileDialog on screen... */
        Rectangle rect = exportDialogGui.getExportDialogFrame().getContentPane().getBounds();
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.validate();
        double width = dialog.getBounds().getWidth();
        double height = dialog.getBounds().getHeight();
        double x = rect.getCenterX() - (width / 2);
        double y = rect.getCenterY() - (height / 2);
        /* Could be new Point(x, y) */
        Point leftCorner = new Point();
        leftCorner.setLocation(x, y);
        dialog.setLocation(leftCorner);
        dialog.setVisible(true);
    }

    private void completeSaveButtonAction(File file) {
        String newPath = file.getAbsolutePath();
        String ext = GeneralUtils.getExtension(newPath);
        ExportFileType type = getType(getDescription(ext));
        exportDialogGui.getExtComboBox().setSelectedItem(type);
        exportFile = file;
        headlessComponentExport(exportComponent, exportFile, selectedExt, false);
        exportNode.put(PREF_FILE, exportFile.getAbsolutePath());
        exportNode.put(PREF_EXT, selectedExt);
        exportNode.put(PREF_DIR, defaultDir.getAbsolutePath());
        exportNode.putInt(PREF_RESOLUTION, imageInfo.getResolution());
        exportNode.put(PREF_UNIT, currentUnit);
    }

    /**
     * Reset export file and file path text field to the new path.
     *
     * @param path
     */
    private void resetPath(String path) {
        exportFile = new File(path);
        exportDialogGui.getFilePathTextField().setText(path);
    }

    /**
     * Get export file type by passed image format description.
     */
    private ExportFileType getType(String description) {
        for (ExportFileType type : FILTER_LIST.keySet()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get image format description by passed image format extension.
     */
    private String getDescription(String ext) {
        if (ext.equalsIgnoreCase(EXTENSION[0])) {
            return DESCRIPTION[0];
        } else if (ext.equalsIgnoreCase(EXTENSION[1])) {
            return DESCRIPTION[1];
        } else {
            return DESCRIPTION[2];
        }
    }

    /**
     * Creates a new buffered image by exportComponent and reset label's icon.
     */
    private void previewImage() {
        prepareExportComponentForExport();
        updatePreviewLabel();
    }

    private void updatePreviewLabel() {
        exportDialogGui.getPreviewLabel().setBufferedImage(exportImage);
        exportDialogGui.getPreviewLabel().repaint();
    }

    private void prepareExportComponentForExport() {
        exportImage = GraphicsUtil.getDeviceCompatibleImage(exportComponent.getWidth(), exportComponent.getHeight());
        Graphics2D g = exportImage.createGraphics();
        if (exportComponent instanceof JFrame) {
            drawTitleBar(g);
        }
        exportComponent.printAll(g);
    }

    /**
     * A hack to force export to draw title bar
     *
     * @param g
     */
    private void drawTitleBar(Graphics2D g) {
        // Draw Background
        g.setColor(exportComponent.getBackground().darker());
        g.fillRect(0, 0, exportComponent.getWidth(), exportComponent.getHeight());

        // Draw Border
        g.setColor(Color.BLACK);
        g.fillRect(0, 20, exportComponent.getWidth(), 2);

        // Draw Title
        g.setFont(g.getFont().deriveFont(FONT_SIZE));
        int x_offset = (exportComponent.getWidth() - g.getFontMetrics().stringWidth(((JFrame) exportComponent).getTitle())) / 2;
        int y_offset = 14;
        g.drawString(((JFrame) exportComponent).getTitle(), x_offset, y_offset);
    }

    /**
     * Return whether the passed extention is contained in IGB support image
     * extention list or not.
     */
    public boolean isExt(String ext) {
        for (String s : EXTENSION) {
            if (s.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reset height Spinner's value when width Spinner changed.
     */
    public void widthSpinnerStateChanged() {
        if (!isHeightSpinner) {
            double newWidth = (Double) exportDialogGui.getWidthSpinner().getValue();
            double newHeight = newWidth * imageInfo.getHeightWidthRate();
            isWidthSpinner = true;
            exportDialogGui.getHeightSpinner().setValue(newHeight);
            isWidthSpinner = false;
            resetWidthHeight(newWidth, newHeight);
        }
    }

    /**
     * Reset width Spinner's value when height Spinner changed.
     */
    public void heightSpinnerStateChanged() {
        if (!isWidthSpinner) {
            double newHeight = (Double) exportDialogGui.getHeightSpinner().getValue();
            double newWidth = newHeight * imageInfo.getWidthHeightRate();
            isHeightSpinner = true;
            exportDialogGui.getWidthSpinner().setValue(newWidth);
            isHeightSpinner = false;
            resetWidthHeight(newWidth, newHeight);
        }
    }

    /**
     * Update the file path text field value when image format combo box action
     * performed.
     */
    public void extComboBoxActionPerformed() {
        String path = exportFile.getAbsolutePath();
        String ext = GeneralUtils.getExtension(path);
        selectedExt = ((ExportFileType) exportDialogGui.getExtComboBox().getSelectedItem()).getExtension();

        int index = path.lastIndexOf('.');
        int length = 0;
        if (index > 0) {
            length = path.substring(index).length();
        }

        // keep user input extension, if it's not a support extension
        if (!isExt(ext) && length > 1) {
            return;
        } else {
            if (!ext.equalsIgnoreCase(selectedExt)) {
                if (selectedExt.equals(EXTENSION[2])
                        && ext.equalsIgnoreCase(EXTENSION[3])) { // special case for jpg
                    return;
                }

                if (length > 0) {
                    path = path.substring(0, index);
                }
                path += selectedExt;
                resetPath(path);
            }
        }

        updateExportDialogComponents();
    }

    private void updateExportDialogComponents() {
        if (selectedExt.equals(EXTENSION[0])) {
            exportDialogGui.getMvRadioButton().setSelected(true);
            exportDialogGui.getWfRadioButton().setEnabled(false);
            exportDialogGui.getResolutionComboBox().setEnabled(false);
        } else {
            exportDialogGui.getWfRadioButton().setEnabled(true);
            exportDialogGui.getResolutionComboBox().setEnabled(true);
        }
    }

    /**
     * Update width and height spinner's value when unit combo box action
     * performed.
     *
     * Reset width spinner will trigger to reset height spinner.
     */
    public void unitComboBoxActionPerformed() {
        String unit = (String) exportDialogGui.getUnitComboBox().getSelectedItem();

        double newWidth = (Double) exportDialogGui.getWidthSpinner().getValue();
        if (!unit.equals(currentUnit)) {
            if (unit.equals(UNIT[0])) {
                newWidth *= imageInfo.getResolution();
            } else {
                newWidth /= imageInfo.getResolution();
            }
        }
        currentUnit = unit;
        exportDialogGui.getWidthSpinner().setValue(newWidth);
    }

    /**
     * Reset size label text by passed width and height.
     *
     * @param width
     * @param height
     */
    private void resetWidthHeight(double width, double height) {
        String sizeLabelText;
        if (currentUnit.equals(UNIT[1])) {
            // Convert back from inches to pixels
            width *= imageInfo.getResolution();
            height *= imageInfo.getResolution();
            NumberFormat numberFormat = NumberFormat.getNumberInstance();
            numberFormat.setMaximumFractionDigits(2);
            sizeLabelText = numberFormat.format(width) + " x " + numberFormat.format(height) + " " + UNIT[0];
        } else {
            NumberFormat numberFormat = NumberFormat.getNumberInstance();
            numberFormat.setMaximumFractionDigits(2);
            double widthInches = (Double) width / imageInfo.getResolution();
            double heighInches = (Double) height / imageInfo.getResolution();
            sizeLabelText = numberFormat.format(widthInches) + " x " + numberFormat.format(heighInches) + " " + UNIT[1];
        }
        exportDialogGui.getSizeLabel().setText(sizeLabelText);
        imageInfo.setWidth(width);
        imageInfo.setHeight(height);
        // Allow user to reset width and height back to current size

    }

    /**
     * Update output image resolution value when resolution combo box action
     * performed. If selected unit is 'inches', update width and height
     * spinner's value(reset width spinner will trigger to reset height
     * spinner).
     */
    public void resolutionComboBoxActionPerformed() {
        imageInfo.setResolution((Integer) exportDialogGui.getResolutionComboBox().getSelectedItem());
        if (currentUnit.equals(UNIT[1])) {
            double width = imageInfo.getWidth();
            width /= imageInfo.getResolution();
            exportDialogGui.getWidthSpinner().setValue(width);
        }
    }

    public void refreshButtonActionPerformed() {
        updatePreview();
    }

    public void mvRadioButtonActionPerformed() {
        exportComponent = mainView;
        refreshPreview();
    }

    public void mvlRadioButtonActionPerformed() {
        exportComponent = mainViewWithLabels;
        refreshPreview();
    }

    public void wfRadioButtonActionPerformed() {
        exportComponent = wholeFrame;
        refreshPreview();
    }

    public void svRadioButtonActionPerformed() {
        exportComponent = slicedViewWithLabels;
        refreshPreview();
    }

    private void updatePreview() {
        previewImage();
    }

    private void refreshPreview() {
        initImageInfo();
        initSpinner((String) exportDialogGui.getUnitComboBox().getSelectedItem());
        previewImage();
    }

    public String getCurrentUnit() {
        return currentUnit;
    }

    @Override
    public void exportComponent(Component component) {
        exportComponent = component;
        initImageInfo();
        display(true);
    }

    private void hideRadioBtns(boolean isCustomComponent) {
        exportDialogGui.getSvRadioButton().setVisible(!isCustomComponent);
        exportDialogGui.getMvRadioButton().setVisible(!isCustomComponent);
        exportDialogGui.getMvlRadioButton().setVisible(!isCustomComponent);
        exportDialogGui.getWfRadioButton().setVisible(!isCustomComponent);
    }

}

class ExportFileType {

    private final String fileExtension;
    private final String fileDescription;

    ExportFileType(String extension, String description) {
        fileExtension = extension;
        fileDescription = description;
    }

    public String getExtension() {
        return fileExtension;
    }

    public String getDescription() {
        return fileDescription;
    }

    @Override
    public String toString() {
        return getDescription();
    }
}

class ExportFileFilter extends FileFilter {

    public ExportFileType type;

    ExportFileFilter(ExportFileType type) {
        this.type = type;
    }

    @Override
    public boolean accept(File file) {

        if (file.isDirectory()) {
            return true;
        }

        return file.getName().toLowerCase().endsWith(type.getExtension());
    }

    @Override
    public String getDescription() {
        return type.getDescription();
    }

    public String getExtension() {
        return type.getExtension();
    }

    public boolean accept(File file, String name) {
        return name.toLowerCase().endsWith(type.getExtension());
    }
}
