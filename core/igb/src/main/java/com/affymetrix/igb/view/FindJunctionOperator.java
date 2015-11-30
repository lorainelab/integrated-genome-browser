/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.filter.ChildThresholdFilter;
import com.affymetrix.genometry.filter.SymmetryFilterI;
import com.affymetrix.genometry.filter.UniqueLocationFilter;
import com.affymetrix.genometry.filter.WithIntronFilter;
import com.affymetrix.genometry.general.IParameters;
import com.affymetrix.genometry.operator.AbstractAnnotationTransformer;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.operator.Operator.Style;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.style.PropertyConstants;
import com.affymetrix.genometry.symmetry.impl.MultiTierSymWrapper;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.symmetry.impl.UcscBedSym;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * Operates on BAM (alignment) tracks. Uses spliced alignments infer the
 * location of introns. 
 * <p>
 * Produces two-span junction features that bracket introns. The score field 
 * indicates the number of spliced reads supporting that junction.
 * <p>
 * Uses a sequence data to infer junction strand, as follows:
 * <pre>
 *       XXXXXX-------------XXXXXX spliced read 
 *           gtxxxxxxxxxxxxxag top (plus) strand of DNA
 *       junction is on plus strand, transcribed left to right 
 *            >.............>
 *
 *       XXXXXX-------------XXXXXX spliced read 
 *           ct-------------ac top (plus) strand of DNA
 *       junction is on the minus strand, transcribed right to left 
 *            <.............<
 *</pre>
 * Only read alignments with a threshold number of bases flanking the intron on each
 * end are counted.
 * <p>
 * The size of the spans on either end of the junction feature depends on the
 * threshold (flanking) parameter.
 * <p>
 * However, if the user selects the tophat option, the flanking spans are as
 * large as the largest flanking region from an aligned read, as with the
 * tophat spliced alignment program.
 * <p>
 * Users are supposed to be able to specifiy whether singly-mapped or 
 * multiply-mapped reads can be used, but this appears to be broken as of
 * IGB 8.5 as only singly-mapped reads are used.
 */
@Component(name = FindJunctionOperator.COMPONENT_NAME, provide = Operator.class, immediate = true)
public class FindJunctionOperator extends AbstractAnnotationTransformer implements Operator, IParameters, Style {

    public static final String COMPONENT_NAME = "FindJunctionOperator";
    public static final String THRESHOLD = "threshold";

    /**
     * TopHat style flanking makes the junction flanks as long as the largest
     * length of extrons from each side of a qualified intron.
     *
     * If not specifying TopHat style, the flank length equals to threadhold (5
     * by default)
     *
     */
    public static final String TOPHATSTYLEFLANKING = "topHatStyleFlanking";
    public static final int default_threshold = 5;

    private final Map<String, Class<?>> properties;
    private final Map<String, Object> style;
    private static final SymmetryFilterI noIntronFilter = new WithIntronFilter();
    private static final ChildThresholdFilter childThresholdFilter = new ChildThresholdFilter();
    private static final SymmetryFilterI uniqueLocationFilter = new UniqueLocationFilter();
    private int threshold;

    public FindJunctionOperator() {
        super(FileTypeCategory.Alignment);
        threshold = default_threshold;
        properties = new HashMap<>();
        properties.put(THRESHOLD, Integer.class);
        style = new HashMap<>();
        style.put(PropertyConstants.PROP_LABEL_FIELD, "score");
    }

    @Override
    public String getName() {
        return "findjunctions";
    }

    private static int[] getStartEnd(SeqSymmetry tsym, BioSeq aseq) {
        int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;

        for (int i = 0; i < tsym.getChildCount(); i++) {
            SeqSymmetry childSym = tsym.getChild(i);
            SeqSpan span = childSym.getSpan(aseq);
            if (span.getMax() > end) {
                end = span.getMax();
            }

            if (span.getMin() < start) {
                start = span.getMin();
            }
        }

        return new int[]{start, end};
    }
    /* This is an Operator method which is used to operates on a given list of symmetries and find the junctions between them
     * by applying different kinds of filters and writes the resultant symmetries onto a Symmetry Container.
     */

