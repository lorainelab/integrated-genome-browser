package com.affymetrix.igb.shared;

import com.affymetrix.genometry.util.DisplayUtils;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGB.IS_MAC;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.util.GraphicsUtil;
import com.affymetrix.igb.view.AltSpliceView;
import java.awt.Color;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
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
public class ExportDialog extends HeadLessExport {

    private static ExportDialog singleton;
    private static final Logger logger = LoggerFactory.getLogger(ExportDialog.class);
    private static final double SPINNER_MIN = 1;
    private static final double SPINNER_MAX = 10_000;
    private static final int DEFAULT_SPINNER_HEIGHT = 800;
    private static final int DEFAULT_SPINNER_WIDTH = 600;
    private static final int DEFAULT_RESOLUTION = 300;
    public static float FONT_SIZE = 13.0f;
    private static final String TITLE = "Save Image";
    private static final String DEFAULT_FILE = "igb.png";
    private static final Object[] RESOLUTION = {72, 200, 300, 400, 500, 600, 800, 1000};
    private static final Object[] UNIT = {"pixels", "inches"};
    private static final ExportFileType SVG = new ExportFileType(EXTENSION[0], DESCRIPTION[0]);
    private static final ExportFileType PNG = new ExportFileType(EXTENSION[1], DESCRIPTION[1]);
    private static final ExportFileType JPEG = new ExportFileType(EXTENSION[2], DESCRIPTION[2]);

    /**
     * @return ExportDialog instance
     */
    public static ExportDialog getSingleton() {
        if (singleton == null) {
            singleton = new ExportDialog();
        }

        return singleton;
    }

    private String currentUnit;
    private JFrame staticFrame;
    private File defaultDir;
    private AffyTieredMap seqMap;
    private AffyTieredMap svseqMap;
    private Component wholeFrame;
    private Component mainView;
    private Component mainViewWithLabels;
    private Component slicedView;
    private Component exportComponent;
    private String unit;
    private boolean isWidthSpinner; // Prevent multiple triggering each other
    private boolean isHeightSpinner;
    private File exportFile;
    private FileFilter extFilter;
    private File defaultExportFile;
    private String selectedExt;
    private final LinkedHashMap<ExportFileType, ExportFileFilter> FILTER_LIST;
    private final JComboBox extComboBox;
    private final JComboBox resolutionComboBox;
    private final JComboBox unitComboBox;
    private final JTextField filePathTextField;
    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JLabel previewLabel;
    private final JLabel sizeLabel;
    private final JButton okButton;
    private final JRadioButton svRadioButton;
    private final JRadioButton wfRadioButton;
    private final JRadioButton mvRadioButton;
    private final JRadioButton mvlRadioButton;
    private final JPanel buttonsPanel;

    public ExportDialog() {
        super();
        FILTER_LIST = new LinkedHashMap<>();
        FILTER_LIST.put(SVG, new ExportFileFilter(SVG));
        FILTER_LIST.put(PNG, new ExportFileFilter(PNG));
        FILTER_LIST.put(JPEG, new ExportFileFilter(JPEG));
        defaultDir = new File(System.getProperty("user.home"));
        extComboBox = new JComboBox();
        resolutionComboBox = new JComboBox(RESOLUTION);
        unitComboBox = new JComboBox(UNIT);
        filePathTextField = new JTextField();
        widthSpinner = new JSpinner();
        heightSpinner = new JSpinner();
        previewLabel = new JLabel();
        sizeLabel = new JLabel();
        okButton = new JButton();
        svRadioButton = new JRadioButton();
        wfRadioButton = new JRadioButton();
        mvRadioButton = new JRadioButton();
        mvlRadioButton = new JRadioButton();
        buttonsPanel = new JPanel();
        isWidthSpinner = false;
        isHeightSpinner = false;
        unit = "";
    }

    /**
     * Display the export panel and initialize all related objects for export
     * process.
     *
     * @param isSequenceViewer
     */
    public synchronized void display(boolean isSequenceViewer) {
        initRadioButton(isSequenceViewer);
        initFrame();
        DisplayUtils.bringFrameToFront(staticFrame);
        previewImage();
    }

