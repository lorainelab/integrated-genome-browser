/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.filter.ChildThresholdFilter;
import com.affymetrix.genometryImpl.filter.NoIntronFilter;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.filter.UniqueLocationFilter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.symmetry.UcscBedSym;
import com.affymetrix.genometryImpl.util.SeqUtils;
import java.util.*;


/**
 *
 * @author Anuj
 */
public class FindJunctionOperator implements Operator{
    private static final int default_threshold = 5;
    private SymmetryFilterI noIntronFilter = new NoIntronFilter();
    private SymmetryFilterI childThresholdFilter = new ChildThresholdFilter();
    private SymmetryFilterI uniqueLocationFilter = new UniqueLocationFilter();
    private int threshold = default_threshold;
    private boolean twoTracks, uniqueness;
    public FindJunctionOperator(){
        this.threshold = default_threshold;
        this.twoTracks = true;
        this.uniqueness = true;
    }   
    
    @Override
    public String getName() {
        return "findjunction";
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
        Collection<JunctionUcscBedSym> symmetrySet = map.values();
        Object syms[] = symmetrySet.toArray();
        for(int i=0;i<syms.length;i++){
            container.addChild((JunctionUcscBedSym)syms[i]);
        }
        map.clear();
        symmetrySet.clear();
        return container;
    }
    
    public void subOperate(BioSeq bioseq, List<SeqSymmetry> list, HashMap<String, JunctionUcscBedSym> map){
      for(SeqSymmetry sym : list){
            if(noIntronFilter.filterSymmetry(bioseq, sym) && ((!uniqueness) || (uniqueness && uniqueLocationFilter.filterSymmetry(bioseq, sym)))){
                updateIntronHashMap(sym , bioseq, map);
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
        return null;
    }

    @Override
    public boolean setParameters(Map<String, Object> map) {
        if(map.size() <= 0)
            return false;                
        for(String s: map.keySet()){
            if(s.equalsIgnoreCase("threshold"))
                threshold = (Integer)map.get(s);
            else if(s.equalsIgnoreCase("twoTracks"))
                twoTracks = (Boolean)map.get(s);
            else if(s.equalsIgnoreCase("uniqueness"))
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
    private void updateIntronHashMap(SeqSymmetry sym , BioSeq bioseq, HashMap<String, JunctionUcscBedSym> map){
        List<Integer> childIntronIndices = new ArrayList<Integer>();
        int childCount = sym.getChildCount();
        SeqSymmetry intronChild, intronSym;
        childThresholdFilter.setParam(threshold);
        for(int i=0;i<childCount - 1;i++){
            if(childThresholdFilter.filterSymmetry(bioseq, sym.getChild(i)) && childThresholdFilter.filterSymmetry(bioseq, sym.getChild(i+1))){
                childIntronIndices.add(i);
            }
        }
        if(childIntronIndices.size() > 0){
            intronSym = SeqUtils.getIntronSym(sym, bioseq);
            for(Integer i : childIntronIndices){
                intronChild = intronSym.getChild(i);
                if(intronChild != null)
                    addToMap(intronChild, map, bioseq);
            }
        }
    }
    
    private void addToMap(SeqSymmetry intronSym , HashMap<String, JunctionUcscBedSym> map, BioSeq bioseq){
        int blockMins[] = new int[2];
        int blockMaxs[] = new int[2];
        boolean canonical = true;
		String residueString;
        String rightResidues= "",leftResidues= "";
        SeqSpan span = intronSym.getSpan(bioseq);
        blockMins[0] = span.getMin() - threshold;
        blockMins[1] = span.getMax();
        blockMaxs[0] = span.getMin();
        blockMaxs[1] = span.getMax() + threshold;
        String name;
        boolean currentForward = false;
        JunctionUcscBedSym tempSym;
        int minimum = span.getMin();
        int maximum = span.getMax();
        if(!twoTracks){
			residueString = bioseq.getResidues(minimum, maximum);
            leftResidues = residueString.substring(0, 2);
            rightResidues = residueString.substring(maximum-minimum-2,maximum-minimum);
            boolean c;
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
        name = "J:" + bioseq.getID() + ":" + span.getMin() + "-" + span.getMax() + ":";
        String key = name;
        if(map.containsKey(key)){
            map.get(key).updateScore(currentForward);
        }
        else{
            tempSym = new JunctionUcscBedSym("test", bioseq, span.getMin()-threshold,
               span.getMax()+threshold, name, 1, currentForward, 0, 0, blockMins, blockMaxs, currentForward?1:0, currentForward?0:1, canonical);
            map.put(key,tempSym);
        }
    }
    
}
class JunctionUcscBedSym extends UcscBedSym{
    
    int positiveScore, negativeScore;
    float localScore = 1;
    boolean canonical;
    public JunctionUcscBedSym(String type, BioSeq seq, int txMin, int txMax, String name, float score,boolean forward, 
            int cdsMin, int cdsMax, int[] blockMins, int[] blockMaxs, int positiveScore, int negativeScore, boolean canonical){
        super(type, seq, txMin, txMax, name, score, forward, cdsMin, cdsMax, blockMins, blockMaxs);
        this.positiveScore = positiveScore;
        this.negativeScore = negativeScore;
        this.canonical = canonical;
    }
    
    public void updateScore(boolean isForward){
       localScore++;
        if(!canonical){
            if(isForward)
                this.positiveScore++;
            else
               this.negativeScore++;
        }
    }
    @Override
     public float getScore(){
        return localScore;
     }
    
     @Override
     public String getName(){
         return getID();
     }
     
     @Override
     public String getID(){
         return super.getID() + (isForward()? "+" : "-");
     }
     
     @Override
     public boolean isForward(){
         return canonical ? super.isForward() : positiveScore > negativeScore? true: false;
     }
}