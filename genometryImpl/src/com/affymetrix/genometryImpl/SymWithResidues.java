package com.affymetrix.genometryImpl;

/**
 *
 * @author hiralv
 */
public interface SymWithResidues {

	public final static int A = 'A';
	public final static int a = 'a';
	public final static int A_pos = 0;

	public final static int T = 'T';
	public final static int t = 't';
	public final static int T_pos = 1;

	public final static int G = 'G';
	public final static int g = 'g';
	public final static int G_pos = 2;

	public final static int C = 'C';
	public final static int c = 'c';
	public final static int C_pos = 3;

	public final static int N = 'N';
	public final static int n = 'n';
	public final static int N_pos = 4;


	public void setResidues(String residues);

	public String getResidues();

	public String getResidues(int start, int end);
}