    /**
     * Set export exportComponent by determined which radio button is selected.
     * If the
     * method is triggered by sequence viewer, radio buttons panel will be set
     * to invisible and set sequence viewer as export exportComponent.
     *
     * @param isSequenceViewer
     */
    private void initRadioButton(boolean isSequenceViewer) {
        if (!isSequenceViewer) {
            initView();

            if (svRadioButton.isSelected()) {
                setComponent(slicedView);
            } else if (mvRadioButton.isSelected()) {
                setComponent(mainView);
            } else if (mvlRadioButton.isSelected()) {
                setComponent(mainViewWithLabels);
            } else {
                setComponent(wholeFrame);
                wfRadioButton.setSelected(true);
            }

            initImageInfo();

            mvRadioButton.setEnabled(!seqMap.getTiers().isEmpty());
            mvlRadioButton.setEnabled(!seqMap.getTiers().isEmpty());
            svRadioButton.setEnabled(!svseqMap.getTiers().isEmpty());
        }

        buttonsPanel.setVisible(!isSequenceViewer);
    }

    /**
     * Initialize the reference components from different part views of IGB.
     */
    private void initView() {
        if (seqMap == null) {
            seqMap = IGB.getSingleton().getMapView().getSeqMap();
            wholeFrame = IGB.getSingleton().getFrame();
            mainView = seqMap.getNeoCanvas();
            AffyLabelledTierMap tm = (AffyLabelledTierMap) seqMap;
            mainViewWithLabels = tm.getSplitPane();
            AltSpliceView slice_view = (AltSpliceView) ((IGB) IGB.getSingleton()).getView(AltSpliceView.class.getName());
            slicedView = ((AffyLabelledTierMap) slice_view.getSplicedView().getSeqMap()).getSplitPane();
            svseqMap = slice_view.getSplicedView().getSeqMap();
        }
    }

    /**
     * Initialize export panel.
     */
    private void initFrame() {
        if (staticFrame == null) {
            String file = exportNode.get(PREF_FILE, DEFAULT_FILE);
            if (file.equals(DEFAULT_FILE)) {
                exportFile = new File(defaultDir, file);
            } else {
                exportFile = new File(file);
            }

            defaultDir = new File(exportNode.get(PREF_DIR, FileTracker.EXPORT_DIR_TRACKER.getFile().getAbsolutePath()));

            imageInfo.setResolution(exportNode.getInt(PREF_RESOLUTION, DEFAULT_RESOLUTION));

            filePathTextField.setText(exportFile.getAbsolutePath());

            ExportFileType type = getType(exportNode.get(PREF_EXT, DESCRIPTION[1]));
            extComboBox.setModel(new DefaultComboBoxModel(FILTER_LIST.keySet().toArray()));
            extComboBox.setSelectedItem(type);
            selectedExt = type.getExtension();
            resolutionComboBox.setSelectedItem(imageInfo.getResolution());

            unit = exportNode.get(PREF_UNIT, (String) UNIT[0]);
            unitComboBox.setSelectedItem(unit);
            currentUnit = unit;
            initSpinner(unit);

            staticFrame = PreferenceUtils.createFrame(TITLE, new ExportDialogGUI(this));
            staticFrame.setLocationRelativeTo(IGB.getSingleton().getFrame());
            staticFrame.getRootPane().setDefaultButton(okButton);
        } else {
            initSpinner((String) unitComboBox.getSelectedItem());
        }
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
        widthSpinner.setModel(sm);
        if (!isValidSpinnerValue(height)) {
            height = DEFAULT_SPINNER_HEIGHT;
            logger.warn("Error detected, invalid image height, reverting to default height.");
        }
        sm = new SpinnerNumberModel(height, SPINNER_MIN, SPINNER_MAX, 1);
        heightSpinner.setModel(sm);

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
        if (exportComponent == null) {
            exportComponent = mainViewWithLabels;
        }
        if (imageInfo == null) {
            imageInfo = new ExportImageInfo(exportComponent.getWidth(), exportComponent.getHeight());
        } else {
            imageInfo.setWidth(exportComponent.getWidth());
            imageInfo.setHeight(exportComponent.getHeight());
        }
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
        staticFrame.setVisible(false);
    }

