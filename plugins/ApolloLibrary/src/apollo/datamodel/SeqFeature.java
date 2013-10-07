package apollo.datamodel;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author hiralv
 */
public class SeqFeature implements SeqFeatureI {

	protected int    low = -1;
	protected int    high = -1;
  
	public SeqFeature() {
	}

	public SeqFeature(SeqFeatureI sf) {
		initWithSeqFeat(sf);
	}
	
	public SeqFeature(int low, int high, String type) {
		init(low, high, type);
	}

	public SeqFeature(int low, int high, String type, int strand) {
		init(low, high, type, strand);
	}

	private void initWithSeqFeat(SeqFeatureI sf) {
		init(sf.getLow(), sf.getHigh(), sf.getFeatureType(), sf.getStrand());
		setName(sf.getName());
		setId(sf.getId());
		if (sf.getRefSequence() != null) {
			setRefSequence(sf.getRefSequence());
		}
	}
	
	private void init(int low, int high, String type) {
		setLow(low);
		setHigh(high);
		setFeatureType(type);
	}

	private void init(int low, int high, String type, int strand) {
		init(low, high, type);
		setStrand(strand);
	}
	
	public Object clone() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	public String getId() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private String id;
	public void setId(String id) {
		this.id = id;
	}

	public boolean hasId() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void flipFlop() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SequenceI getFeatureSequence() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getRefId() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SeqFeatureI getRefFeature() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public FeatureSetI getParent() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private SeqFeatureI refFeature;
	public void setRefFeature(SeqFeatureI refFeature) {
		this.refFeature = refFeature;
	}

	public String getTopLevelType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setTopLevelType(String type) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getProgramName() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setProgramName(String type) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getDatabase() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setDatabase(String db) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public double getScore() {
		return score;
	}

	public double getScore(String score) {
		if(scores == null){
			return 0;
		}
		Score s = scores.get("score");
		if(s == null) {
			return 0;
		}
		return s.getValue();
	}

	public Hashtable getScores() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private Map<String, Score> scores;
	private double score;
	public void setScore(double score) {
		if (scores == null) {
			scores = new HashMap<String, Score>();
		}
		Score s = scores.get("score");
		if (s == null) {
			s = new Score("score", score);
			scores.put("score", s);
			this.score = score;
		} else {
			s.setValue(score);
			this.score = score;
		}
	}

	public void addScore(Score s) {
		if (scores == null) {
			scores = new HashMap<String, Score>();
		}
		if (!scores.containsValue(s)) {
			scores.put(s.getName(), s);
		}
	}

  public void addScore(double score) {
		if (scores == null) {
			scores = new HashMap<String, Score>();
		}
		String name = scores.isEmpty() ? "score" : "score" + scores.size() + 1;

		addScore(new Score(name, score));
		if (name.equals("score")) {
			this.score = score;
		}
	}

	public void addScore(String name, double score) {
		addScore(new Score(name, score));
	}

	public void addScore(String name, String score) {
		try {
			double s = Double.valueOf(score).doubleValue();
			addScore(name, s);
		} catch (Exception ex) {
		}
	}

	private Map<String, String> properties;
	public void addProperty(String name, String property) {
		if(properties == null){
			properties = new HashMap<String, String>();
		}
		properties.put(name, property);
	}

	public void removeProperty(String name) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getProperty(String name) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Hashtable getProperties() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void replaceProperty(String key, String value) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void clearProperties() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Hashtable getPropertiesMulti() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Vector getPropertyMulti(String name) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getFrame() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setPhase(int phase) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getPhase() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getEndPhase() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int compareTo(Object sfObj) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SeqFeatureI merge(SeqFeatureI sf) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String translate() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String get_cDNA() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getCodingDNA() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getGenomicPosition(int feature_pos) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getGenomicPosForPeptidePos(int peptidePos) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getFeaturePosition(int genomic_pos) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getNumberOfChildren() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int size() {
		return features == null ? 0 : features.size();
	}

	public void clearKids() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Vector getFeatures() {
		return this.features;
	}

	public boolean hasKids() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SeqFeatureI getFeatureAt(int i) {
		if(features == null || i > features.size() - 1){
			return null;
		}
		return features.get(i);
	}

	public int getFeatureIndex(SeqFeatureI child) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	protected Vector<SeqFeatureI> features = new Vector<SeqFeatureI>(2);
	/**
	 * Add feature to end of features list, recalc low and high
	 */
	public void addFeature(SeqFeatureI feature) {
		insertFeatureAt(feature, features.size());
	}

	public void addFeature(SeqFeatureI feature, boolean sort) {
		if (feature == null) {
			throw new NullPointerException("Can't add null feature");
		}
		if (!sort) {
			insertFeatureAt(feature, features.size());
		} else {
			insertFeatureAt(feature, calculateSortPosition(feature));
		}
	}

	/**
	 * Add feature(kid) to features list at position position. Recalculate low
	 * and high
	 */
	protected void insertFeatureAt(SeqFeatureI feature, int position) {
		if (feature == null) {
			throw new NullPointerException("Can't add null feature");
		}
		if (!features.contains(feature)) {
			if (position >= features.size()) {
				features.addElement(feature);
			} else {
				features.insertElementAt(feature, position);
			}
			feature.setRefFeature(this);
		}

		// Set the reference sequence here
		if (getRefSequence() != null
				&& feature.getRefSequence() == null) {
			feature.setRefSequence(getRefSequence());
		} else if (this.getRefSequence() == null
				&& feature.getRefSequence() != null) {
			this.setRefSequence(feature.getRefSequence());
		}
		
	}

