package com.affymetrix.genometryImpl.operator;

import java.util.*;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.filter.ChildThresholdFilter;
import com.affymetrix.genometryImpl.filter.NoIntronFilter;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.filter.UniqueLocationFilter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.*;
import com.affymetrix.genometryImpl.util.SeqUtils;

/**
 *
 * @author Anuj
 */
public class FindJunctionOperator implements Operator{
	public static final String THRESHOLD = "threshold";
	public static final String TWOTRACKS = "twoTracks";
	public static final String UNIQUENESS = "uniqueness";
	
    private static final int default_threshold = 5;
	private static final boolean default_twoTracks = true;
	private static final boolean default_uniqueness = true;
	private static final SymmetryFilterI noIntronFilter = new NoIntronFilter();
    private static final SymmetryFilterI childThresholdFilter = new ChildThresholdFilter();
    private static final SymmetryFilterI uniqueLocationFilter = new UniqueLocationFilter();
    
    private int threshold;
    private boolean twoTracks;
	private boolean uniqueness;
	
    public FindJunctionOperator(){
		threshold = default_threshold;
		twoTracks = default_twoTracks;
		uniqueness = default_uniqueness;
    }   
    
    @Override
    public String getName() {
        return "findjunctions";
    }

    @Override
    public String getDisplay() {
        return GenometryConstants.BUNDLE.getString("operator_" + getName());
    }
    
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
        HashMap<String, JunctionUcscBedSym> map = new HashMap<String , JunctionUcscBedSym>();
        subOperate(bioseq, symList, map);
		for(SeqSymmetry sym : map.values()){
			container.addChild(sym);
		}
        map.clear();
        symList.clear();
		
        return container;
    }
    
    public void subOperate(BioSeq bioseq, List<SeqSymmetry> list, HashMap<String, JunctionUcscBedSym> map){
      for(SeqSymmetry sym : list){
            if(noIntronFilter.filterSymmetry(bioseq, sym) && ((!uniqueness) || (uniqueness && uniqueLocationFilter.filterSymmetry(bioseq, sym)))){
                updateIntronHashMap(sym , bioseq, map, threshold, twoTracks);
            }
        }
    }
	
    @Override
    public int getOperandCountMin(FileTypeCategory ftc) {
        return ftc == FileTypeCategory.Alignment ? 1 : 0;
    }

    @Override
    public int getOperandCountMax(FileTypeCategory ftc) {
        return ftc == FileTypeCategory.Alignment ? 1 : 0;
    }

    @Override
    public Map<String, Class<?>> getParameters() {
		Map<String, Class<?>> parameters = new HashMap<String, Class<?>>();
		parameters.put(THRESHOLD, Integer.class);
        return parameters;
    }

    @Override
    public boolean setParameters(Map<String, Object> map) {
        if(map.size() <= 0)
            return false;                
        for(String s: map.keySet()){
            if(s.equalsIgnoreCase(THRESHOLD))
                threshold = (Integer)map.get(s);
            else if(s.equalsIgnoreCase(TWOTRACKS))
                twoTracks = (Boolean)map.get(s);
            else if(s.equalsIgnoreCase(UNIQUENESS))
                uniqueness = (Boolean)map.get(s);
        }
        return true;
    }


    @Override
    public boolean supportsTwoTrack() {
        return true;
    }

    @Override
    public FileTypeCategory getOutputCategory() {
        return FileTypeCategory.Annotation;
    }
    
    //This method splits the given Sym into introns and filters out the qualified Introns
    private static void updateIntronHashMap(SeqSymmetry sym , BioSeq bioseq, HashMap<String, JunctionUcscBedSym> map, int threshold, boolean twoTracks){
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
                if(intronChild != null)
                    addToMap(intronChild, map, bioseq, threshold, twoTracks);
            }
        }
    }
    
    private static void addToMap(SeqSymmetry intronSym , HashMap<String, JunctionUcscBedSym> map, BioSeq bioseq, int threshold, boolean twoTracks){
        boolean canonical = true;
        boolean currentForward = false;
        SeqSpan span = intronSym.getSpan(bioseq);
        if(!twoTracks){
			/*int minimum = span.getMin();
			int maximum = span.getMax();
			String residueString = bioseq.getResidues(minimum, maximum);
            String leftResidues = residueString.substring(0, 2);
            String rightResidues = residueString.substring(maximum-minimum-2,maximum-minimum);*/

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
            }
        }
        else{
            currentForward = span.isForward();
        }
		
        String name = "J:" + bioseq.getID() + ":" + span.getMin() + "-" + span.getMax() + ":";
        if(map.containsKey(name)){
            map.get(name).updateScore(currentForward);
        }
        else{
			int parentStart = span.getMin() - threshold;
			int parentEnd = span.getMax() + threshold;
			int blockMins[] = new int[]{parentStart, span.getMax()};
			int blockMaxs[] = new int[]{span.getMin(), parentEnd};
            JunctionUcscBedSym tempSym = new JunctionUcscBedSym("test", bioseq, 
					parentStart, parentEnd, name, currentForward,  
					blockMins, blockMaxs, canonical);
			
            map.put(name, tempSym);
        }
    }
    
	//Helper seqsymmetry class
	private static class JunctionUcscBedSym extends UcscBedSym {

		int positiveScore, negativeScore;
		float localScore;
		boolean canonical;

		private JunctionUcscBedSym(String type, BioSeq seq, int txMin, int txMax, 
				String name, boolean forward, int[] blockMins, int[] blockMaxs, boolean canonical) {
			super(type, seq, txMin, txMax, name, 1, forward, 0, 0, blockMins, blockMaxs);
			this.localScore = 1;
			this.positiveScore = forward? 1 : 0;
			this.negativeScore = forward? 0 : 1;
			this.canonical = canonical;
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
	}
}