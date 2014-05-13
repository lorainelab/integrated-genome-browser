package apollo.datamodel;

/**
 *
 * @author hiralv
 */
public class StrandedFeatureSet extends FeatureSet implements StrandedFeatureSetI{

	protected FeatureSetI forward;
	protected FeatureSetI reverse;
  
	public FeatureSetI getForwardSet() {
		return forward;
	}

	public FeatureSetI getReverseSet() {
		return reverse;
	}

	public FeatureSetI getFeatSetForStrand(int strand) {
		if (strand == 1) {
			return getForwardSet();
		}
		return getReverseSet();
	}
}
