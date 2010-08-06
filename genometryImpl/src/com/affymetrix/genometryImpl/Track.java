package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.general.GenericFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A track is a (GenericFeature, BioSeq, String) combination that uniquely identifies a list of symmetries.
 * A track is currently not positive- or negative-stranded.
 * @author jnicol
 */
public class Track {
	static Map<TrackKey, Track> trackMap = new HashMap<TrackKey, Track>();

	public final Set<SeqSymmetry> symList = new HashSet<SeqSymmetry>();	// set of symmetries in the track

	//public final List<DependentData> dependentData = new ArrayList<DependentData>();

	public static Track getTrack(GenericFeature feature, BioSeq seq, String trackID) {
		TrackKey tKey = new TrackKey(feature, seq, trackID);
		if (!trackMap.containsKey(tKey)) {
			Track t = new Track();
			trackMap.put(tKey, t);
		}
		return trackMap.get(tKey);
	}

}

class TrackKey {
	private final GenericFeature feature;
	private final BioSeq seq;	// chromosome
	private final String trackID;	// a track ID that's unique for this chromosome/feature

	public TrackKey(GenericFeature feature, BioSeq seq, String trackID) {
		this.feature = feature;
		this.seq = seq;
		this.trackID = trackID;
	}

	@Override
	public boolean equals(Object o) {
		TrackKey tKey = (TrackKey)o;
		return this.feature == tKey.feature && this.seq == tKey.seq && this.trackID.equals(tKey.trackID);
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 23 * hash + (this.feature != null ? this.feature.hashCode() : 0);
		hash = 23 * hash + (this.seq != null ? this.seq.hashCode() : 0);
		hash = 23 * hash + (this.trackID != null ? this.trackID.hashCode() : 0);
		return hash;
	}
}
