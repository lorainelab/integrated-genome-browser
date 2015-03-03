package apollo.datamodel;

/**
 *
 * @author hiralv
 */
public class FeaturePair extends SeqFeature implements FeaturePairI {

    private SeqFeatureI hit;

    public FeaturePair(SeqFeatureI query, SeqFeatureI hit) {
        this.query = query;
        setHitFeature(hit);
    }

    public void setHstrand(int strand) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setHstart(int start) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setHend(int end) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setHlow(int low) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setHhigh(int high) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SeqFeatureI getQueryFeature() {
        return query;
    }

    public void setHitFeature(SeqFeatureI sf) {
        this.hit = sf;
        hit.setQueryFeature(this);
    }

    private SequenceI hitSeq;

    public void setHitSequence(SequenceI seq) {
        this.hitSeq = seq;
    }
}
