package com.affymetrix.genometryImpl.symmetry;


import com.affymetrix.genometryImpl.BioSeq;

public class JunctionUcscBedSym extends UcscBedSym{
    
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