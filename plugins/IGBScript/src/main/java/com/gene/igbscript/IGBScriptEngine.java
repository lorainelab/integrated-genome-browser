package com.gene.igbscript;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL_SCHEME;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.igb.swing.script.ScriptManager;
import com.google.common.base.Strings;
import org.lorainelab.igb.igb.services.IgbService;
import org.lorainelab.igb.image.exporter.service.ImageExportService;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.logging.Level;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java ScriptEngine to run scripts written in the IGB scripting language.
 *
 * @see
 * http://today.java.net/pub/a/today/2006/09/21/making-scripting-languages-jsr-223-aware.html
 */
public class IGBScriptEngine implements ScriptEngine {

    private static final Logger logger = LoggerFactory.getLogger(IGBScriptEngine.class);
    private final ScriptEngineFactory igbFactory;
    private final ImageExportService imageExportService;
    private static final String SPACE = " ";
    private static final String SPACES = "\\s+";
    static final String[] EXTENSION = {".svg", ".png", ".jpeg", ".jpg"};
    private static final String BASH_HOME = "~/"; // '~/' over '~' because ~Filename is a valid file name
    private String fileName;

    public static enum ExportMode {

        MAIN("mainView"),
        MAINWITHLABELS("mainViewWithLabels"),
        SLICEDWITHLABELS("slicedViewWithLabels"),
        WHOLEFRAME("wholeFrame");
        private String name;

        ExportMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private IgbService igbService;
    private ScriptContext defaultContext;

    public IGBScriptEngine(IGBScriptEngineFactory factory, IgbService igbService, ImageExportService imageExportService) {
        this.imageExportService = imageExportService;
        this.igbService = igbService;
        this.igbFactory = factory;
        SimpleScriptContext c = new SimpleScriptContext();
        Bindings b = c.getBindings(ScriptContext.ENGINE_SCOPE);
        b.put(ENGINE, "IGB Scripting Language");
        b.put(ENGINE_VERSION, "0.2");
        b.put(LANGUAGE, "IGBScript");
        b.put(LANGUAGE_VERSION, "1.0");
        b.put(ARGV, ""); // TO DO: set correct value
        b.put(FILENAME, ""); // TO DO: set correct value
        b.put(NAME, "IGBScript");
        /*
         * I am not sure if this is correct; we need to check if
         * the name really is THREADING. I have no idea why there is
         * no constant as for the other keys
         */
        b.put("THREADING", null);
        this.defaultContext = c;
    }

