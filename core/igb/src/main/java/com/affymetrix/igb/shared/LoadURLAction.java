package com.affymetrix.igb.shared;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.UniFileFilter;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.util.OptionChooserImpl;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jnicol
 */
public final class LoadURLAction extends OpenURIAction {

    private static final long serialVersionUID = 1l;
    private static final LoadURLAction ACTION = new LoadURLAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static LoadURLAction getAction() {
        return ACTION;
    }

    private LoadURLAction() {
        super(BUNDLE.getString("openURL"), null,
                "16x16/status/network-receive.png",
                "22x22/status/network-receive.png",
                KeyEvent.VK_UNDEFINED, null, true);
        this.ordinal = -9009100;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        loadURL();
    }

    private void loadURL() {
        JOptionPane pane = new JOptionPane("Enter URL", JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        final JTextField urlTextField = new JTextField();

        List<UniFileFilter> filters = getSupportedFiles(FileTypeCategory.Sequence);
        Set<String> all_known_endings = new HashSet<>();
        for (UniFileFilter filter : filters) {
            all_known_endings.addAll(filter.getExtensions());
        }
        final UniFileFilter seq_ref_filter = new UniFileFilter(all_known_endings.toArray(new String[all_known_endings.size()]), "Known Types");
        final UniFileFilter all_known_types = getAllKnowFilter();

        final OptionChooserImpl optionChooser = new OptionChooserImpl();
        optionChooser.refreshSpeciesList();
        String clipBoardContent = GeneralUtils.getClipboard();
        if (LocalUrlCacher.isURL(clipBoardContent)) {
            urlTextField.setText(clipBoardContent);
            checkLoadSeqCB(false, clipBoardContent, seq_ref_filter, optionChooser);
        }

        urlTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent de) {
                checkLoadSeqCB(true, urlTextField.getText(), seq_ref_filter, optionChooser);
            }

            @Override
            public void insertUpdate(DocumentEvent de) {
                checkLoadSeqCB(true, urlTextField.getText(), seq_ref_filter, optionChooser);
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                checkLoadSeqCB(true, urlTextField.getText(), seq_ref_filter, optionChooser);
            }
        });

        pane.setMessage(new Object[]{"Enter URL", urlTextField});

        String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();
        AnnotatedSeqGroup loadGroup = GenometryModel.getInstance().getSelectedSeqGroup();

        if (!SELECT_SPECIES.equals(speciesName) && loadGroup != null) {
            JDialog dialog = pane.createDialog(igbService.getFrame(), BUNDLE.getString("openURL"));
            dialog.setModal(true);
            dialog.getContentPane().add(optionChooser, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setLocationRelativeTo(igbService.getFrame());
            dialog.setVisible(true);
        } else {
            ErrorHandler.errorPanel(BUNDLE.getString("noGenomeSelectedTitle"),
                    BUNDLE.getString("noGenomeSelectedMessage"), Level.INFO);
        }

        String urlStr = urlTextField.getText();

        int result = JOptionPane.CANCEL_OPTION;
        if (pane.getValue() != null && pane.getValue() instanceof Integer) {
            result = (Integer) pane.getValue();
        }
        URL url;
        URI uri;
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            if (StringUtils.isBlank(urlStr)) {
                throw new Exception();
            }

            urlStr = urlStr.trim();
            url = new URL(urlStr);
            uri = url.toURI();
        } catch (Exception ex) {
            // verify these are valid
            ErrorHandler.errorPanel("Invalid URL", "The URL " + urlStr + " is not valid.  Please enter a valid URL", Level.SEVERE);
            return;
        }

        String friendlyName = getFriendlyName(urlStr);

        if ((!all_known_types.accept(new File(friendlyName)))) {
            ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + url, Level.WARNING);
            return;
        }

        openURI(uri, friendlyName, true, loadGroup, (String) optionChooser.getSpeciesCB().getSelectedItem(), optionChooser.getLoadAsSeqCB().isSelected());

    }

    private static String getFriendlyName(String urlStr) {
        // strip off final "/" character, if it exists.
        if (urlStr.endsWith("/")) {
            urlStr = urlStr.substring(0, urlStr.length() - 1);
        }

        //strip off all earlier slashes.
        urlStr = urlStr.substring(urlStr.lastIndexOf('/') + 1);

        return urlStr;
    }

    private void checkLoadSeqCB(boolean checkURL, String fileName, UniFileFilter filter, OptionChooserImpl optionChooser) {
        if (checkURL && LocalUrlCacher.isURL(fileName)) {
            checkLoadSeqCB(fileName, filter, optionChooser);
        } else {
            checkLoadSeqCB(fileName, filter, optionChooser);
        }
    }

    private void checkLoadSeqCB(String fileName, UniFileFilter filter, OptionChooserImpl optionChooser) {
        boolean enableLoadAsSeqCB = filter.accept(new File(fileName));
        optionChooser.getLoadAsSeqCB().setEnabled(enableLoadAsSeqCB);
        if (!enableLoadAsSeqCB) {
            optionChooser.getLoadAsSeqCB().setSelected(false);
        } // Uncheck for disabled
    }
}
