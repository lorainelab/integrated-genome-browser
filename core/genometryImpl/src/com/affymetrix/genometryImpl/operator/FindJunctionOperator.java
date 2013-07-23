package com.affymetrix.genometryImpl.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.filter.ChildThresholdFilter;
import com.affymetrix.genometryImpl.filter.NoIntronFilter;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.filter.UniqueLocationFilter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.UcscBedSym;
import com.affymetrix.genometryImpl.util.SeqUtils;

/**
 *
 * @author Anuj
 */
public class FindJunctionOperator extends AbstractAnnotationTransformer implements Operator{
	public static final String THRESHOLD = "threshold";
	public static final String TWOTRACKS = "twoTracks";
	public static final String UNIQUENESS = "uniqueness";
	private static final Map<String, Class<?>> properties;
	static {
		properties = new HashMap<String, Class<?>>();
		properties.put(THRESHOLD, Integer.class);
	}
		
    public static final int default_threshold = 5;
	public static final boolean default_twoTracks = true;
	public static final boolean default_uniqueness = true;
	
	private static final SymmetryFilterI noIntronFilter = new NoIntronFilter();
    private static final SymmetryFilterI childThresholdFilter = new ChildThresholdFilter();
    private static final SymmetryFilterI uniqueLocationFilter = new UniqueLocationFilter();
    
    private int threshold;
    private boolean twoTracks;
	private boolean uniqueness;
	
    public FindJunctionOperator(){
		super(FileTypeCategory.Alignment);
		threshold = default_threshold;
		twoTracks = default_twoTracks;
		uniqueness = default_uniqueness;
    }   
    
    @Override
    public String getName() {
        return "findjunctions";
    }
    
    /* This is an Operator method which is used to operates on a given list of symmetries and find the junctions between them
	 * by applying different kinds of filters and writes the resultant symmetries onto a Symmetry Container.
	 */
	
	@Override
    public SeqSymmetry operate(BioSeq bioseq, List<SeqSymmetry> list) {
		
		SimpleSymWithProps container = new SimpleSymWithProps();
		if(list.isEmpty())
			return container;
		SeqSymmetry topSym = list.get(0);
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>();
		for(int i=0; i<topSym.getChildCount(); i++){
			symList.add(topSym.getChild(i));
		}
        HashMap<String, SeqSymmetry> map = new HashMap<String ,SeqSymmetry>();
        subOperate(bioseq, symList, map);
		for(SeqSymmetry sym : map.values()){
			container.addChild(sym);
		}
        map.clear();
        symList.clear();
		
        return container;
    }
    
    /*
	 * This is specifically used to apply the filters on the given list of symmetries and updates the resultant hash map
	 * with the resultant symmetries.
	 */
	
	public void subOperate(BioSeq bioseq, List<SeqSymmetry> list, HashMap<String, SeqSymmetry> map){
      for(SeqSymmetry sym : list){
            if(noIntronFilter.filterSymmetry(bioseq, sym) && ((!uniqueness) || (uniqueness && uniqueLocationFilter.filterSymmetry(bioseq, sym)))){
                updateIntronHashMap(sym , bioseq, map, threshold, twoTracks);
            }
        }
    }
	
	@Override
	public java.util.Map<String, Class<?>> getParameters() {
		//return properties;
		return null;
	}
	
    @Override
    public boolean setParameters(Map<String, Object> map) {
        if(map.size() <= 0)
        for(Entry<String, Object> entry : map.entrySet()){
            if(entry.getKey().equalsIgnoreCase(THRESHOLD)) {
				threshold = Integer.valueOf(entry.getValue().toString());
			} else if(entry.getKey().equalsIgnoreCase(TWOTRACKS)) {
				twoTracks = Boolean.valueOf(entry.getValue().toString());
			} else if(entry.getKey().equalsIgnoreCase(UNIQUENESS)) {
				uniqueness = Boolean.valueOf(entry.getValue().toString());
			} 
        }
        return true;
    }


    @Override
    public boolean supportsTwoTrack() {
        return true;
    }
   
	@Override
	public Operator clone(){
		try {
			return getClass().getConstructor().newInstance();
		} catch (Exception ex) {
		}
		return null;
	}
	