    @Override
    public Object eval(String script) throws ScriptException {
        return eval(script, getContext());
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        try {
            fileName = URLDecoder.decode((String) context.getAttribute(ScriptManager.FILENAME), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            java.util.logging.Logger.getLogger(IGBScriptEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
        doActions(script);
        return null;
    }

    @Override
    public Object eval(String script, Bindings bindings) throws ScriptException {
        Bindings current = getContext().getBindings(ScriptContext.ENGINE_SCOPE);
        getContext().setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        Object result = eval(script);
        getContext().setBindings(current, ScriptContext.ENGINE_SCOPE);
        return result;
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return eval(getScriptFromReader(reader));
    }

    @Override
    public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
        return eval(getScriptFromReader(reader), scriptContext);
    }

    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        return eval(getScriptFromReader(reader), bindings);
    }

    @Override
    public void put(String key, Object value) {
        getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
    }

    @Override
    public Object get(String key) {
        return getBindings(ScriptContext.ENGINE_SCOPE).get(key);
    }

    @Override
    public Bindings getBindings(int scope) {
        return getContext().getBindings(scope);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        getContext().setBindings(bindings, scope);
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptContext getContext() {
        return defaultContext;
    }

    @Override
    public void setContext(ScriptContext context) {
        defaultContext = context;
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return igbFactory;
    }

    private static String getScriptFromReader(Reader reader) {
        try {
            StringWriter script = new StringWriter();
            int data;
            while ((data = reader.read()) != -1) {
                script.write(data);
            }
            script.flush();
            return script.toString();
        } catch (IOException ex) {
        }
        return null;
    }

    // language code
    /**
     * Read and execute the actions from the stream.
     */
    private void doActions(String scriptString) {
        try {
            String[] lines = scriptString.split("\n");
            for (String line : lines) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                // Ignore comments and blank lines.
                if (line.startsWith("#") || line.trim().length() == 0) {
                    continue;
                }

                try {
                    igbService.addNotLockedUpMsg("Executing script line: " + line);
                    doSingleAction(line);
                    // User actions don't happen instantaneously.
                    // So, give a short sleep time between batch actions.
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    logger.warn("Thread interrupted while sleeping. Cancelling the script.");
                    break;
                } finally {
                    igbService.removeNotLockedUpMsg("Executing script line: " + line);
                }
            }
        } catch (Exception ex) {
            logger.error("doActions", ex);
        }
    }

    private String extractAction(String line) {
        if (!Strings.isNullOrEmpty(line)) {
            int spaceIndex = line.indexOf(SPACE);
            if (spaceIndex > 0) {
                return line.substring(0, spaceIndex).trim();
            } else {
                return line.trim();
            }
        } else {
            throw new IllegalArgumentException("Illegal argument in script");
        }
    }

    private String extractParamString(String line) {
        if (!Strings.isNullOrEmpty(line)) {
            int spaceIndex = line.indexOf(SPACE);
            if (spaceIndex > 0) {
                return line.substring(spaceIndex).trim();
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Illegal argument in script");
        }
    }

    private String getAbsolutePath(String filePath) throws UnsupportedEncodingException {
        if (CommonUtils.IS_WINDOWS) {
            return filePath;
        }
        if (filePath.startsWith(HTTP_PROTOCOL_SCHEME)) {
            return filePath;
        }
        String scriptLocation = File.separator + fileName.substring(fileName.indexOf("/") + 1, fileName.lastIndexOf("/"));
        if (filePath.startsWith(BASH_HOME)) {
            filePath = filePath.replaceAll(BASH_HOME, System.getProperty("user.home") + File.separator);
        }
        if (!filePath.startsWith("/")) {
            filePath = scriptLocation + File.separator + filePath;
        }
        return filePath;
    }

    /**
     * Every line in script is divided into two parts. Action and params. Action
     * decide what operations should be performed on params. Assumptions: Action
     * cannot have spaces. First param of load mode will not have any space.
     *
     * @param line
     */
    public void doSingleAction(String line) {
        logger.info("doSingleAction line: {}", line);
        line = line.replaceAll(SPACES, SPACE);
        try {
            String action = extractAction(line);
            String paramString = extractParamString(line);
            if (action.equalsIgnoreCase("genome") && !Strings.isNullOrEmpty(paramString)) {
                // go to genome
                goToGenome(paramString);
                return;
            }
            if (action.equalsIgnoreCase("goto") && !Strings.isNullOrEmpty(paramString)) {
                // go to region
                goToRegion(paramString);
                return;
            }
            if (action.equalsIgnoreCase("load")) {
                // Allowing multiple files to be specified, split by commas
                String[] loadFiles = paramString.split(",");
                for (int i = 0; i < loadFiles.length; i++) {
                    if (i > 0) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {
                            logger.error("doSingleAction waiting", ex);
                        }
                    }
                    loadFile(getAbsolutePath(loadFiles[i]));
                }
                return;
            }
            if (action.equalsIgnoreCase("unload")) {
                // Allowing multiple files to be specified, split by commas
                String[] loadFiles = paramString.split(",");
                for (int i = 0; i < loadFiles.length; i++) {
                    if (i > 0) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            logger.error("error while running unload action", ex);
                        }
                    }
                    unLoadFile(getAbsolutePath(loadFiles[i]));
                }
                return;
            }
            if (action.equalsIgnoreCase("loadmode")) {
                String mode = paramString.substring(0, paramString.indexOf(SPACE));
                String featureUri = paramString.substring(paramString.indexOf(SPACE) + 1);
                if (Strings.isNullOrEmpty(mode) || Strings.isNullOrEmpty(featureUri)) {
                    return;
                }
                loadMode(mode, featureUri);
                return;
            }
            if (action.equalsIgnoreCase("print")) {
                if (Strings.isNullOrEmpty(paramString)) {
                    try {
                        igbService.print(0, true);
                    } catch (Exception ex) {
                        ErrorHandler.errorPanel("Problem trying to print.", ex, Level.SEVERE);
                    }
                }
                return;
            }
            if (action.equalsIgnoreCase("refresh")) {
                igbService.loadVisibleFeatures();
                return;
            }
            if (action.equalsIgnoreCase("select") && !Strings.isNullOrEmpty(paramString)) {
                igbService.performSelection(paramString);
                return;
            }
            if (action.equalsIgnoreCase("selectfeature") && !Strings.isNullOrEmpty(paramString)) {
                igbService.selectFeatureAndCenterZoomStripe(paramString);
                return;
            }
            if (action.equalsIgnoreCase("setZoomStripePosition") && !Strings.isNullOrEmpty(paramString)) {
                double position = Double.parseDouble(paramString);
                igbService.getSeqMapView().setZoomSpotX(position);
                igbService.getSeqMapView().setZoomSpotY(0);
                return;
            }
            if (action.equals("sleep") && !Strings.isNullOrEmpty(paramString)) {
                try {
                    int sleepTime = Integer.parseInt(paramString);
                    Thread.sleep(sleepTime);
                } catch (Exception ex) {
                    logger.error("doActions", ex);
                }
                return;
            }
            if (action.equalsIgnoreCase("hidetrack")) {
                // Allowing multiple files to be specified, split by commas
                String[] hideTrack = paramString.split(",");
                for (String aHideTrack : hideTrack) {
                    hideTrack(getAbsolutePath(aHideTrack));
                }
                return;
            }
            if (action.equalsIgnoreCase("showtrack")) {
                // Allowing multiple files to be specified, split by commas
                String[] showTrack = paramString.split(",");
                for (String aShowTrack : showTrack) {
                    showTrack(getAbsolutePath(aShowTrack));
                }
                return;
            }
            if (action.startsWith("snapshot")) {
                // determine the export mode
                action = action.substring(8, action.length());
                IGBScriptEngine.ExportMode exportMode = IGBScriptEngine.ExportMode.WHOLEFRAME;
                if (action.length() == 0 || action.equalsIgnoreCase(IGBScriptEngine.ExportMode.WHOLEFRAME.toString())) {
                    exportMode = IGBScriptEngine.ExportMode.WHOLEFRAME;
                } else if (action.equalsIgnoreCase(IGBScriptEngine.ExportMode.MAIN.toString())) {
                    exportMode = IGBScriptEngine.ExportMode.MAIN;
                } else if (action.equalsIgnoreCase(IGBScriptEngine.ExportMode.MAINWITHLABELS.toString())) {
                    exportMode = IGBScriptEngine.ExportMode.MAINWITHLABELS;
                } else if (action.equalsIgnoreCase(IGBScriptEngine.ExportMode.SLICEDWITHLABELS.toString())) {
                    exportMode = IGBScriptEngine.ExportMode.SLICEDWITHLABELS;
                }

                // determine the file name, and export.
                if (!Strings.isNullOrEmpty(paramString)) {
                    snapShot(exportMode, new File(getAbsolutePath(paramString)));	// second field and possibly others are a single filename
                } else {
                    // base filename upon organism and timestamp
                    String id = GenometryModel.getInstance().getSelectedGenomeVersion() == null ? "default"
                            : GenometryModel.getInstance().getSelectedGenomeVersion().getName();
                    snapShot(exportMode, new File(id + System.currentTimeMillis() + ".gif"));
                }
                return;
            }

            if (action.startsWith("homescreen")) {
                igbService.setHome();
                return;
            }
            if (action.equalsIgnoreCase("bringToFront")) {
                igbService.bringToFront();
                return;
            }
            if (action.equalsIgnoreCase("LoadSequence")) {
                igbService.loadResidues(true);
                return;
            }

            logger.warn("Unrecognized or invalid command: {}", action);
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            logger.warn("Illegal argument in script", e);
        }
    }

