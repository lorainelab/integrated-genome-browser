package com.affymetrix.genometryImpl.parsers.useq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.affymetrix.genometryImpl.UcscBedSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.parsers.useq.*;
import com.affymetrix.genometryImpl.parsers.useq.data.*;

/**For parsing binary USeq region data into GenViz display objects.
 * @author david.nix@hci.utah.edu*/
public final class USeqRegionParser  {

	private List<SeqSymmetry> symlist = new ArrayList<SeqSymmetry>();
	private String nameOfTrack = null;
	//private boolean indexNames = false;
	private AnnotatedSeqGroup group;
	private boolean addAnnotationsToSeq;
	private ArchiveInfo archiveInfo;
	private SliceInfo sliceInfo;


	public List<SeqSymmetry> parse(InputStream istr, AnnotatedSeqGroup group, String stream_name, boolean addAnnotationsToSeq, ArchiveInfo ai) throws IOException {		
		this.group = group;
		symlist = new ArrayList<SeqSymmetry>();
		nameOfTrack = stream_name.replace(USeqUtilities.USEQ_EXTENSION_WITH_PERIOD, "");;
		this.addAnnotationsToSeq = addAnnotationsToSeq;
		this.archiveInfo = ai;

		//open IO
		BufferedInputStream bis = null;
		ZipInputStream zis = null;
		if (istr instanceof ZipInputStream) zis = (ZipInputStream)istr;
		else {
			if (istr instanceof BufferedInputStream) bis = (BufferedInputStream)istr;
			else bis = new BufferedInputStream(istr);
			zis = new ZipInputStream(bis);
		}
		
		//parse it!
		parse(zis);

		return symlist;
	}
	

	private void parse(ZipInputStream zis) throws IOException  {
		//open streams
		DataInputStream dis = new DataInputStream(zis);
		
		//make ArchiveInfo from first ZipEntry?
		if (archiveInfo == null){
			zis.getNextEntry();
			this.archiveInfo = new ArchiveInfo(zis);
		}
		
		//for each entry parse, will contain all of the same kind of data so just parse first to find out data type 
		ZipEntry ze;
		while ((ze = zis.getNextEntry()) != null){
			//set SliceInfo
			sliceInfo = new SliceInfo (ze.getName());
			String dataType = sliceInfo.getBinaryType();
			//Region
			if (USeqUtilities.REGION.matcher(dataType).matches()) parseRegionData(dis);
			//RegionScore
			else if (USeqUtilities.REGION_SCORE.matcher(dataType).matches()) parseRegionScoreData(dis);
			//RegionText
			else if (USeqUtilities.REGION_TEXT.matcher(dataType).matches()) parseRegionTextData(dis);
			//RegionScoreText
			else if (USeqUtilities.REGION_SCORE_TEXT.matcher(dataType).matches()) parseRegionScoreTextData(dis);
			//Position
			else if(USeqUtilities.POSITION.matcher(dataType).matches()) parsePositionData(dis);
			//PositionScore
			else if(USeqUtilities.POSITION_SCORE.matcher(dataType).matches()) parsePositionScoreData(dis);
			//PositionText
			else if(USeqUtilities.POSITION_TEXT.matcher(dataType).matches()) parsePositionTextData(dis);
			//PositionScoreText
			else if(USeqUtilities.POSITION_SCORE_TEXT.matcher(dataType).matches()) parsePositionScoreTextData(dis);
			//unknown!
			else {
				throw new IOException ("Unknown USeq data type, '"+dataType+"', for parsing region data from  -> '"+ze.getName()+"' in "+nameOfTrack +"\n");
			}
		}
		//close IO
		if (dis != null) dis.close();
		if (zis != null) zis.close();
	}
	
	private void parsePositionData(DataInputStream dis) throws IOException{
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//make data
		PositionData pd = new PositionData (dis, sliceInfo);
		//add syms
		Position[] r = pd.getPositions();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = r[i].getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, start, start+1, null, score, forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start+1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getPosition()+1 > seq.getLength()) seq.setLength(r[r.length-1].getPosition()+1);
	}
	
	private void parsePositionScoreData(DataInputStream dis) throws IOException{
		PositionScoreData pd = new PositionScoreData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		PositionScore[] r = pd.getPositionScores();
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = r[i].getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, start, start+1, null, r[i].getScore(), forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start+1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getPosition()+1 > seq.getLength()) seq.setLength(r[r.length-1].getPosition()+1);
	}
	
	private void parsePositionTextData(DataInputStream dis) throws IOException{
		PositionTextData pd = new PositionTextData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		PositionText[] r = pd.getPositionTexts();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = r[i].getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, start, start+1, r[i].getText(), score, forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start+1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getPosition()+1 > seq.getLength()) seq.setLength(r[r.length-1].getPosition()+1);
	}
	
	private void parsePositionScoreTextData(DataInputStream dis) throws IOException{
		PositionScoreTextData pd = new PositionScoreTextData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		PositionScoreText[] r = pd.getPositionScoreTexts();
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = r[i].getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, start, start+1, r[i].getText(), r[i].getScore(), forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start+1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameScoreText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getPosition()+1 > seq.getLength()) seq.setLength(r[r.length-1].getPosition()+1);
	}

	private void parseRegionData(DataInputStream dis) throws IOException{
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//make data
		RegionData pd = new RegionData (dis, sliceInfo);
		//add syms
		Region[] r = pd.getRegions();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, r[i].getStart(), r[i].getStop(), null, score, forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{r[i].getStart()}, new int[]{r[i].getStop()});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getStop() > seq.getLength()) seq.setLength(r[r.length-1].getStop());
	}

	private void parseRegionScoreData(DataInputStream dis) throws IOException{
		RegionScoreData pd = new RegionScoreData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		RegionScore[] r = pd.getRegionScores();
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, r[i].getStart(), r[i].getStop(), null, r[i].getScore(), forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{r[i].getStart()}, new int[]{r[i].getStop()});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getStop() > seq.getLength()) seq.setLength(r[r.length-1].getStop());
	}

	private void parseRegionTextData(DataInputStream dis) throws IOException{
		RegionTextData pd = new RegionTextData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		RegionText[] r = pd.getRegionTexts();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, r[i].getStart(), r[i].getStop(), r[i].getText(), score, forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{r[i].getStart()}, new int[]{r[i].getStop()});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getStop() > seq.getLength()) seq.setLength(r[r.length-1].getStop());
	}

	private void parseRegionScoreTextData(DataInputStream dis) throws IOException{
		RegionScoreTextData pd = new RegionScoreTextData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		RegionScoreText[] r = pd.getRegionScoreTexts();
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, r[i].getStart(), r[i].getStop(), r[i].getText(), r[i].getScore(), forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{r[i].getStart()}, new int[]{r[i].getStop()});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameScoreText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getStop() > seq.getLength()) seq.setLength(r[r.length-1].getStop());
	}
	
	/*find BioSeq or make a new one*/
	private BioSeq getBioSeq(){
		String chromosome = sliceInfo.getChromosome();
		BioSeq seq = group.getSeq(chromosome);
		if (seq == null)  {
			seq = group.addSeq(chromosome, 0);
		}
		return seq;
	}
	/*Returns true if strand is + or .*/
	private boolean forward(){
		String strand = sliceInfo.getStrand();
		return strand.equals("+") || strand.equals(".");
	}

}

