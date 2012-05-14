/*
 * http://today.java.net/pub/a/today/2006/09/21/making-scripting-languages-jsr-223-aware.html
 */

package com.gene.igbscript;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.igb.osgi.service.IGBService;

/**
 * java ScriptEngine to run scripts written in IGB scripting language
 */
public class IGBScriptEngine implements ScriptEngine {
    
    private static final String __ENGINE_VERSION__ = "V0.01a";
    private static final String IGB_NAME = "IGBLanguage";
    private static final String IGB_SHORT_NAME = "IGBLanguage";
    private static final String STR_DUMMYLANGUAGE = "igb language";
    
    private final ScriptEngineFactory igbFactory;

	private static String splitter = "\\s";
	private final IGBService igbService;

    private ScriptContext defaultContext;
    
    public IGBScriptEngine(IGBScriptEngineFactory factory, IGBService igbService) {
    	super();
    	this.igbService = igbService;
    	igbFactory = factory;
        setContext(new SimpleScriptContext());
        // set special values
        put(LANGUAGE_VERSION, "???");
        put(LANGUAGE, STR_DUMMYLANGUAGE);
        put(ENGINE, IGB_NAME);
        put(ENGINE_VERSION, __ENGINE_VERSION__);
        put(ARGV, ""); // TO DO: set correct value
        put(FILENAME, ""); // TO DO: set correct value
        put(NAME, IGB_SHORT_NAME);
        /*
         * I am not sure if this is correct; we need to check if
         * the name really is THREADING. I have no idea why there is
         * no constant as for the other keys
         */
        put("THREADING", null);
    }
    
    public Object eval(String script) throws ScriptException {
        return eval(script, getContext());
    }
    
    public Object eval(String script, ScriptContext context) throws ScriptException {
    	doActions(script);
        return null;
    }
    
    public Object eval(String script, Bindings bindings) throws ScriptException {
        Bindings current = getContext().getBindings(ScriptContext.ENGINE_SCOPE);
        getContext().setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        Object result = eval(script);
        getContext().setBindings(current, ScriptContext.ENGINE_SCOPE);
        return result;
    }
    
    public Object eval(Reader reader) throws ScriptException {
        return eval(getScriptFromReader(reader));
    }
    
