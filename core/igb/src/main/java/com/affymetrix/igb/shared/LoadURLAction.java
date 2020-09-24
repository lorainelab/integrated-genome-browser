package com.affymetrix.igb.shared;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.UniFileFilter;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.shared.OpenURIAction.getFriendlyName;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jnicol
 */
public class LoadURLAction extends OpenURIAction {

    private static final long serialVersionUID = 1L;
    private static final LoadURLAction ACTION = new LoadURLAction();
    private boolean mergeSelected = false;

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
        setKeyStrokeBinding("ctrl shift O");
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
        filters.stream().forEach((filter) -> {
            all_known_endings.addAll(filter.getExtensions());
        });
        final UniFileFilter all_known_types = getAllSupportedExtensionsFilter();

        String clipBoardContent = GeneralUtils.getClipboard();
        if (LocalUrlCacher.isURL(clipBoardContent)) {
            urlTextField.setText(clipBoardContent);
        }

        pane.setMessage(new Object[]{"Enter URL", urlTextField});
        String speciesName = getSpeciesName();
        GenomeVersion loadGroup = getGenomeVersion();

        // IGBF-1620 Start
        JDialog dialog = pane.createDialog(igbService.getApplicationFrame(), BUNDLE.getString("openURL"));
        dialog.setModal(true);
        dialog.setMinimumSize(new Dimension(450,150));
        dialog.pack();
        dialog.setLocationRelativeTo(igbService.getApplicationFrame());
        dialog.setResizable(true);
        dialog.setVisible(true);
        
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
        
        boolean isReferenceSequence = false;   
        
        String friendlyName = getFriendlyName(urlStr);

        if ((!all_known_types.accept(new File(friendlyName)))) {
            ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + url, Level.WARNING);
            return;
        }
        
        // If no genome available, open URL should not work for BAI input type
        if(gmodel.getSelectedGenomeVersion()==null && "bai".equals(urlStr.substring(urlStr.length()-3,urlStr.length()))){
                ErrorHandler.errorPanel(BUNDLE.getString("noGenomeSelectedTitle"),
                        BUNDLE.getString("noGenomeSelectedMessage"), Level.INFO);
                return;
        }
        
        openURI(uri, friendlyName, mergeSelected, loadGroup, speciesName, isReferenceSequence);
    }
    
    /* 
    IGBF-1620 - Pulled from LoadFileAction to Deal with Null
    Genome Version when a Reference Genome is not selected before import
    */
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
    
    // To get the Species Name When a Genome is not specified
    private String getSpeciesName() {
        String speciesName = igbService.getSelectedSpecies();
        if (SELECT_SPECIES.equals(speciesName)) {
            speciesName = UNKNOWN_SPECIES_PREFIX + " " + CUSTOM_GENOME_COUNTER;
        }
        return speciesName;
    }
    // IGBF-1620 End
}

