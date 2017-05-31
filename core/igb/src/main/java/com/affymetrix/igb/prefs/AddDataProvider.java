/*
 * AddDataProvider.java
 *
 * Created on Dec 30, 2011, 12:26:34 PM
 */
package com.affymetrix.igb.prefs;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.data.DataProviderFactoryManager;
import static com.affymetrix.genometry.data.DataProviderUtils.toExternalForm;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.genometry.util.LoadUtils;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.igb.EventService;
import com.affymetrix.igb.general.DataProviderManager;
import com.affymetrix.igb.general.DataProviderManager.DataProviderServiceChangeEvent;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.JRPTextField;
import com.google.common.base.Strings;
import java.awt.Point;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.lorainelab.igb.javafx.DirectoryChooserUtil;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

@aQute.bnd.annotation.component.Component(name = AddDataProvider.COMPONENT_NAME, immediate = true, provide = AddDataProvider.class)
public class AddDataProvider extends JFrame {

    public static final String COMPONENT_NAME = "AddDataProvider";
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AddDataProvider.class);
    private static final long serialVersionUID = 1L;

    private boolean isEditPanel;

    private DataProviderFactoryManager dataProviderFactoryManager;
    private DataProviderManager dataProviderManager;
    private DataProvider dataProvider;
    private EventService eventService;
    private BundleContext bundleContext;

    public AddDataProvider() {
    }

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        eventService.getEventBus().register(this);
        initComponents();
        DocumentListener dl = new MyDocumentListener();
        nameText.getDocument().addDocumentListener(dl);
        urlText.getDocument().addDocumentListener(dl);
    }

    @Reference
    public void setDataProviderFavotryManager(DataProviderFactoryManager dataProviderFactoryManager) {
        this.dataProviderFactoryManager = dataProviderFactoryManager;
    }

    @Reference
    public void setDataProviderManager(DataProviderManager dataProviderManager) {
        this.dataProviderManager = dataProviderManager;
    }

    @Reference
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    private void checkFieldsChange() {
        if (nameText.getText().trim().isEmpty() || urlText.getText().trim().isEmpty()) {
            addServerButton.setEnabled(false);
            return;
        }
        addServerButton.setEnabled(true);
    }

    private class MyDocumentListener implements DocumentListener {

        public void changedUpdate(DocumentEvent e) {
            checkFieldsChange();
        }

        public void insertUpdate(DocumentEvent e) {
            checkFieldsChange();
        }

        public void removeUpdate(DocumentEvent e) {
            checkFieldsChange();
        }
    }

    public void init(boolean isEditP, String title, DataProvider dataProvider) {
        isEditPanel = isEditP;
        if (isEditPanel) {
            this.dataProvider = dataProvider;
            nameText.setText(dataProvider.getName());
            typeCombo.setSelectedItem(dataProvider.getFactoryName().get());
            typeCombo.setEnabled(false);
            urlText.setText(dataProvider.getUrl());
            addServerButton.setText("Save Changes");
        } else {
            typeCombo.setEnabled(true);
            nameText.setText("Your server name");
            addServerButton.setText("Submit");
            urlText.setText("http://");
        }

        setTitle(title);
        resetTypeLabelAndCombo();
        display();
    }

    private void resetTypeLabelAndCombo() {
        typeLabel.setVisible(true);
        typeCombo.setVisible(true);
    }

    private void display() {
        JFrame frame = PreferencesPanel.getSingleton().getFrame();
        Point location = frame.getLocation();
        setLocation(location.x + frame.getWidth() / 2 - getWidth() / 2,
                location.y + getHeight() / 2 - getHeight() / 2);
        setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        nameLabelField = new javax.swing.JLabel();
        nameText = new JRPTextField("ServerPrefsView_name", "Your server nameText");
        typeLabel = new javax.swing.JLabel();
        typeCombo = new javax.swing.JComboBox();
        urlLabelField = new javax.swing.JLabel();
        urlText = new JRPTextField("ServerPrefsView_url", "http://");
        openDir = new JRPButton("DataLoadPrefsView_openDir", "\u2026");
        cancelButton = new javax.swing.JButton();
        addServerButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        nameLabelField.setText("Name:");

        typeLabel.setText("Type:");

        typeCombo = new JComboBox(dataProviderFactoryManager.getAllAvailableFactoryTypeNames().toArray());
        typeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboActionPerformed(evt);
            }
        });

        urlLabelField.setText("URL:");

        openDir.setText("Choose local folder");
        openDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDirActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        addServerButton.setText("Submit");
        addServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addServerButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(12, 12, 12)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(nameLabelField)
                            .add(typeLabel)
                            .add(urlLabelField))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(urlText)
                            .add(typeCombo, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(nameText)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .addContainerGap(136, Short.MAX_VALUE)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, openDir)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .add(cancelButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(addServerButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 150, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))
                .add(8, 8, 8))
        );

        layout.linkSize(new java.awt.Component[] {addServerButton, openDir}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(24, 24, 24)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(nameLabelField)
                    .add(nameText, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(typeLabel)
                    .add(typeCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(urlLabelField)
                    .add(urlText, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(openDir)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(addServerButton)
                    .add(cancelButton))
                .addContainerGap())
        );

        openDir.setToolTipText("Open Local Directory");

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void openDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDirActionPerformed

            // <Ashwini Kadam> IGBF-1140
            File f = dirChooser();
            
            if (f != null && f.isDirectory()) {
                try {
                    urlText.setText(URLDecoder.decode(f.toURI().toURL().toString(), "UTF-8"));
                } catch (MalformedURLException ex) {
                    LOG.error(ex.getMessage(), ex);
                } catch (UnsupportedEncodingException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
	}//GEN-LAST:event_openDirActionPerformed

	private void typeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboActionPerformed
            Optional<DataProviderFactory> factory = dataProviderFactoryManager.findFactoryByName((String) typeCombo.getSelectedItem());
            openDir.setEnabled(factory.get().supportsLocalFileInstances());
	}//GEN-LAST:event_typeComboActionPerformed

	private void addServerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addServerButtonActionPerformed

            CThreadWorker<Boolean, Void> worker;
            worker = new CThreadWorker<Boolean, Void>("Adding " + nameText.getText().trim()) {
                boolean isUnavailable = false;

                @Override
                protected Boolean runInBackground() {
                    DataProvider updatedDataProvider = null;
                    if (isEditPanel) {
                        String existingName = dataProvider.getName();
                        final String updatedName = nameText.getText().trim();
                        String existingUrl = toExternalForm(dataProvider.getUrl());
                        final String updatedUrl = toExternalForm(urlText.getText().trim());
                        if (!existingName.equals(updatedName)) {
                            dataProvider.setName(updatedName);
                        }
                        if (!existingUrl.equalsIgnoreCase(updatedUrl)) {
                            if (dataProvider.getFactoryName().isPresent()) {
                                Optional<DataProviderFactory> dataProviderFactory = dataProviderFactoryManager.findFactoryByName(dataProvider.getFactoryName().get());
                                if (dataProviderFactory.isPresent()) {
                                    updatedDataProvider = dataProviderFactory.get().createDataProvider(updatedUrl, updatedName, dataProvider.getLoadPriority());
                                }
                            }
                            dataProviderManager.removeDataProvider(dataProvider);
                            dataProvider = updatedDataProvider;
                            Timer timer = new Timer(500, evt -> {
                                if (dataProvider != null) {
                                    dataProviderManager.addDataProvider(dataProvider);
                                }
                            });
                            timer.setRepeats(false);
                            timer.start();

                        }
                        Timer timer = new Timer(2000, event -> {
                            if (dataProvider.getStatus() == LoadUtils.ResourceStatus.NotResponding) {
                                ModalUtils.infoPanel("Your Data Source is not responding, please confirm you have entered everything correctly.");
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        String url = urlText.getText().trim();
                        String name = nameText.getText().trim();
                        if (!Strings.isNullOrEmpty(url) || !Strings.isNullOrEmpty(name)) {
                            Optional<DataProviderFactory> factory = dataProviderFactoryManager.findFactoryByName((String) typeCombo.getSelectedItem());
                            if (factory.isPresent()) {
                                DataProvider createdDataProvider = factory.get().createDataProvider(url, name, -1);
                                dataProviderManager.addDataProvider(createdDataProvider);
                                if (createdDataProvider.getStatus() == LoadUtils.ResourceStatus.NotResponding) {
                                    isUnavailable = true;
                                }
                            }
                        }
                    }
                    return true;
                }

                @Override

                protected void finished() {
                    boolean serverAdded = true;
                    try {
                        serverAdded = get();
                    } catch (InterruptedException | ExecutionException ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
                    GenometryModel gmodel = GenometryModel.getInstance();
                    GenomeVersion genomeVersion = gmodel.getSelectedGenomeVersion();
                    if (serverAdded && genomeVersion != null) {
                        ModalUtils.infoPanel("<html>Your data source <b>" + nameText.getText().trim() + "</b> is now available in <b>Data Access Tab</b> under <b>Available Data</b>.</html>", "", false);
                    }
                    if (isUnavailable) {
                        ModalUtils.infoPanel("Your newly added Data Source is not responding, please confirm you have entered everything correctly.");
                    }
                }
            };

            Optional<DataProvider> server = dataProviderManager.getServerFromUrl(urlText.getText().trim());
            if (!server.isPresent() || isEditPanel) {
                CThreadHolder.getInstance().execute(evt, worker);
            } else {
                ModalUtils.infoPanel("<html>The server <i color=blue>" + server.get().getUrl() + "</i> has already been added. </html>");
            }
            this.setVisible(false);
            eventService.getEventBus().post(new DataProviderServiceChangeEvent());
	}//GEN-LAST:event_addServerButtonActionPerformed

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
            this.setVisible(false);
	}//GEN-LAST:event_cancelButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addServerButton;
    private static javax.swing.JButton cancelButton;
    private static javax.swing.JLabel nameLabelField;
    private static javax.swing.JTextField nameText;
    private static javax.swing.JButton openDir;
    private static javax.swing.JComboBox typeCombo;
    private static javax.swing.JLabel typeLabel;
    private static javax.swing.JLabel urlLabelField;
    private static javax.swing.JTextField urlText;
    // End of variables declaration//GEN-END:variables

    // <Ashwini Kadam> IGBF-1140
    // JFileChooser displays JavaFX Swing version of file chooser. 
    // To diplsay operating system's native file chooser, I am using class FileChooser.
    // DirectoryChooserUtil (based on FileChooserUtil) opens and allows user to
    // choose only directory (and not file)
    protected static File dirChooser() {
        FileTracker fileTracker = FileTracker.DATA_DIR_TRACKER;
        File dir = null;
        Optional<File> selectedDir = DirectoryChooserUtil.build()
                .setContext(fileTracker.getFile())
                .setTitle("Choose Local Folder")
                .retrieveDirFromFxChooser();
        
        if (selectedDir.isPresent()) {
            dir = selectedDir.get();
            fileTracker.setFile(dir.getParentFile());
        }
        
        return dir;
    }
}
