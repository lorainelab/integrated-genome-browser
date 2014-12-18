package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.view.NewGenome;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author hiralv
 */
public class NewGenomeAction extends OpenURIAction {

    private static final long serialVersionUID = 1l;

    private static final NewGenomeAction ACTION = new NewGenomeAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static NewGenomeAction getAction() {
        return ACTION;
    }

    private NewGenomeAction() {
        super(BUNDLE.getString("addNewSpecies"), BUNDLE.getString("openCustomGenomeTooltip"),
                "16x16/actions/new_genome.png", "22x22/actions/new_genome.png",
                KeyEvent.VK_UNDEFINED, null, false);
        this.ordinal = 200;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        NewGenome ng = new NewGenome();
        int reply = JOptionPane.showConfirmDialog(getSeqMapView(), ng, getText(), JOptionPane.OK_CANCEL_OPTION);
        if (reply == JOptionPane.OK_OPTION && ng.getVersionName().length() > 0 && ng.getSpeciesName().length() > 0) {
            String speciesName = SpeciesLookup.getPreferredName(ng.getSpeciesName());
            String versionName = SynonymLookup.getDefaultLookup().getPreferredName(ng.getVersionName());

            AnnotatedSeqGroup group = gmodel.addSeqGroup(versionName);
            String refSeqPath = ng.getRefSeqFile();

            if (refSeqPath != null && refSeqPath.length() > 0) {
                String fileName = getFriendlyName(refSeqPath);
                if (Constants.GENOME_TXT.equals(fileName) || Constants.MOD_CHROM_INFO_TXT.equals(fileName)) {
                    try {
                        ChromInfoParser.parse(getInputStream(refSeqPath), group, refSeqPath);
                        GenericVersion version = GeneralLoadUtils.getLocalFilesVersion(group, speciesName);
                        ServerList.getServerInstance().fireServerInitEvent(version.gServer, ServerStatus.Initialized, false);
                    } catch (Exception ex) {
                        Logger.getLogger(NewGenomeAction.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    igbService.openURI(new File(refSeqPath).toURI(), fileName, group, speciesName, true);
                }
            } else {
                GenericVersion version = GeneralLoadUtils.getLocalFilesVersion(group, speciesName);
                ServerList.getServerInstance().fireServerInitEvent(version.gServer, ServerStatus.Initialized, false);
            }

            gmodel.setSelectedSeqGroup(group);

        }
    }

    private InputStream getInputStream(String fileName) throws Exception {
        return LocalUrlCacher.getInputStream(relativeToAbsolute(fileName).toURL());
    }

    /* This method is used to convert the given file path from relative to absolute.
     */
    private URI relativeToAbsolute(String path) throws URISyntaxException {
        if (!(path.startsWith("file:") && !(path.startsWith("http:")) && !(path.startsWith("https:")) && !(path.startsWith("ftp:")))) {
            return getAbsoluteFile(path).toURI();
        }
        return new URI(path);
    }

    /*Returns the File object at given path
     */
    private File getAbsoluteFile(String path) {
        return new File(path).getAbsoluteFile();
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
}