    /**
     * Display a file chooser panel and let user choose output path.
     *
     * @param panel
     */
    public void browseButtonActionPerformed(JPanel panel) {
        String fileName = "igb";
        File directory = defaultDir;

        if (StringUtils.isNotBlank(filePathTextField.getText())) {
            fileName = filePathTextField.getText();
            try {
                String tempDir = fileName.substring(0, fileName.lastIndexOf("/"));
                File f = new File(tempDir);
                if (f.exists()) {
                    directory = f;
                } else {
                    ErrorHandler.errorPanel("The output path is invalid.");
                }
            } catch (Exception ex) {
                //do nothing
            }
            if (fileName.contains("/")) {
                if (fileName.length() > fileName.lastIndexOf("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
            }
        }

        defaultExportFile = new File(directory, fileName);
        extFilter = getFilter(selectedExt);

        if (IS_MAC) {
            showFileDialog(directory.getAbsolutePath(), fileName);
        } else {
            ExportFileChooser fileChooser = new ExportFileChooser(directory, defaultExportFile, extFilter, this);
            fileChooser.setDialogTitle("Save view as...");
            fileChooser.showDialog(panel, "Select");
            if (fileChooser.getSelectedFile() != null) {
                completeBrowseButtonAction(fileChooser.getSelectedFile());
            }
        }
    }

    private void showFileDialog(String directory, String defaultFileName) {
        String ext = GeneralUtils.getExtension(defaultFileName);
        if (StringUtils.isBlank(ext)) {
            defaultFileName += selectedExt;
        }
        FileDialog dialog = new FileDialog(staticFrame, "Save Image", FileDialog.SAVE);
        //dialog.setFilenameFilter(fileNameFilter);
        dialog.setDirectory(directory);
        dialog.setFile(defaultFileName);
        dialog.setVisible(true);
        String fileS = dialog.getFile();
        if (fileS != null) {
            String fileName = dialog.getFile();
            String currentExt = GeneralUtils.getExtension(fileName);
            if (ArrayUtils.contains(EXTENSION, currentExt)) {

            } else {
                fileName += ".png";
            }
            File imageFile = new File(dialog.getDirectory(), fileName);
            completeBrowseButtonAction(imageFile);
            okButtonActionPerformed(true);

        }
    }

    private void completeBrowseButtonAction(File file) {
        String newPath = file.getAbsolutePath();
        String ext = GeneralUtils.getExtension(newPath);
        ExportFileType type = getType(getDescription(ext));
        extComboBox.setSelectedItem(type);
        resetPath(newPath);
    }

    /**
     * Reset export file and file path text field to the new path.
     *
     * @param path
     */
    private void resetPath(String path) {
        exportFile = new File(path);
        filePathTextField.setText(path);
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
     * Get export filter by passed image format extension.
     */
    private ExportFileFilter getFilter(String ext) {
        for (ExportFileFilter filter : FILTER_LIST.values()) {
            if (filter.getExtension().equals(ext)) {
                return filter;
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

    private void okButtonActionPerformed(boolean keepWindowOpen) {
        String previousPath = exportFile.getAbsolutePath();
        String newPath = filePathTextField.getText();
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

        if (exportFile.exists() && !keepWindowOpen) {
            if (!isOverwrite()) {
                return;
            }
        }

        exportScreenshot(exportComponent, exportFile, selectedExt, false);

        String des = getDescription(ext);
        extComboBox.setSelectedItem(getType(des));

        exportNode.put(PREF_FILE, path);
        exportNode.put(PREF_EXT, des);
        exportNode.put(PREF_DIR, defaultDir.getAbsolutePath());
        exportNode.putInt(PREF_RESOLUTION, imageInfo.getResolution());
        exportNode.put(PREF_UNIT, unit);

        staticFrame.setVisible(keepWindowOpen);
    }

    /**
     * Start export process when OK Button action performed.
     */
    public void okButtonActionPerformed() throws IOException {
        okButtonActionPerformed(false);
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
            filePathTextField.grabFocus();
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

    /**
     * Give the user the choice to overwrite the existing file or not.
     */
    private boolean isOverwrite() {
        String[] options = {"Yes", "No"};
        if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
                null, "Overwrite Existing File?", "File Exists",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                options, options[1])) {
            return true;
        }

        return false;
    }

    /**
     * Creates a new buffered image by exportComponent and reset label's icon.
     */
    private void previewImage() {
        exportImage = GraphicsUtil.getDeviceCompatibleImage(exportComponent.getWidth(), exportComponent.getHeight());
        Graphics2D g = exportImage.createGraphics();
        if (exportComponent instanceof JFrame) {
            drawTitleBar(g);
        }
        exportComponent.printAll(g);

        Image previewImage = GraphicsUtil.resizeImage(exportImage,
                previewLabel.getWidth(), previewLabel.getHeight());

        previewLabel.setIcon(new ImageIcon(previewImage));
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
            double newWidth = (Double) widthSpinner.getValue();
            double newHeight = newWidth * imageInfo.getHeightWidthRate();

            isWidthSpinner = true;
            heightSpinner.setValue(newHeight);
            isWidthSpinner = false;

            resetWidthHeight(newWidth, newHeight);
        }
    }

    /**
     * Reset width Spinner's value when height Spinner changed.
     */
    public void heightSpinnerStateChanged() {
        if (!isWidthSpinner) {
            double newHeight = (Double) heightSpinner.getValue();
            double newWidth = newHeight * imageInfo.getWidthHeightRate();

            isHeightSpinner = true;
            widthSpinner.setValue(newWidth);
            isHeightSpinner = false;

            resetWidthHeight(newWidth, newHeight);

        }
    }

    /**
     * Update the file path text field value when image format combo box action
     * performed.
     */
    public void extComboBoxActionPerformed() {
        String path = filePathTextField.getText();
        String ext = GeneralUtils.getExtension(path);
        selectedExt = ((ExportFileType) extComboBox.getSelectedItem()).getExtension();

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
                filePathTextField.grabFocus();
            }
        }

        if (selectedExt.equals(EXTENSION[0])) {
            mvRadioButton.setSelected(true);
            mvRadioButtonActionPerformed();
            wfRadioButton.setEnabled(false);
            resolutionComboBox.setEnabled(false);
        } else {
            wfRadioButton.setEnabled(true);
            resolutionComboBox.setEnabled(true);
        }
    }

    /**
     * Update width and height spinner's value when unit combo box action
     * performed.
     *
     * Reset width spinner will trigger to reset height spinner.
     */
    public void unitComboBoxActionPerformed() {
        unit = (String) unitComboBox.getSelectedItem();

        double newWidth = (Double) widthSpinner.getValue();
        if (!unit.equals(currentUnit)) {
            if (unit.equals(UNIT[0])) {
                newWidth *= imageInfo.getResolution();
            } else {
                newWidth /= imageInfo.getResolution();
            }
        }
        currentUnit = unit;
        widthSpinner.setValue(newWidth);
    }

    /**
     * Reset size label text by passed width and height.
     *
     * @param width
     * @param height
     */
    private void resetWidthHeight(double width, double height) {
        if (unit != null) {
            if (unit.equals(UNIT[1])) {
                // Convert back from inches to pixels

                width *= imageInfo.getResolution();
                height *= imageInfo.getResolution();
                sizeLabel.setText(String.valueOf((int) width)
                        + " x "
                        + String.valueOf((int) height)
                        + " " + UNIT[0]);
            } else {
                sizeLabel.setText(String.valueOf((int) width / imageInfo.getResolution())
                        + " x "
                        + String.valueOf((int) height / imageInfo.getResolution())
                        + " " + UNIT[1]);
            }

            imageInfo.setWidth(width);
            imageInfo.setHeight(height);
            // Allow user to reset width and height back to current size
        }
    }

    /**
     * Update output image resolution value when resolution combo box action
     * performed. If selected unit is 'inches', update width and height
     * spinner's value(reset width spinner will trigger to reset height
     * spinner).
     */
    public void resolutionComboBoxActionPerformed() {
        imageInfo.setResolution((Integer) resolutionComboBox.getSelectedItem());

        if (unit.equals(UNIT[1])) {
            double width = imageInfo.getWidth();
            width /= imageInfo.getResolution();
            widthSpinner.setValue(width);
        }
    }

    public void refreshButtonActionPerformed() {
        updatePreview();
        //refreshButton.setEnabled(false);
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
        exportComponent = slicedView;
        refreshPreview();
    }

    private void updatePreview() {
        previewImage();
    }

    private void refreshPreview() {
        initImageInfo();
        initSpinner((String) unitComboBox.getSelectedItem());
        previewImage();
    }

    public JTextField getFilePathTextField() {
        return filePathTextField;
    }

    public JSpinner getWidthSpinner() {
        return widthSpinner;
    }

    public JSpinner getHeightSpinner() {
        return heightSpinner;
    }

    public JComboBox getExtComboBox() {
        return extComboBox;
    }

    public JComboBox getResolutionComboBox() {
        return resolutionComboBox;
    }

    public JLabel getSizeLabel() {
        return sizeLabel;
    }

    public String getCurrentUnit() {
        return currentUnit;
    }

    public JComboBox getUnitComboBox() {
        return unitComboBox;
    }

    public JLabel getPreviewLabel() {
        return previewLabel;
    }

    public JButton getOkButton() {
        return okButton;
    }

    public JRadioButton getSvRadioButton() {
        return svRadioButton;
    }

    public JRadioButton getWfRadioButton() {
        return wfRadioButton;
    }

    public JRadioButton getMvRadioButton() {
        return mvRadioButton;
    }

    public JRadioButton getMvlRadioButton() {
        return mvlRadioButton;
    }

    public JPanel getButtonsPanel() {
        return buttonsPanel;
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