    /* This method splits the given Sym into introns and filters out the qualified Introns
	 * and adds the qualified introns into map using addtoMap method
	 */
    private static void updateIntronHashMap(SeqSymmetry sym , BioSeq bioseq, HashMap<String, SeqSymmetry> map, int threshold, boolean twoTracks){
        List<Integer> childIntronIndices = new ArrayList<Integer>();
        int childCount = sym.getChildCount();
        childThresholdFilter.setParam(threshold);
        for(int i=0;i<childCount - 1;i++){
            if(childThresholdFilter.filterSymmetry(bioseq, sym.getChild(i)) && childThresholdFilter.filterSymmetry(bioseq, sym.getChild(i+1))){
                childIntronIndices.add(i);
            }
        }
        if(childIntronIndices.size() > 0){
			SeqSymmetry intronChild, intronSym;
            intronSym = SeqUtils.getIntronSym(sym, bioseq);
            for(Integer i : childIntronIndices){
                intronChild = intronSym.getChild(i);
                if(intronChild != null){
					SeqSpan span = intronChild.getSpan(bioseq);
                    addToMap(span, map, bioseq, threshold, twoTracks);
				}
            }
        }
    }
    
    /*
	 * This builds the JunctionUcscBedSym based on different properties of sym and adds the sym into map.
	 */
	private static void addToMap(SeqSpan span , HashMap<String, SeqSymmetry> map, BioSeq bioseq, int threshold, boolean twoTracks){
       
        boolean currentForward = false;
		String name = "J:" + bioseq.getID() + ":" + span.getMin() + "-" + span.getMax() + ":";
		if(map.containsKey(name)){
			JunctionUcscBedSym sym = (JunctionUcscBedSym)map.get(name);
			if(!twoTracks){
				currentForward = sym.isCanonical() ? sym.isForward() : (sym.isRare() ? span.isForward() : sym.isForward());					
			}
			else{
				currentForward = span.isForward();
			}
			sym.updateScore(currentForward);
		}
		else{
			boolean canonical = true;
			boolean rare = false;
			if(!twoTracks){
				String leftResidues = bioseq.getResidues(span.getMin(), span.getMin() + 2);
				String rightResidues = bioseq.getResidues(span.getMax() - 2, span.getMax());
	            if(leftResidues.equalsIgnoreCase("GT") && rightResidues.equalsIgnoreCase("AG")){
		            canonical = true;
			        currentForward = true;
				}
				else if(leftResidues.equalsIgnoreCase("CT") && rightResidues.equalsIgnoreCase("AC")){
					canonical = true;
					currentForward = false;
				}
				else if((leftResidues.equalsIgnoreCase("AT") && rightResidues.equalsIgnoreCase("AC")) || 
					    (leftResidues.equalsIgnoreCase("GC") && rightResidues.equalsIgnoreCase("AG"))){
					canonical = false;
					currentForward = true;
				}
				else if((leftResidues.equalsIgnoreCase("GT") && rightResidues.equalsIgnoreCase("AT")) || 
					    (leftResidues.equalsIgnoreCase("CT") && rightResidues.equalsIgnoreCase("GC"))){
					canonical = false;
					currentForward = false;
				}
				else{
					canonical = false;
					currentForward = span.isForward();
					rare = true;
				}
			}
			else{
				currentForward = span.isForward();
			}
			
			int blockMins[] = new int[]{span.getMin() - threshold, span.getMax()};
			int blockMaxs[] = new int[]{span.getMin(), span.getMax() + threshold};
            JunctionUcscBedSym tempSym = new JunctionUcscBedSym(bioseq, name, 
					currentForward, blockMins, blockMaxs, canonical, rare);			
            map.put(name, (SeqSymmetry)tempSym);
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
				int[] blockMins, int[] blockMaxs, boolean canonical, boolean rare) {
			super(name, seq, blockMins[0], blockMaxs[1], name, 1, forward, 
					0, 0, blockMins, blockMaxs);
			this.localScore = 1;
			this.positiveScore = forward? 1 : 0;
			this.negativeScore = forward? 0 : 1;
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
		protected String getScoreString(){
			return Integer.toString(localScore);
		}
		
		@Override
		public Map<String, Object> cloneProperties() {
			Map<String, Object> tprops = super.cloneProperties();
			tprops.put("score", localScore);
			if(!canonical){
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
			return canonical ? super.isForward() : positiveScore > negativeScore ? true : false;
		}
		
		public boolean isCanonical(){
			return canonical;
		}
		
		public boolean isRare(){
			return rare;
		}
	}
}