package com.affymetrix.genometry.general;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryConstants;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.LoadUtils.RefreshStatus;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometry.util.ServerUtils;
import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

public final class DataSet {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DataSet.class);
    public static final String LOAD_WARNING_MESSAGE = GenometryConstants.BUNDLE.getString("howtoloadmessage");
    public static final String REFERENCE_SEQUENCE_LOAD_MESSAGE = GenometryConstants.BUNDLE.getString("howToLoadReferenceSequence");
    public static final String show_how_to_load = GenometryConstants.BUNDLE.getString("show_how_to_load");
    public static final boolean default_show_how_to_load = true;

    private static final String WHOLE_GENOME = "Whole Sequence";

    private final String name;      // friendly name of the feature.
    private final Map<String, String> properties;
    private final DataContainer dataContainer;        // Points to the version that uses this feature.
    private boolean visible;							// indicates whether this feature should be visible or not (used in FeatureTreeView/GeneralLoadView interaction).
    private LoadStrategy loadStrategy;  // range chosen by the user, defaults to NO_LOAD.
    private RefreshStatus lastRefresh;
    private SymLoader symL;
    private String method;
    private URI uri;
    private boolean supportsAvailabilityCheck;

    private final boolean isReferenceSequence;

    private static final List<LoadStrategy> STANDARD_LOAD_CHOICES = ImmutableList.<LoadStrategy>of(
            LoadStrategy.NO_LOAD,
            LoadStrategy.VISIBLE,
            LoadStrategy.GENOME);

    // Requests that have been made for this feature (to avoid overlaps)
    private final MutableSeqSymmetry requestSym = new SimpleMutableSeqSymmetry();
    // Request that are currently going on. (To avoid parsing more than once)
    private final MutableSeqSymmetry currentRequestSym = new SimpleMutableSeqSymmetry();

    public DataSet(URI uri, Map<String, String> dataSetProps, DataContainer dataContainer) {
        this.uri = uri;
        boolean autoload = PreferenceUtils.getBooleanParam(PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load);
        this.name = dataSetProps.get("title");
        this.properties = dataSetProps;
        this.setAutoload(autoload);
        this.dataContainer = dataContainer;
        this.lastRefresh = RefreshStatus.NOT_REFRESHED;
        this.isReferenceSequence = false;
        this.supportsAvailabilityCheck = false;
    }

    public DataSet(URI uri, String name, Map<String, String> featureProps, DataContainer dataContainer,
            SymLoader symLoader, boolean autoload) {
        this(uri, name, featureProps, dataContainer, symLoader, autoload, false);
    }

    public DataSet(String name, Map<String, String> featureProps, DataContainer dataContainer,
            SymLoader symLoader, Object typeObj, boolean autoload) {
        this(null, name, featureProps, dataContainer, symLoader, autoload, false);
    }

    /**
     * @param name
     * @param props
     * @param dataContainer
     * @param typeObj
     */
    public DataSet(URI uri,
            String name, Map<String, String> props, DataContainer dataContainer, SymLoader symLoader, boolean autoload, boolean isReferenceSequence) {
        this.uri = uri;
        this.name = name;
        this.properties = props;
        this.dataContainer = dataContainer;
        this.symL = symLoader;

        this.setAutoload(autoload);
        this.lastRefresh = RefreshStatus.NOT_REFRESHED;
        this.isReferenceSequence = isReferenceSequence;
        this.supportsAvailabilityCheck = false;
        //methods.add(name);
    }

    public static String detemineFriendlyName(URI uri) {
        String uriString = uri.toASCIIString().toLowerCase();
        String unzippedStreamName = GeneralUtils.stripEndings(uriString);
        String ext = GeneralUtils.getExtension(unzippedStreamName);

        String unzippedName = GeneralUtils.getUnzippedName(uri.toString());
        String strippedName = unzippedName.substring(unzippedName.lastIndexOf('/') + 1);
        String friendlyName = strippedName.substring(0, strippedName.toLowerCase().indexOf(ext));
        return friendlyName;
    }

    public boolean setAutoload(boolean auto) {
        if (shouldAutoLoad(getProperties(), WHOLE_GENOME) && auto) {
            setLoadStrategy(LoadStrategy.GENOME);
            this.setVisible();
            return true;
        }
        if (!visible) {
            setLoadStrategy(LoadStrategy.NO_LOAD);
        }
        return false;
    }

    public void setVisible() {
        this.visible = true;
        if (this.loadStrategy != LoadStrategy.NO_LOAD) {
            return;
        }
        if (getDataContainer() != null && getDataContainer().getDataProvider() != null) {
            if (this.getSymL() != null) {
                if (this.getSymL().getLoadChoices().contains(LoadStrategy.VISIBLE)) {
                    setLoadStrategy(LoadStrategy.VISIBLE);
//					} else if (this.symL.getLoadChoices().contains(LoadStrategy.CHROMOSOME)) {
//						setLoadStrategy(LoadStrategy.CHROMOSOME);
                } else {
                    setLoadStrategy(LoadStrategy.GENOME);
                }
            }
            setLoadStrategy(LoadStrategy.VISIBLE);
        }
    }

    private void setInvisible() {
        this.visible = false;
        setLoadStrategy(LoadStrategy.NO_LOAD);
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean isReferenceSequence() {
        return isReferenceSequence;
    }

    public LoadStrategy getLoadStrategy() {
        return loadStrategy;
    }

    public void setLoadStrategy(LoadStrategy loadStrategy) {
        this.loadStrategy = loadStrategy;
    }

    public boolean setPreferredLoadStrategy(LoadStrategy loadStrategy) {
        if (getLoadChoices().contains(loadStrategy)) {
            setLoadStrategy(loadStrategy);
            return true;
        } else {
            setLoadStrategy(getLoadChoices().get(1));
            Logger.getLogger(DataSet.class.getName()).log(Level.WARNING,
                    "Given {0} strategy is not permitted instead using {1} "
                    + "strategy.", new Object[]{loadStrategy, getLoadStrategy()});
        }
        return false;
    }

    /**
     * @param featureProps feature properties
     * @return true if feature should be loaded automatically
     */
    private static boolean shouldAutoLoad(Map<String, String> featureProps, String loadStrategy) {
        return (featureProps != null
                && featureProps.containsKey("load_hint")
                && featureProps.get("load_hint").equals(loadStrategy));
    }

    public String description() {
        if (this.getProperties() != null) {
            String summary = getProperties().get("summary");
            String descrip = getProperties().get("description");

            if (summary != null && summary.length() > 0) {
                return summary;
            }
            if (descrip != null && descrip.length() > 0) {
                return descrip;
            }
        }
        return getDataSetName();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
        ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method, method, getExtension(), getProperties());
        style.setFeature(this);
    }

    public void setLastRefreshStatus(RefreshStatus status) {
        lastRefresh = status;
    }

    public RefreshStatus getLastRefreshStatus() {
        return lastRefresh;
    }

    public void clear(BioSeq seq) {
        List<SeqSymmetry> removeList = new ArrayList<>();

        for (int i = 0; i < requestSym.getChildCount(); i++) {
            SeqSymmetry sym = requestSym.getChild(i);
            if (sym.getSpan(seq) != null) {
                removeList.add(sym);
            }
        }

        removeList.forEach(requestSym::removeChild);

        removeList.clear();
    }

    /**
     * Remove all methods and set feature invisible.
     */
    public void clear() {
        // Remove all childred from request
        requestSym.removeChildren();
        if (currentRequestSym.getChildCount() > 0) {
            Logger.getLogger(DataSet.class.getName()).log(Level.WARNING, "Genericfeature contains current request sym for server {0}", getDataContainer().getDataProvider());
            currentRequestSym.removeChildren();
        }
        method = null;
        if (symL != null) {
            getSymL().clear();
        }
        setInvisible();
        setLastRefreshStatus(RefreshStatus.NOT_REFRESHED);
    }

    public synchronized boolean isLoaded(SeqSpan span) {
        MutableSeqSymmetry query_sym = new SimpleMutableSeqSymmetry();
        query_sym.addSpan(span);

        SeqSymmetry optimized_sym = SeqUtils.exclusive(query_sym, requestSym, span.getBioSeq());
        return !SeqUtils.hasSpan(optimized_sym);
    }

    public synchronized boolean isLoading(SeqSpan span) {
        MutableSeqSymmetry query_sym = new SimpleMutableSeqSymmetry();
        query_sym.addSpan(span);

        SeqSymmetry optimized_sym = SeqUtils.exclusive(query_sym, currentRequestSym, span.getBioSeq());
        return !SeqUtils.hasSpan(optimized_sym);
    }

    /**
     * Split the requested span into spans that still need to be loaded. Note we
     * can't filter inside spans (in general) until after the data is returned.
     */
    public synchronized SeqSymmetry optimizeRequest(SeqSpan span) {
        MutableSeqSymmetry query_sym = new SimpleMutableSeqSymmetry();
        query_sym.addSpan(span);

        SeqSymmetry optimized_sym = SeqUtils.exclusive(query_sym, requestSym, span.getBioSeq());
        optimized_sym = SeqUtils.exclusive(optimized_sym, currentRequestSym, span.getBioSeq());
        if (SeqUtils.hasSpan(optimized_sym)) {
            return optimized_sym;
        }
        return null;
    }

    /**
     * This span is now considered loaded.
     *
     * @param span
     */
    public synchronized void addLoadedSpanRequest(SeqSpan span) {
        MutableSeqSymmetry query_sym = new SimpleMutableSeqSymmetry();
        query_sym.addSpan(span);
        requestSym.addChild(query_sym);
        removeCurrentRequest(span);
    }

    public final synchronized void removeCurrentRequest(SeqSpan span) {
        for (int i = 0; i < currentRequestSym.getChildCount(); i++) {
            SeqSymmetry sym = currentRequestSym.getChild(i);
            if (span == sym.getSpan(span.getBioSeq())) {
                currentRequestSym.removeChild(sym);
            }
        }
    }

    public synchronized void addLoadingSpanRequest(SeqSpan span) {
        MutableSeqSymmetry query_sym = new SimpleMutableSeqSymmetry();
        query_sym.addSpan(span);
        currentRequestSym.addChild(query_sym);
    }

    public synchronized MutableSeqSymmetry getRequestSym() {
        return requestSym;
    }

    public List<LoadStrategy> getLoadChoices() {
        if (getSymL() != null) {
            return getSymL().getLoadChoices();
        }

        return STANDARD_LOAD_CHOICES;
    }

    @Override
    public String toString() {
        // remove all but the last "/", since these will be represented in a friendly tree view.
        if (!this.name.contains("/")) {
            return this.getDataSetName();
        }

        int lastSlash = this.getDataSetName().lastIndexOf('/');
        return this.getDataSetName().substring(lastSlash + 1, getDataSetName().length());
    }

    public URI getURI() {
        return uri;
    }

    public String getExtension() {
        if (getSymL() != null) {
            return getSymL().extension;
        }

        return "";
    }

    /**
     * @return the name
     */
    public String getDataSetName() {
        return name;
    }

    /**
     * @return the featureProps
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @return the dataContainer
     */
    public DataContainer getDataContainer() {
        return dataContainer;
    }

    /**
     * @return the symL
     */
    public SymLoader getSymL() {
        if (symL == null) {
            symL = ServerUtils.determineLoader(SymLoader.getExtension(uri), uri, detemineFriendlyName(uri), dataContainer.getGenomeVersion());
        }
        return symL;
    }

    public boolean isSupportsAvailabilityCheck() {
        return supportsAvailabilityCheck;
    }

    public void setSupportsAvailabilityCheck(boolean supportsAvailabilityCheck) {
        this.supportsAvailabilityCheck = supportsAvailabilityCheck;
    }

}