    @Override
    public SeqSymmetry operate(BioSeq bioseq, List<SeqSymmetry> list) {

        SimpleSymWithProps container = new SimpleSymWithProps();
        if (list.isEmpty()) {
            return container;
        }
        int[] startEnd = getStartEnd(list.get(0), bioseq);
        SeqSpan loadSpan = new SimpleSeqSpan(startEnd[0], startEnd[1], bioseq);

        //Load Residues
        if (!bioseq.isAvailable(loadSpan)) {
            if (!GeneralLoadView.getLoadView().loadResidues(loadSpan, true)) {
                return null;
            }
        }
        SeqSymmetry topSym = list.get(0);
        List<SeqSymmetry> symList = new ArrayList<>();
        for (int i = 0; i < topSym.getChildCount(); i++) {
            symList.add(topSym.getChild(i));
        }
        HashMap<String, SeqSymmetry> map = new HashMap<>();
        applyFilters(bioseq, symList, map);
        map.values().forEach(container::addChild);
        map.clear();
        symList.clear();

        return container;
    }

    /*
     * This is specifically used to apply the filters on the given list of symmetries and updates the resultant hash map
     * with the resultant symmetries.
     */
    private void applyFilters(BioSeq bioseq, List<SeqSymmetry> list, HashMap<String, SeqSymmetry> map) {
        for (SeqSymmetry sym : list) {
            if (sym instanceof MultiTierSymWrapper) {
                applyFilters(bioseq, sym.getChild(0), map);
                applyFilters(bioseq, sym.getChild(1), map);
            } else {
                applyFilters(bioseq, sym, map);
            }
        }
    }

    private void applyFilters(BioSeq bioseq, SeqSymmetry sym, HashMap<String, SeqSymmetry> map) {
        if (noIntronFilter.filterSymmetry(bioseq, sym) && uniqueLocationFilter.filterSymmetry(bioseq, sym)) {
            updateIntronHashMap(sym, bioseq, map);
        }
    }

    @Override
    public java.util.Map<String, Class<?>> getParametersType() {
        return properties;
    }

    @Override
    public boolean setParametersValue(Map<String, Object> map) {
        if (map.isEmpty()) {
            return false;
        }

        boolean ret = true;
        for (Entry<String, Object> entry : map.entrySet()) {
            ret &= setParameterValue(entry.getKey(), entry.getValue());
        }
        return ret;
    }

    @Override
    public Object getParameterValue(String key) {
        if (Strings.isNullOrEmpty(key)) {
            return null;
        }

        if (key.equalsIgnoreCase(THRESHOLD)) {
            return threshold;
        }
        return null;
    }

    @Override
    public List<Object> getParametersPossibleValues(String key) {
        return null;
    }

    @Override
    public boolean setParameterValue(String key, Object value) {
        if (key.equalsIgnoreCase(THRESHOLD)) {
            threshold = Integer.valueOf(value.toString());
            return true;
        }
        return false;
    }

    @Override
    public boolean supportsTwoTrack() {
        return true;
    }

    @Override
    public Map<String, Object> getStyleProperties() {
        return style;
    }

    @Override
    public String getPrintableString() {
        return "";
    }

    /* This method splits the given Sym into introns and filters out the qualified Introns
     * and adds the qualified introns into map using addtoMap method
     */
    private void updateIntronHashMap(SeqSymmetry sym, BioSeq bioseq, HashMap<String, SeqSymmetry> map) {
        List<Integer> childIntronIndices = new ArrayList<>();
        int childCount = sym.getChildCount();
        int flanksLength[] = new int[2];
        childThresholdFilter.setParameterValue(childThresholdFilter.getParametersType().entrySet().iterator().next().getKey(), threshold);
        for (int i = 0; i < childCount - 1; i++) {
            if (childThresholdFilter.filterSymmetry(bioseq, sym.getChild(i)) && childThresholdFilter.filterSymmetry(bioseq, sym.getChild(i + 1))) {
                childIntronIndices.add(i);
            }
        }
        if (childIntronIndices.size() > 0) {
            SeqSymmetry intronChild, intronSym;
            intronSym = SeqUtils.getIntronSym(sym, bioseq);
            for (Integer i : childIntronIndices) {
                intronChild = intronSym.getChild(i);
                if (intronChild != null) {
                    int leftExtronLength = sym.getChild(i).getSpan(bioseq).getLength();
                    int rightExtronLength = sym.getChild(i + 1).getSpan(bioseq).getLength();
                    flanksLength[0] = leftExtronLength;
                    flanksLength[1] = rightExtronLength;
                    SeqSpan span = intronChild.getSpan(bioseq);
                    addToMap(span, map, bioseq);
                }
            }
        }
    }

