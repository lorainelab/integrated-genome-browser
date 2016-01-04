package com.affymetrix.igb.shared;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.operator.AbstractLogTransform;
import com.affymetrix.genometry.operator.AbstractMathTransform;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.operator.PowerTransformer;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symloader.Delegate;
import com.affymetrix.genometry.symloader.Delegate.DelegateParent;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.symmetry.impl.UcscBedSym;
import com.affymetrix.genometry.util.BioSeqUtils;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.action.SeqMapViewActionA;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import org.lorainelab.igb.igb.genoviz.extensions.glyph.StyledGlyph;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class TrackFunctionOperationA extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;

    private static Boolean isForward(StyledGlyph vg) {
        if (vg.getAnnotStyle().isGraphTier()) {
            return null;
        }

        if (vg.getDirection() == StyledGlyph.Direction.BOTH || vg.getDirection() == StyledGlyph.Direction.NONE) {
            return null;
        }

        return vg.getDirection() == StyledGlyph.Direction.FORWARD;
    }

    private static String removeIllegalCharacters(String string) {
        string = string.replaceAll("\\s+", "_");
        string = string.replaceAll("\\|", "_");
        string = string.replaceAll("\u221E", "infinite");
        string = string.replaceAll("\\[", "(");
        string = string.replaceAll("\\]", ")");
        return string;
    }
    private final Operator mOperator;

    protected TrackFunctionOperationA(Operator operator, String text) {
        super(text, null, null);
        this.mOperator = operator;
    }

    protected TrackFunctionOperationA(Operator operator) {
        this(operator, null);
    }

    protected void addTier(List<StyledGlyph> vgs) {
        java.util.List<DelegateParent> dps = new java.util.ArrayList<>();

        for (StyledGlyph vg : vgs) {
            if (vg.getAnnotStyle().getFeature() == null) {
                addNonUpdateableTier(vgs);
                return;
            }

            dps.add(new DelegateParent(vg.getAnnotStyle().getMethodName(),
                    isForward(vg), vg.getAnnotStyle().getFeature(), vg.getAnnotStyle().getFilter()));

        }

        DataSet feature = createFeature(getMethod(vgs, false), getMethod(vgs, true), getOperator(), dps, vgs.get(0).getAnnotStyle());
        GeneralLoadUtils.loadAndDisplayAnnotations(feature);
    }

    protected Operator getOperator() {
        return mOperator;
    }

    protected String getMethod(List<? extends GlyphI> vgs, boolean append_symbol) {
        StringBuilder meth = new StringBuilder();
        for (GlyphI gl : vgs) {
            if (((StyledGlyph) gl).getAnnotStyle().getFilter() != null) {
                meth.append("Filtered ");
                break;
            }
        }
        Optional<Object> operationParam = getOperatorParam();
        if (operationParam.isPresent()) {
            meth.append(getOperator().getDisplay()).append(" " + operationParam.get().toString() + ": ");
        } else {
            meth.append(getOperator().getDisplay()).append(": ");
        }
        boolean started = false;
        for (GlyphI gl : vgs) {
            if (started) {
                meth.append(", ");
            }
            meth.append(((StyledGlyph) gl).getAnnotStyle().getTrackName());
            if (append_symbol) {
                meth.append(((StyledGlyph) gl).getDirection().getDisplay());
            }
            started = true;
        }
        return meth.toString();
    }

    private Optional<Object> getOperatorParam() {
        Operator operator = getOperator();
        if (operator instanceof AbstractMathTransform && !(operator instanceof AbstractLogTransform)) {
            AbstractMathTransform op = (AbstractMathTransform) operator;
            return Optional.of(op.getParameterValue(op.getParamPrompt()).toString());
        }
        if(operator instanceof PowerTransformer) {
            PowerTransformer op = (PowerTransformer) operator;
            return Optional.ofNullable(op.getParameterValue(op.getParamPrompt()));
        }
        return Optional.empty();
    }

    private void addNonUpdateableTier(List<? extends GlyphI> vgs) {
        BioSeq aseq = GenometryModel.getInstance().getSelectedSeq().orElse(null);
        ITrackStyleExtended preferredStyle = null;
        List<SeqSymmetry> seqSymList = new ArrayList<>();
        for (GlyphI gl : vgs) {
            SeqSymmetry rootSym = (SeqSymmetry) gl.getInfo();
            if (rootSym == null && gl.getChildCount() > 0) {
                rootSym = (SeqSymmetry) gl.getChild(0).getInfo();
            }
            if (rootSym != null) {
                seqSymList.add(rootSym);
                String method = BioSeqUtils.determineMethod(rootSym);
                if (rootSym instanceof SimpleSymWithProps && preferredStyle == null && method != null) {
                    preferredStyle = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method);
                }
            }
        }
        Operator operator = getOperator();
        SeqSymmetry result_sym = operator.operate(aseq, seqSymList);
        if (result_sym != null) {
            StringBuilder meth = new StringBuilder();
            if (result_sym instanceof UcscBedSym) {
                meth.append(((UcscBedSym) result_sym).getType());
            } else {
                meth.append(operator.getDisplay()).append("- ");
                for (GlyphI gl : vgs) {
                    meth.append(((StyledGlyph) gl).getAnnotStyle().getTrackName()).append(", ");
                }
            }
            TrackUtils.getInstance().addTrack(result_sym, meth.toString(), operator, preferredStyle);
        }
    }

    private DataSet createFeature(String method, String featureName, Operator operator, List<Delegate.DelegateParent> dps, ITrackStyleExtended preferredStyle) {
        method = IGBStateProvider.getUniqueName("file:/" + removeIllegalCharacters(method));

        java.net.URI uri;
        try {
            uri = java.net.URI.create(method);
        } catch (java.lang.IllegalArgumentException ex) {
            if (ex.getCause() instanceof java.net.URISyntaxException) {
                java.net.URISyntaxException uriex = (java.net.URISyntaxException) ex.getCause();
                Logger.getLogger(TrackFunctionOperationA.class.getName()).log(Level.INFO, "{0}.\nCharacter {1}", new Object[]{uriex.getMessage(), method.charAt(uriex.getIndex())});
            } else {
                Logger.getLogger(TrackFunctionOperationA.class.getName()).log(Level.INFO, "Illegal character in string {0}", method);
            }

            //method = GeneralUtils.URLEncode(featureName);
            //method = TrackStyle.getUniqueName("file:/"+method);
            uri = java.net.URI.create(GeneralUtils.URLEncode(method));
        }

        DataContainer version = GeneralLoadUtils.getLocalFileDataContainer(GenometryModel.getInstance().getSelectedGenomeVersion(), GeneralLoadView.getLoadView().getSelectedSpecies());
        DataSet feature = GeneralLoadView.getLoadView().createDataSet(uri, featureName, new Delegate(uri, Optional.empty(), featureName, version.getGenomeVersion(), operator, dps));

        ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method, featureName, Delegate.EXT, null);
        if (preferredStyle != null) {
            style.copyPropertiesFrom(preferredStyle);
            style.setSeparate(false);
        }
        if (operator.getOutputCategory() == FileTypeCategory.Graph
                || operator.getOutputCategory() == FileTypeCategory.Mismatch) {
            style.setExpandable(false);
            style.setGraphTier(true);
        }

        style.setTrackName(featureName);
        style.setLabelField(null);
        if (style instanceof TrackStyle && operator instanceof Operator.Style) {
            ((TrackStyle) style).setProperties(((Operator.Style) operator).getStyleProperties());
        }

        return feature;
    }

}
