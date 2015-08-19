package com.lorainelab.igb.preferences.weblink;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.lorainelab.igb.preferences.IgbPreferencesService;
import com.lorainelab.igb.preferences.model.AnnotationUrl;
import com.lorainelab.igb.preferences.model.IgbPreferences;
import com.lorainelab.igb.preferences.model.JsonWrapper;
import com.lorainelab.igb.preferences.weblink.model.WebLink;
import com.lorainelab.igb.preferences.weblink.model.WebLinkList;
import com.lorainelab.synonymlookup.services.SpeciesSynonymsLookup;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hiralv
 */
@Component(name = WebLinkUtils.COMPONENT_NAME, immediate = true)
public class WebLinkUtils implements WebLinkExporter {

    public static final String COMPONENT_NAME = "WebLinkUtils";
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebLinkUtils.class);
    private static final String OLD_WEBLINKS_FILE_NAME = "weblinks.xml";
    private static final String WEBLINKS_FILE_NAME = "weblinks.json";
    static final WebLinkList LOCAL_WEBLINK_LIST = new WebLinkList("local", true);
    static final WebLinkList SERVER_WEBLINK_LIST = new WebLinkList("default", false);
    //TODO this should not be static, but must be during this refactoring to avoid larger changes
    private static IgbPreferencesService igbPreferencesService;
    private SpeciesSynonymsLookup speciesSynLookup;
    
    public WebLinkUtils() {

    }

    /**
     * Returns the file that is used to store the user-edited web links.
     */
    private static File getOldLinksFile() {
        return new File(PreferenceUtils.getAppDataDirectory(), OLD_WEBLINKS_FILE_NAME);
    }

    private static File getLinksFileJson() {
        return new File(PreferenceUtils.getAppDataDirectory(), WEBLINKS_FILE_NAME);
    }

    @Activate
    public void activate() {
        LOCAL_WEBLINK_LIST.setSpeciesSynLookup(speciesSynLookup);
        SERVER_WEBLINK_LIST.setSpeciesSynLookup(speciesSynLookup);
        initializeDefaultWebLinks();
        File f = getLinksFileJson();
        if (f == null || !f.exists()) {
            f = getOldLinksFile();
            if (f == null || !f.exists()) {
                return;
            }
        }

        String filename = f.getAbsolutePath();
        try {
            logger.debug("Loading web links from file {}", filename);
            importWebLinks(f);
        } catch (Exception ioe) {
            logger.error("Could not load web links from file {}", filename);
        }
    }

    private void initializeDefaultWebLinks() {
        Optional<IgbPreferences> defaultPreferences = igbPreferencesService.fromDefaultPreferences();
        if (defaultPreferences.isPresent()) {
            defaultPreferences.get().getAnnotationUrl().stream().map((url) -> new WebLink(url)).forEach((webLink) -> {
                getWebLinkList(webLink.getType()).addWebLink(webLink);
            });
        }
    }

    @Override
    public void exportUserWebLinks() {
        File f = getLinksFileJson();
        String filename = f.getAbsolutePath();
        try {
            logger.info(
                    "Saving web links to file \"{}\"", filename);
            File parent_dir = f.getParentFile();
            if (parent_dir != null) {
                parent_dir.mkdirs();
            }
            exportWebLinks(f);
        } catch (IOException ioe) {
            logger.error(
                    "Error while saving web links to \"{}\"", filename);
        }
    }

    //not a fan of doing this import from WebLinkUtils, but this is legacy code
    //TODO consider passing this responsibility to preference loading orchestrator
    public static void importWebLinks(File f) throws IOException {
        String ext = com.google.common.io.Files.getFileExtension(f.getAbsolutePath());
        Optional<IgbPreferences> igbPreferences;
        //Eventually this can be removed, but for now we will continue to support xml format
        if (ext.equals("xml")) {
            try (Reader reader = new FileReader(f);) {
                igbPreferences = igbPreferencesService.fromXml(reader);
            }
        } else {
            try (Reader reader = new FileReader(f);) {
                igbPreferences = igbPreferencesService.fromJson(reader);
            }
        }
        if (igbPreferences.isPresent()) {
            igbPreferences.get().getAnnotationUrl().stream().map((url) -> new WebLink(url)).forEach((webLink) -> {
                getWebLinkList(webLink.getType()).addWebLink(webLink);
            });
        }
    }

    public static void exportWebLinks(File f) throws IOException {

        try (FileWriter fw = new FileWriter(f);
                BufferedWriter bw = new BufferedWriter(fw);) {
            List<AnnotationUrl> annotationUrls = new ArrayList<>();
            for (WebLink link : getLocalList().getWebLinkList()) {
                AnnotationUrl url = new AnnotationUrl();
                if (link.getRegexType() == WebLink.RegexType.ANNOTATION_NAME) {
                    url.setAnnotTypeRegex(link.getRegex());
                } else {
                    url.setAnnotIdRegex(link.getRegex());
                }
                url.setIdField(link.getIDField());
                url.setImageIconPath(link.getImageIconPath());
                url.setName(link.getName());
                url.setSpecies(link.getSpeciesName());
                url.setType(link.getType());
                url.setUrl(link.getUrl());
                annotationUrls.add(url);
            }
            JsonWrapper jsonWrapper = new JsonWrapper();
            IgbPreferences prefs = new IgbPreferences();
            prefs.setAnnotationUrl(annotationUrls);
            jsonWrapper.setPrefs(prefs);
            bw.write(igbPreferencesService.toJson(jsonWrapper));
        }
    }

    public static WebLinkList getServerList() {
        return SERVER_WEBLINK_LIST;
    }

    public static WebLinkList getLocalList() {
        return LOCAL_WEBLINK_LIST;
    }

    public static WebLinkList getWebLinkList(String type) {
        if (SERVER_WEBLINK_LIST.getName().equalsIgnoreCase(type)) {
            return SERVER_WEBLINK_LIST;
        }
        return LOCAL_WEBLINK_LIST;
    }

    @Reference(optional = false)
    public void setIgbPreferencesService(IgbPreferencesService igbPreferencesService) {
        WebLinkUtils.igbPreferencesService = igbPreferencesService;
    }

    @Reference
    public void setSpeciesSynLookup(SpeciesSynonymsLookup speciesSynLookup) {
        this.speciesSynLookup = speciesSynLookup;
    }
    
}