    private static boolean residueCheck(String leftResidues, String rightResidues) {
        return !((leftResidues.equalsIgnoreCase("GA") && rightResidues.equalsIgnoreCase("TG"))
                || (leftResidues.equalsIgnoreCase("CT") && rightResidues.equalsIgnoreCase("AC"))
                || (leftResidues.equalsIgnoreCase("CA") && rightResidues.equalsIgnoreCase("TA"))
                || (leftResidues.equalsIgnoreCase("GT") && rightResidues.equalsIgnoreCase("AT")));
    }
    /*
     * This builds the JunctionUcscBedSym based on different properties of sym and adds the sym into map.
     */

    private void addToMap(SeqSpan span, HashMap<String, SeqSymmetry> map, BioSeq bioseq) {

        boolean currentForward = false;
        String name = "J:" + bioseq.getId() + ":" + span.getMin() + "-" + span.getMax() + ":";
        String leftResidues = bioseq.getResidues(span.getMin(), span.getMin() + 2);
        String rightResidues = bioseq.getResidues(span.getMax() - 2, span.getMax());
        if (map.containsKey(name)) {
            JunctionUcscBedSym sym = (JunctionUcscBedSym) map.get(name);
            currentForward = residueCheck(leftResidues, rightResidues);

            sym.updateScore(currentForward);
            map.put(name, sym);
        } else {
            boolean canonical = true;
            boolean rare = false;
            currentForward = residueCheck(leftResidues, rightResidues);
            // Create TopHat style flanking if requested by parameter
            int[] blockMins;
            int[] blockMaxs;

            blockMins = new int[]{span.getMin() - threshold, span.getMax()};
            blockMaxs = new int[]{span.getMin(), span.getMax() + threshold};

            JunctionUcscBedSym tempSym = new JunctionUcscBedSym(bioseq, name,
                    currentForward, blockMins, blockMaxs, canonical, rare, 0, 0, 0);
            map.put(name, tempSym);
        }
    }

    /*
     * Specific BED Sym used for Junction representation which has some extra parameters than a normal UcscBedSym
     */
    private static class JunctionUcscBedSym extends UcscBedSym {

        int positiveScore, negativeScore;
        int localScore;
        boolean canonical, rare;

        private JunctionUcscBedSym(BioSeq seq, String name, boolean forward,
                int[] blockMins, int[] blockMaxs, boolean canonical, boolean rare, int localScore, int positiveScore, int negativeScore) {
            super(name, seq, blockMins[0], blockMaxs[1], name, 1, forward,
                    0, 0, blockMins, blockMaxs);

            if (localScore > 1) {
                this.localScore = localScore;
            } else {
                this.localScore = 1;
            }

            if (positiveScore > 0) {
                this.positiveScore = positiveScore;
            } else {
                this.positiveScore = forward ? 1 : 0;
            }

            if (positiveScore > 0) {
                this.negativeScore = negativeScore;
            } else {
                this.negativeScore = forward ? 0 : 1;
            }

            this.canonical = canonical;
            this.rare = rare;
        }

        private void updateScore(boolean isForward) {
            localScore++;
            if (!canonical) {
                if (isForward) {
                    this.positiveScore++;
                } else {
                    this.negativeScore++;
                }
            }
        }

        @Override
        public float getScore() {
            return localScore;
        }

        @Override
        protected String getScoreString() {
            return Integer.toString(localScore);
        }

        @Override
        public Map<String, Object> cloneProperties() {
            Map<String, Object> tprops = super.cloneProperties();
            tprops.put("score", localScore);
            if (!canonical) {
                tprops.put("canonical", canonical);
                tprops.put("positive_score", positiveScore);
                tprops.put("negative_score", negativeScore);
            }
            return tprops;
        }

        @Override
        public Object getProperty(String key) {
            if (key.equals("score")) {
                return localScore;
            }
            return super.getProperty(key);
        }

        @Override
        public String getName() {
            return getID();
        }

        @Override
        public String getID() {
            return super.getID() + (isForward() ? "+" : "-");
        }

        @Override
        public boolean isForward() {
            return canonical ? super.isForward() : positiveScore > negativeScore;
        }

        public boolean isCanonical() {
            return canonical;
        }

        public boolean isRare() {
            return rare;
        }
    }

    @Override
    public Operator newInstance() {
        try {
            return getClass().getConstructor().newInstance();
        } catch (Exception ex) {
        }
        return null;
    }
}