	private int calculateSortPosition(SeqFeatureI feature) {
		int setSize = size();
		int location = setSize;
		if (setSize > 0) {
			// its possible to have a child on the opposite strand?
			if (feature.getStrand() != getStrand()
					|| getStrand() == 0) {
				// just do a low to high sort
				boolean found = false;
				for (int i = 0; i < setSize && !found; i++) {
					SeqFeatureI sf = getFeatureAt(i);
					if (feature.getLow() < sf.getLow()) {
						location = i;
						found = true;
					}
				}
			} else {
				if (pastThreePrimeEnd(feature)) {
					location = setSize;
				} else if (beforeFivePrimeEnd(feature)) {
					location = 0;
				} else {
					boolean found = false;
					SeqFeatureI preceding_sf = null;
					for (int i = 0; i < setSize && !found; i++) {
						SeqFeatureI sf = getFeatureAt(i);
						if ((feature.getStrand() == 1
								&& feature.getStart() < sf.getStart()
								&& (preceding_sf == null
								|| feature.getStart() >= preceding_sf.getStart()))
								|| (feature.getStrand() == -1
								&& feature.getStart() > sf.getStart()
								&& (preceding_sf == null
								|| feature.getStart() <= preceding_sf.getStart()))) {
							location = i;
							found = true;
						}
						preceding_sf = sf;
					}
				}
			}
		}
		return location;
	}

	/**
	 * Returns true if the feature passed in has a 5 prime start that is located
	 * beyound the 3prime end of this feature. False is the feature is not
	 * 3prime of this feature
	 */
	public boolean pastThreePrimeEnd(SeqFeatureI feature) {
		if (feature.getStrand() != getStrand()) {
			return false;
		} else {
			return ((getStrand() == 1 && feature.getLow() > getHigh())
					|| (getStrand() == -1 && feature.getHigh() < getLow()));
		}
	}
	
	/**
	 * Returns true if the feature passed in has a 3 prime end that is more
	 * 5prime than this feature. False is the feature is not 5prime of this
	 * feature
	 */
	public boolean beforeFivePrimeEnd(SeqFeatureI feature) {
		if (feature.getStrand() != getStrand()) {
			return false;
		} else {
			return ((feature.getStrand() == 1 && feature.getHigh() < getLow())
					|| (feature.getStrand() == -1 && feature.getLow() > getHigh()));
		}
	}

	public int getNumberOfDescendents() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int numberOfGenerations() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isSameFeat(SeqFeatureI seqFeat) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isAncestorOf(SeqFeatureI sf) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean descendsFrom(SeqFeatureI sf) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean hasAnnotatedFeature() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isAnnot() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean hasAlignable() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean hasHitFeature() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SeqFeatureI getHitFeature() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SequenceI getHitSequence() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	protected SeqFeatureI query;
	public void setQueryFeature(SeqFeatureI queryFeat) {
		this.query = queryFeat;
	}

	public String getAlignment() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setAlignment(String alignment) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getUnpaddedAlignment() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getExplicitAlignment() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private String explicitAlignment;
	public void setExplicitAlignment(String explicitAlignment) {
		this.explicitAlignment = explicitAlignment;
	}

	public boolean haveExplicitAlignment() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean haveRealAlignment() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getCigar() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setCigar(String cigar) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void parseCigar() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getHname() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getHstart() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getHend() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getHlow() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getHhigh() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getHstrand() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean alignmentIsPeptide() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean hasPeptideSequence() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SequenceI getPeptideSequence() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SeqFeatureI getFeatureContaining(int position) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SeqFeatureI cloneFeature() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isClone() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SeqFeatureI getCloneSource() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setAnalogousOppositeStrandFeature(SeqFeatureI oppositeFeature) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean hasAnalogousOppositeStrandFeature() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SeqFeatureI getAnalogousOppositeStrandFeature() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isTranscript() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isExon() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isProtein() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isSequencingError() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isAnnotTop() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean hasTranslation() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isProteinCodingGene() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public TranslationI getTranslation() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Vector getDbXrefs() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public HashMap getGenomicErrors() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setUserObject(Object userObject) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Object getUserObject() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getCodingProperties() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public StrandedFeatureSetI getStrandedFeatSetAncestor() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isCodon() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setSyntenyLinkInfo(String linkInfo) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getSyntenyLinkInfo() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean hasSyntenyLinkInfo() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getName() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private String name;
	public void setName(String name) {
		this.name = name;
	}

	public boolean hasName() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getFeatureType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private String type;
	public void setFeatureType(String type) {
		this.type = type;
	}

	public boolean hasFeatureType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getStart() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setStart(int start) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getEnd() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setEnd(int end) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getLow() {
		return low;
	}

	public void setLow(int low) {
		this.low = low;
	}

	public int getHigh() {
		return high;
	}

	public void setHigh(int high) {
		this.high = high;
	}

	public boolean isSequenceAvailable(int position) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public SequenceI refSeq;
	public SequenceI getRefSequence() {
		return refSeq;
	}

	public boolean hasRefSequence() {
		return refSeq != null;
	}

	public void setRefSequence(SequenceI refSeq) {
		this.refSeq = refSeq;
	}

	public boolean isContainedByRefSeq() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getResidues() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private int strand;
	public int getStrand() {
		return strand;
	}

	public void setStrand(int strand) {
		this.strand = strand;
	}

	public boolean isForwardStrand() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int length() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getLeftOverlap(RangeI sf) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getRightOverlap(RangeI sf) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isExactOverlap(RangeI sf) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean contains(RangeI sf) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean contains(int position) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean overlaps(RangeI sf) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean sameRange(RangeI r) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean canHaveChildren() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public RangeI getRangeClone() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isIdentical(RangeI range) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean rangeIsUnassigned() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void convertFromBaseOrientedToInterbase() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void convertFromInterbaseToBaseOriented() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
