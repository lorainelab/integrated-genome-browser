package com.affymetrix.genometryImpl;

/**
 *
 * @author hiralv
 */
public interface SymWithResidues {

	public enum ResiduesChars {
		A(new char[]{'A','a'},0),
		T(new char[]{'T','t'},1),
		G(new char[]{'G','g'},2),
		C(new char[]{'C','c'},3),
		N(new char[]{'N','n', 'D', 'd'},4);

		char[] chars;
		int value;
		
		private ResiduesChars(char[] chars, int value){
			this.chars = chars;
			this.value = value;
		}

		public int getValue(){
			return value;
		}

		@Override
		public String toString(){
			return String.valueOf(chars[0]);
		}

		public boolean equal(char ch){
			for(char c : chars){
				if(c == ch){
					return true;
				}
			}
			return false;
		}
		
		public static ResiduesChars valueOf(int i){
			for(ResiduesChars ch : ResiduesChars.values()){
				if(ch.value == i){
					return ch;
				}
			}

			return null;
		}

		public static int valueOf(char c){
			for(ResiduesChars ch : ResiduesChars.values()){
				for(char cc : ch.chars){
					if(cc == c){
						return ch.value;
					}
				}
			}

			return -1;
		}
	};

	public void setResidues(String residues);

	public String getResidues();

	public String getResidues(int start, int end);
}
