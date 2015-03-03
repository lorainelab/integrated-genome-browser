package apollo.datamodel;

public interface StrandedFeatureSetI extends FeatureSetI {

    public FeatureSetI getForwardSet();

    public FeatureSetI getReverseSet();

    public FeatureSetI getFeatSetForStrand(int strand);
}
