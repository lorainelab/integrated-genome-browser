package apollo.util;

import apollo.datamodel.SeqFeatureI;

public interface FeatureListI {

    public int size();

    public FeatureListI getAllLeaves();

    public SeqFeatureI getFeature(int i);
}
