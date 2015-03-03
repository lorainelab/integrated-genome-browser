package apollo.datamodel;

import apollo.util.FeatureListI;

/**
 *
 * @author hiralv
 */
public class FeatureSet extends SeqFeature implements FeatureSetI {

    public void deleteFeature(SeqFeatureI feature) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SeqFeatureI deleteFeatureAt(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getIndexContaining(int pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean hasDescendents() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void adjustEdges() {
        low = 1000000000;
        high = -1000000000;
        for (int i = 0; i < size(); i++) {
            adjustEdges(getFeatureAt(i));
        }
    }

    public void adjustEdges(SeqFeatureI span) {
        boolean adjusted = false;
        if (span.getLow() < getLow()) {
            setLow(span.getLow());
            adjusted |= true;
        }
        if (span.getHigh() > high) {
            setHigh(span.getHigh());
            adjusted |= true;
        }
        if (adjusted && getRefFeature() != null) {
            ((FeatureSetI) getRefFeature()).adjustEdges();
        }
    }

    @Override
    protected void insertFeatureAt(SeqFeatureI feature, int position) {
        super.insertFeatureAt(feature, position);
        // sets low and high according to lowest low and highest high in kids
        adjustEdges(feature);
    }

    public void sort(int sortStrand) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void sort(int sortStrand, boolean byLow) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean hasNameBeenSet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public FeatureListI findFeaturesByHitName(String hname) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public FeatureListI findFeaturesByName(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public FeatureListI findFeaturesByName(String name, boolean kidNamesOverParent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public FeatureListI findFeaturesByAllNames(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public FeatureListI findFeaturesByAllNames(String name, boolean useRegExp) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public FeatureListI findFeaturesByAllNames(String searchString, boolean useRegExp, boolean kidNamesOverParent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getPositionFrom(int at_pos, int offset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean withinCDS(int pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setProteinCodingGene(boolean isProteinCodingGene) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean unConventionalStart() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getStartCodon() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getStartAA() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean hasReadThroughStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String readThroughStopResidue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setReadThroughStop(boolean readthrough) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setReadThroughStop(String residue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int readThroughStopPosition() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int plus1FrameShiftPosition() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int minus1FrameShiftPosition() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean setPlus1FrameShiftPosition(int shift_pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean setMinus1FrameShiftPosition(int shift_pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isSequencingErrorPosition(int base_position) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SequenceEditI getSequencingErrorAtPosition(int base_position) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SequenceEditI[] buildEditList() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private SequenceI hitSeq;

    public void setHitSequence(SequenceI seq) {
        this.hitSeq = seq;
    }

    public int getSplicedLength() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isFlagSet(int flag) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setFlag(boolean state, byte mask) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean setTranslationStart(int pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean setTranslationStart(int pos, boolean set_end) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setTranslationEnd(int pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean hasTranslationStart() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getTranslationStart() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean hasTranslationEnd() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setMissing5prime(boolean partial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isMissing5prime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setMissing3prime(boolean partial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isMissing3prime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getTranslationEnd() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void calcTranslationStartForLongestPeptide() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setTranslationEndFromStart() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getLastBaseOfStopCodon() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RangeI getTranslationRange() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setPeptideValidity(boolean validity) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
