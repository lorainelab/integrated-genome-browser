package com.affymetrix.genometry.general;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryConstants;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.das2.Das2Type;
import com.affymetrix.genometry.das2.FormatPriorities;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.LoadUtils.RefreshStatus;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometry.util.ServerTypeI;
import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that's useful for visualizing a generic feature. A feature is unique
 * to a genome version/species/server. Thus, there is a many-to-one map to
 * GenericVersion. (Even if the feature names and version names match, but the
 * servers don't, we can't guarantee that they would contain the same
 * information.)
 *
 * @version $Id: GenericFeature.java 10112 2012-02-02 16:24:42Z imnick $
 */
public final class GenericFeature {

    public static final String LOAD_WARNING_MESSAGE = GenometryConstants.BUNDLE.getString("howtoloadmessage");
    public static final String REFERENCE_SEQUENCE_LOAD_MESSAGE = GenometryConstants.BUNDLE.getString("howToLoadReferenceSequence");
    public static final String show_how_to_load = GenometryConstants.BUNDLE.getString("show_how_to_load");
    public static final boolean default_show_how_to_load = true;

    private static final String WHOLE_GENOME = "Whole Sequence";

    public final String featureName;      // friendly name of the feature.
    public final Map<String, String> featureProps;
    public final GenericVersion gVersion;        // Points to the version that uses this feature.
    private boolean visible;							// indicates whether this feature should be visible or not (used in FeatureTreeView/GeneralLoadView interaction).
    private LoadStrategy loadStrategy;  // range chosen by the user, defaults to NO_LOAD.
    private final String friendlyURL;			// friendly URL that users may look at.
    private RefreshStatus lastRefresh;
    public final Object typeObj;    // Das2Type, ...?
    public final SymLoader symL;
    private String method;

    private final boolean isReferenceSequence;

    private static final List<LoadStrategy> STANDARD_LOAD_CHOICES = ImmutableList.<LoadStrategy>of(
            LoadStrategy.NO_LOAD,
            LoadStrategy.VISIBLE,
            LoadStrategy.GENOME);

    // Requests that have been made for this feature (to avoid overlaps)
    private final MutableSeqSymmetry requestSym = new SimpleMutableSeqSymmetry();
    // Request that are currently going on. (To avoid parsing more than once)
    private final MutableSeqSymmetry currentRequestSym = new SimpleMutableSeqSymmetry();

    public GenericFeature(
            String featureName, Map<String, String> featureProps, GenericVersion gVersion, SymLoader symLoader, Object typeObj, boolean autoload) {
        this(featureName, featureProps, gVersion, symLoader, typeObj, autoload, false);
    }

    /**
     * @param featureName
     * @param featureProps
     * @param gVersion
     * @param typeObj
     */
    public GenericFeature(
            String featureName, Map<String, String> featureProps, GenericVersion gVersion, SymLoader symLoader, Object typeObj, boolean autoload, boolean isReferenceSequence) {
        this.featureName = featureName;
        this.featureProps = featureProps;
        this.gVersion = gVersion;
        this.symL = symLoader;
        this.typeObj = typeObj;
        if (typeObj instanceof Das2Type) {
            ((Das2Type) typeObj).setFeature(this);
        }

        this.friendlyURL = this.featureProps == null ? null : this.featureProps.get("url");

        this.setAutoload(autoload);
        this.lastRefresh = RefreshStatus.NOT_REFRESHED;
        this.isReferenceSequence = isReferenceSequence;
        //methods.add(featureName);
    }

