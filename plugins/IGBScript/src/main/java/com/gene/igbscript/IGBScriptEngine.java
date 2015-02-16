package com.gene.igbscript;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL_SCHEME;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.lorainelab.igb.service.api.IgbService;
import com.affymetrix.igb.shared.HeadLessExport;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.*;

/**
 * Java ScriptEngine to run scripts written in the IGB scripting language.
 *
 * @see
 * http://today.java.net/pub/a/today/2006/09/21/making-scripting-languages-jsr-223-aware.html
 */
public class IGBScriptEngine implements ScriptEngine {

    private final ScriptEngineFactory igbFactory;

    private static String splitter = "\\s";
    static final String[] EXTENSION = {".svg", ".png", ".jpeg", ".jpg"};

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

    private static IgbService igbService;
    private static final Logger LOG
            = Logger.getLogger(IGBScriptEngine.class.getPackage().getName());

    static {
//		LOG.setLevel(Level.FINER); // FINER or FINEST for debugging.
        LOG.addHandler(new ConsoleHandler());
    }

    private ScriptContext defaultContext;

    public IGBScriptEngine(IGBScriptEngineFactory factory, IgbService igbService) {
        super();
        IGBScriptEngine.igbService = igbService;
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
                    LOG.log(Level.WARNING, "Thread interrupted while sleeping. Cancelling the script.");
                    break;
                } finally {
                    igbService.removeNotLockedUpMsg("Executing script line: " + line);
                }
            }
        } catch (Exception ex) {
            LOG.logp(Level.SEVERE, this.getClass().getName(), "doActions", "", ex);
        }
    }

    public void doSingleAction(String line) {
        LOG.logp(Level.INFO, this.getClass().getName(), "doSingleAction",
                "line: {0}", line);
        String[] fields = line.split(splitter);
        String action = fields[0].toLowerCase();
        if (action.equalsIgnoreCase("genome") && fields.length >= 2) {
            // go to genome
            goToGenome(join(fields, 1));
            return;
        }
        if (action.equalsIgnoreCase("goto") && fields.length >= 2) {
            // go to region
            goToRegion(join(fields, 1));
            return;
        }
        if (action.equalsIgnoreCase("load")) {
            // Allowing multiple files to be specified, split by commas
            String[] loadFiles = join(fields, 1).split(",");
            for (int i = 0; i < loadFiles.length; i++) {
                if (i > 0) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        LOG.logp(Level.SEVERE, this.getClass().getName(),
                                "doSingleAction", "waiting", ex);
                    }
                }
                loadFile(loadFiles[i]);
            }
            return;
        }
        if (action.equalsIgnoreCase("loadfromserver")) {
            if (fields.length >= 2) {
                loadData(fields[1], join(fields, 2));
                return;
            }
            return;
        }
        if (action.equalsIgnoreCase("loadmode")) {
            if (fields.length >= 2) {
                loadMode(fields[1], join(fields, 2));
            }
            return;
        }
        if (action.equalsIgnoreCase("print")) {
            if (fields.length == 1) {
                try {
                    igbService.print(0, true);
                } catch (Exception ex) {
                    ErrorHandler.errorPanel("Problem trying to print.", ex, Level.SEVERE);
                }
                return;
            }
            return;
        }
        if (action.equalsIgnoreCase("refresh")) {
            igbService.loadVisibleFeatures();
            return;
        }
        if (action.equalsIgnoreCase("select") && fields.length >= 2) {
            igbService.performSelection(join(fields, 1));
            return;
        }
        if (action.equalsIgnoreCase("selectfeature") && fields.length >= 2) {
            igbService.selectFeatureAndCenterZoomStripe(join(fields, 1));
            return;
        }
        if (action.equalsIgnoreCase("setZoomStripePosition") && fields.length >= 2) {
            double position = Double.parseDouble(join(fields, 1));
            igbService.getSeqMapView().setZoomSpotX(position);
            igbService.getSeqMapView().setZoomSpotY(0);
            return;
        }
        if (action.equals("sleep") && fields.length == 2) {
            try {
                int sleepTime = Integer.parseInt(fields[1]);
                Thread.sleep(sleepTime);
            } catch (Exception ex) {
                LOG.logp(Level.SEVERE, this.getClass().getName(), "doActions", "", ex);
            }
            return;
        }
        if (action.equalsIgnoreCase("hidetrack")) {
            // Allowing multiple files to be specified, split by commas
            String[] hideTrack = join(fields, 1).split(",");
            for (String aHideTrack : hideTrack) {
                hideTrack(aHideTrack);
            }
            return;
        }
        if (action.equalsIgnoreCase("showtrack")) {
            // Allowing multiple files to be specified, split by commas
            String[] showTrack = join(fields, 1).split(",");
            for (String aShowTrack : showTrack) {
                showTrack(aShowTrack);
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
            if (fields.length >= 1) {
                snapShot(exportMode, new File(join(fields, 1)));	// second field and possibly others are a single filename
            } else {
                // base filename upon organism and timestamp
                String id = GenometryModel.getInstance().getSelectedSeqGroup() == null ? "default"
                        : GenometryModel.getInstance().getSelectedSeqGroup().getID();
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
        LOG.log(Level.WARNING, "Unrecognized or invalid command: {0}", action);
    }

    /**
     * Take a snapshot, i.e., export to a file.
     *
     * @param f
     */
    private static void snapShot(IGBScriptEngine.ExportMode exportMode, File f) {
        Logger.getLogger(IGBScriptEngine.class.getName()).log(
                Level.INFO, "Exporting file {0} in mode: {1}", new Object[]{f.getName(), exportMode.toString()});
        String ext = GeneralUtils.getExtension(f.getName().toLowerCase());
        if (ext.length() == 0) {
            Logger.getLogger(IGBScriptEngine.class.getName()).log(
                    Level.SEVERE, "no file extension given for file", f.getName());
            return;
        }

        if (!isExt(ext)) {
            Logger.getLogger(IGBScriptEngine.class.getName()).log(
                    Level.SEVERE, "image file extension {0} is not supported", ext);
            return;
        }

        try {
            Component c = null;
            switch (exportMode) {
                case WHOLEFRAME:
                    c = igbService.getFrame();
                    break;
                case MAIN:
                    c =  igbService.getSeqMapView().getSeqMap().getNeoCanvas();
                    break;
                case MAINWITHLABELS:
                    c = igbService.getSeqMapView().getSeqMap();
                    break;
                case SLICEDWITHLABELS:
                    c = igbService.determineSlicedComponent();
                    break;
            }
            HeadLessExport hle = new HeadLessExport();
            hle.exportScreenshot(c, f, ext, true);
        } catch (Exception ex) {
            Logger.getLogger(IGBScriptEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void goToGenome(String genomeVersion) {
        AnnotatedSeqGroup group = igbService.determineAndSetGroup(genomeVersion).orNull();
        if (group == null) {
            return;
        }
        for (int i = 0; i < 100; i++) {
            // sleep until versions are initialized
            for (GenericVersion version : group.getEnabledVersions()) {
                if (version.isInitialized()
                        && group == GenometryModel.getInstance().getSelectedSeqGroup()) {
                    continue;
                }
                try {
                    Thread.sleep(300); // not finished initializing versions
                } catch (InterruptedException ex) {
                    LOG.logp(Level.SEVERE, this.getClass().getName(), "goToGenome", "", ex);
                }
            }
        }
    }

    private void goToRegion(String region) {
        igbService.goToRegion(region);
    }

    private void loadData(String serverURIorName, String feature_url) {
        GenericServer server = igbService.loadServer(serverURIorName);
        GenericFeature feature = igbService.getFeature(
                GenometryModel.getInstance().getSelectedSeqGroup(),
                server, feature_url, true);

        if (feature != null) {
            feature.setVisible();
            feature.setPreferredLoadStrategy(LoadStrategy.VISIBLE);
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
                LOG.logp(Level.SEVERE, this.getClass().getName(), "loadFile", "", ex);
                return;
            }
        } else {
            uri = f.toURI();
        }
        AnnotatedSeqGroup group = GenometryModel.getInstance().getSelectedSeqGroup();
        igbService.openURI(uri, fileName, group, group.getOrganism(), false);
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
        } else if (loadMode.equalsIgnoreCase("region_in_view")
                || loadMode.equalsIgnoreCase("visible")) {
            s = LoadStrategy.VISIBLE;
//		} else if (loadMode.equalsIgnoreCase("chromosome")) {
//			s = LoadStrategy.CHROMOSOME;
        } else if (loadMode.equalsIgnoreCase("genome")) {
            s = LoadStrategy.GENOME;
        }

        // First try to look up for feature in current group.
        AnnotatedSeqGroup seqGroup = GenometryModel.getInstance().getSelectedSeqGroup();
        GenericFeature feature = null;

        // If feature is not found in current group then look up all groups.
        if (seqGroup != null) {
            feature = findFeatureInGroup(seqGroup, featureURI);
        }

        if (feature == null) {
            for (AnnotatedSeqGroup group : GenometryModel.getInstance().getSeqGroups().values()) {
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
            LOG.log(Level.SEVERE, "Could not find feature: {0}", featureURIStr);
        }
    }

    private GenericFeature findFeatureInGroup(AnnotatedSeqGroup seqGroup, URI featureURI) {
        GenericFeature feature = null;
        for (GenericVersion version : seqGroup.getEnabledVersions()) {
            feature = igbService.findFeatureWithURI(version, featureURI);
            if (feature != null) {
                break;
            }
        }

        return feature;
    }

    /**
     * Join fields from startField to end of fields.
     */
    private String join(String[] fields, int startField) {
        StringBuilder buffer = new StringBuilder("");
        for (int i = startField; i < fields.length; i++) {
            buffer.append(fields[i]);
        }
        return buffer.toString();
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