    /**
     * Take a snapshot, i.e., export to a file.
     *
     * @param f
     */
    private void snapShot(IGBScriptEngine.ExportMode exportMode, File f) {
        logger.info("Exporting file {} in mode: {}", new Object[]{f.getName(), exportMode.toString()});
        String ext = GeneralUtils.getExtension(f.getName().toLowerCase());
        if (ext.length() == 0) {
            logger.error("no file extension given for file", f.getName());
            return;
        }

        if (!isExt(ext)) {
            logger.error("image file extension {0} is not supported", ext);
            return;
        }

        try {
            Component c = null;
            switch (exportMode) {
                case WHOLEFRAME:
                    c = igbService.getApplicationFrame();
                    break;
                case MAIN:
                    c = igbService.getMainViewComponent();
                    break;
                case MAINWITHLABELS:
                    c = igbService.getMainViewComponentWithLabels();
                    break;
                case SLICEDWITHLABELS:
                    c = igbService.getSpliceViewComponentWithLabels();
                    break;
            }

            imageExportService.headlessComponentExport(c, f, ext, true);
        } catch (Exception ex) {
            logger.error("ImageExport failed", ex);
        }
    }

    private void goToGenome(String genomeVersion) {
        GenomeVersion group = igbService.determineAndSetGroup(genomeVersion).orElse(null);
        if (group == null) {
            return;
        }
        for (int i = 0; i < 100; i++) {
            // sleep until versions are initialized
            for (DataContainer version : group.getAvailableDataContainers()) {
                if (version.isInitialized()
                        && group == GenometryModel.getInstance().getSelectedGenomeVersion()) {
                    continue;
                }
                try {
                    Thread.sleep(300); // not finished initializing versions
                } catch (InterruptedException ex) {
                    logger.error("goToGenome", ex);
                }
            }
        }
    }