    public boolean setAutoload(boolean auto) {
        if (shouldAutoLoad(featureProps, WHOLE_GENOME) && auto) {
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
        if (gVersion != null && gVersion.gServer != null) {
            if (gVersion.gServer.serverType.loadStrategyVisibleOnly()) {
                setLoadStrategy(LoadStrategy.VISIBLE);
            } else {
                // Local File or QuickLoad
                if (this.symL != null) {
                    if (this.symL.getLoadChoices().contains(LoadStrategy.VISIBLE)) {
                        setLoadStrategy(LoadStrategy.VISIBLE);
//					} else if (this.symL.getLoadChoices().contains(LoadStrategy.CHROMOSOME)) {
//						setLoadStrategy(LoadStrategy.CHROMOSOME);
                    } else {
                        setLoadStrategy(LoadStrategy.GENOME);
                    }
                }
            }
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
            Logger.getLogger(GenericFeature.class.getName()).log(Level.WARNING,
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

    public String getFriendlyURL() {

        if (friendlyURL == null) {
            return null;
        }

        String friendlyURLString = friendlyURL;

        // Support relative path in friendly URL for Quickload
        if (!(friendlyURLString.toLowerCase().startsWith(HTTP_PROTOCOL)
                || friendlyURLString.toLowerCase().startsWith(HTTPS_PROTOCOL)
                || friendlyURLString.toLowerCase().startsWith(FTP_PROTOCOL)
                || friendlyURLString.toLowerCase().startsWith(FILE_PROTOCOL))) {

            if (this.gVersion.gServer.serverType == ServerTypeI.QuickLoad) {

                if (friendlyURLString.startsWith("./")) {
                    friendlyURLString = friendlyURLString.substring(2);
                } else if (friendlyURLString.startsWith("/")) {
                    friendlyURLString = friendlyURLString.substring(1);
                }

                /**
                 * For Quickload the server path to be used is stored in
                 * serverObj, and it always end with a '/' during server
                 * initialization
                 *
                 * Concentrate that URL with server path to support relative
                 * friendly URL (documentation link in feature tree)
                 */
                return this.gVersion.gServer.serverObj + friendlyURLString;

            } else {
                return friendlyURLString;
            }
        } else {
            if (this.gVersion.gServer.serverType == ServerTypeI.QuickLoad) {
                return friendlyURLString.replaceAll(this.gVersion.gServer.URL, (String) this.gVersion.gServer.serverObj);
            }
            return friendlyURLString;
        }
    }

    public String description() {
        if (this.featureProps != null) {
            String summary = featureProps.get("summary");
            String descrip = featureProps.get("description");

            if (summary != null && summary.length() > 0) {
                return summary;
            }
            if (descrip != null && descrip.length() > 0) {
                if (descrip.length() > 100) {
                    return descrip.substring(0, 100) + "...";
                }
                return descrip;
            }
        }
        return featureName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
        ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method, method, getExtension(), featureProps);
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
            Logger.getLogger(GenericFeature.class.getName()).log(Level.WARNING, "Genericfeature contains current request sym for server {0}", gVersion.gServer.serverType);
            currentRequestSym.removeChildren();
        }
        method = null;
        if (symL != null) {
            symL.clear();
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

    public synchronized final void removeCurrentRequest(SeqSpan span) {
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
        if (symL != null) {
            return symL.getLoadChoices();
        }

        return STANDARD_LOAD_CHOICES;
    }

    @Override
    public String toString() {
        // remove all but the last "/", since these will be represented in a friendly tree view.
        if (!this.featureName.contains("/")) {
            return this.featureName;
        }

        int lastSlash = this.featureName.lastIndexOf('/');
        return this.featureName.substring(lastSlash + 1, featureName.length());
    }

    public URI getURI() {
        if (typeObj instanceof Das2Type) {
            return ((Das2Type) typeObj).getURI();
        }
        if (typeObj instanceof String) {
            return URI.create(typeObj.toString());
        }

        if (symL != null) {
            return symL.uri;
        }
        return null;
    }

    public String getExtension() {
        if (typeObj instanceof Das2Type) {
            String ext = FormatPriorities.getFormat((Das2Type) typeObj);
            if (ext == null) {
                ext = "";
            }
            return ext;
        }

        if (symL != null) {
            return symL.extension;
        }

        return "";
    }
}