    public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
        return eval(getScriptFromReader(reader), scriptContext);
    }
    
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        return eval(getScriptFromReader(reader), bindings);
    }
    
    public void put(String key, Object value) {
        getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
    }
    
    public Object get(String key) {
        return getBindings(ScriptContext.ENGINE_SCOPE).get(key);
    }
    
    public Bindings getBindings(int scope) {
        return getContext().getBindings(scope);
    }
    
    public void setBindings(Bindings bindings, int scope) {
        getContext().setBindings(bindings, scope);
    }
    
    public Bindings createBindings() {
        return new SimpleBindings();
    }
    
    public ScriptContext getContext() {
        return defaultContext;
    }
    
    public void setContext(ScriptContext context) {
        defaultContext = context;
    }
    
    public ScriptEngineFactory getFactory() {
        return igbFactory;
    }
    
    /*
     * private methods
     */
    
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
	 *
	 * @param bis
	 */
	private void doActions(String scriptString) {
		try {
			String[] lines = scriptString.split("\n");
			for (String line : lines) {
				//Ignore comments.
				if (line.startsWith("#") || line.trim().length() == 0) {
					continue;
				}

				try {
					igbService.addNotLockedUpMsg("Executing script line: " + line);
					Logger.getLogger(this.getClass().getName()).log(
							Level.INFO, "line: {0}", line);
					doSingleAction(line);
					Thread.sleep(1000);	// user actions don't happen instantaneously, so give a short sleep time between batch actions.
				} finally {
					igbService.removeNotLockedUpMsg("Executing script line: " + line);
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void doSingleAction(String line) {
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
						Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
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
		}
		if (action.equalsIgnoreCase("loadmode")) {
			if (fields.length >= 2) {
				loadMode(fields[1], join(fields, 2));
			}
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
		}
		if (action.equalsIgnoreCase("refresh")) {
			igbService.loadVisibleFeatures();
		}
		if (action.equalsIgnoreCase("select") && fields.length >= 2) {
			igbService.performSelection(join(fields, 1));
		}
		if (action.equalsIgnoreCase("selectfeature") && fields.length >= 2) {
			igbService.selectFeatureAndCenterZoomStripe(join(fields, 1));
		}
		if (action.equalsIgnoreCase("setZoomStripePosition") && fields.length >= 2) {
			double position = Double.parseDouble(join(fields, 1));
			igbService.getSeqMapView().setZoomSpotX(position);
			igbService.getSeqMapView().setZoomSpotY(0);
		}
		if (action.equals("sleep") && fields.length == 2) {
			try {
				int sleepTime = Integer.parseInt(fields[1]);
				Thread.sleep(sleepTime);
			} catch (Exception ex) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private void goToGenome(String genomeVersion) {
		AnnotatedSeqGroup group = igbService.determineAndSetGroup(genomeVersion);
		if (group == null) {
			return;
		}
		for (int i = 0; i < 100; i++) {
			// sleep until versions are initialized
			for (GenericVersion version : group.getEnabledVersions()) {
				if (version.isInitialized() && group == GenometryModel.getGenometryModel().getSelectedSeqGroup()) {
					continue;
				}
				try {
					Thread.sleep(300); // not finished initializing versions
				} catch (InterruptedException ex) {
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	private void goToRegion(String region) {
		igbService.goToRegion(region);
	}

	private void loadData(String serverURIorName, String feature_url) {
		GenericServer server = igbService.loadServer(serverURIorName);
		GenericFeature feature = igbService.getFeature(server, feature_url);

		if (feature != null) {
			feature.setVisible();
			feature.setPreferredLoadStrategy(LoadStrategy.VISIBLE);
		}

		igbService.refreshDataManagementView();
	}

	private void loadFile(String fileName) {
		URI uri;
		File f = new File(fileName.trim());
		if (fileName.startsWith("http")) {
			try {
				uri = new URI(fileName);
			} catch (URISyntaxException ex) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
				return;
			}
		} else {
			uri = f.toURI();
		}
		AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		igbService.openURI(uri, fileName, group, group.getOrganism(), true);
	}

	private void loadMode(String loadMode, String featureURIStr) {

		URI featureURI = null;
		File featureFile = new File(featureURIStr.trim());
		if (featureFile.exists()) {
			featureURI = featureFile.toURI();
		} else {
			featureURI = URI.create(featureURIStr.trim());
		}
		LoadStrategy s = LoadStrategy.NO_LOAD;
		if (loadMode.equalsIgnoreCase("no_load")) {
			s = LoadStrategy.NO_LOAD;
		} else if (loadMode.equalsIgnoreCase("region_in_view") || loadMode.equalsIgnoreCase("visible")) {
			s = LoadStrategy.VISIBLE;
		} else if (loadMode.equalsIgnoreCase("chromosome")) {
			s = LoadStrategy.CHROMOSOME;
		} else if (loadMode.equalsIgnoreCase("genome")) {
			s = LoadStrategy.GENOME;
		}

		// First try to look up for feature in current group.
		AnnotatedSeqGroup seqGroup = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		GenericFeature feature = null;

		// If feature is not found in current group then look up all groups.
		if (seqGroup != null) {
			feature = findFeatureInGroup(seqGroup, featureURI);
		}

		if (feature == null) {
			for (AnnotatedSeqGroup group : GenometryModel.getGenometryModel().getSeqGroups().values()) {
				feature = findFeatureInGroup(group, featureURI);
				if (feature != null) {
					break;
				}
			}
		}

		if (feature != null) {
			feature.setPreferredLoadStrategy(s);
		} else {
			Logger.getLogger(this.getClass().getName()).log(
					Level.SEVERE, "Couldn''t find feature :{0}", featureURIStr);
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
	 *
	 * @param fields
	 * @param startField
	 * @return
	 */
	private String join(String[] fields, int startField) {
		StringBuilder buffer = new StringBuilder("");
		for (int i = startField; i < fields.length; i++) {
			buffer.append(fields[i]);
		}
		return buffer.toString();
	}
}