    private void goToRegion(String region) {
        igbService.goToRegion(region);
    }

    private void loadData(String serverURIorName, String feature_url) {
        Optional<DataProvider> server = igbService.loadServer(serverURIorName);
        Optional<DataSet> feature = igbService.getDataSet(
                GenometryModel.getInstance().getSelectedGenomeVersion(),
                server.get(), feature_url, true);

        if (feature.isPresent()) {
            feature.get().setVisible();
            feature.get().setPreferredLoadStrategy(LoadStrategy.VISIBLE);
        }

        igbService.refreshDataManagementView();
    }

    private void loadFile(String fileName) {
        URI uri;
        File f = new File(fileName.trim());
        if (fileName.startsWith(HTTP_PROTOCOL_SCHEME)) {
            try {
                uri = new URI(fileName);
            } catch (URISyntaxException ex) {
                logger.error("loadFile", ex);
                return;
            }
        } else {
            uri = f.toURI();
        }
        GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
        if (fileName.contains(File.separator)) {
            fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
        } else {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        igbService.openURI(uri, fileName, genomeVersion, genomeVersion.getSpeciesName(), false);
    }

    private void unLoadFile(String fileName) {
        URI uri;
        File f = new File(fileName.trim());
        if (fileName.startsWith(HTTP_PROTOCOL_SCHEME)) {
            try {
                uri = new URI(fileName);
            } catch (URISyntaxException ex) {
                logger.error("loadFile", ex);
                return;
            }
        } else {
            uri = f.toURI();
        }
        igbService.deleteTrack(uri);
    }

    private void hideTrack(String fileName) {
        ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(fileName);
        style.setShow(false);
        igbService.packMap(false, false);
        //	loadMode("no_load",fileName);
        igbService.updateGeneralLoadView();

    }

    private void showTrack(String fileName) {
        ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(fileName);
        style.setShow(true);
        igbService.packMap(false, true);
        igbService.updateGeneralLoadView();

    }

    private void loadMode(String loadMode, String featureURIStr) {

        URI featureURI;
        File featureFile = new File(featureURIStr.trim());
        if (featureFile.exists()) {
            featureURI = featureFile.toURI();
        } else {
            featureURI = URI.create(featureURIStr.trim());
        }
        LoadStrategy s = LoadStrategy.NO_LOAD;
        if (loadMode.equalsIgnoreCase("no_load")) {
            s = LoadStrategy.NO_LOAD;
        } else if (loadMode.equalsIgnoreCase("region_in_view")) {
            s = LoadStrategy.VISIBLE;
        } else if (loadMode.equalsIgnoreCase("genome")) {
            s = LoadStrategy.GENOME;
        }

        // First try to look up for feature in current group.
        GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
        DataSet feature = null;

        // If feature is not found in current group then look up all groups.
        if (genomeVersion != null) {
            feature = findFeatureInGroup(genomeVersion, featureURI);
        }

        if (feature == null) {
            for (GenomeVersion group : GenometryModel.getInstance().getSeqGroups().values()) {
                feature = findFeatureInGroup(group, featureURI);
                if (feature != null) {
                    break;
                }
            }
        }

        if (feature != null) {
            feature.setPreferredLoadStrategy(s);
            igbService.refreshDataManagementView();
            if (s == LoadStrategy.GENOME) {
                igbService.loadAndDisplayAnnotations(feature);
            }
        } else {
            logger.error("Could not find feature: {}", featureURIStr);
        }
    }

    private DataSet findFeatureInGroup(GenomeVersion genomeVersion, URI featureURI) {
        DataSet feature = null;
        for (DataContainer version : genomeVersion.getAvailableDataContainers()) {
            feature = igbService.findFeatureWithURI(version, featureURI);
            if (feature != null) {
                break;
            }
        }

        return feature;
    }

    /**
     * Return whether the passed extention is contained in IGB support image
     * extention list or not.
     */
    private static boolean isExt(String ext) {
        for (String s : EXTENSION) {
            if (s.equalsIgnoreCase(ext)) {
                return true;
            }
        }

        return false;
    }
}
