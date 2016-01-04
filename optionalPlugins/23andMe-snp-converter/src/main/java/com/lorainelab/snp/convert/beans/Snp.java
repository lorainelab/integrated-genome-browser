/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.snp.convert.beans;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 * @author Daniel
 */
public class Snp {
    
    
    /** Unique Snp identifier */
    private final String rsid;
    /** Chromosome the Snp is located on */
    private final String chromosome;
    /** Location of the Snp on the chromosome */
    private int position;
    /** Nucleotide sequence of the Snp */
    private final String genotype;

    /**
     * Constructor for each SNP object.
     * @param rsid the rsid of the SNP in the file.
     * @param chromosome the chromosome the SNP is located on.
     * @param position the location within the chromosome.
     * @param genotype the actual nucleotide sequence of the SNP.
     */
    public Snp(String rsid, String chromosome, int position, String genotype){
            checkNotNull(rsid);
            checkNotNull(chromosome);
            checkNotNull(genotype);
            this.rsid = rsid;
            this.chromosome = chromosome;
            this.position = position;
            this.genotype = genotype;
    }

    /**
     * Returns the rsid of the Snp.
     * @return rsid
     */
    public String getRsid() {
            return rsid;
    }

    /**
     * Returns the number of the chromosome the Snp is located on.
     * @return chromosome: chromosome number
     */
    public String getChromosome() {
            return chromosome;
    }

    /**
     * Returns the position of the Snp on the chromosome.
     * @return position: Snp location
     */
    public int getPosition() {
            return position;
    }

    /**
     * Returns the genotype of the Snp
     * @return genotype: Snp genotype
     */
    public String getGenotype() {
            return genotype;
    }
    
    public void setPosition(int position){
        this.position = position;
    }
}
