package com.affymetrix.genometryImpl.parsers.useq;

import com.affymetrix.genometryImpl.BioSeq;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.util.regex.*;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.graph.GraphParser;
import com.affymetrix.genometryImpl.parsers.useq.data.Position;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionData;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionScore;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionScoreData;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionScoreText;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionScoreTextData;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionText;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionTextData;
import com.affymetrix.genometryImpl.parsers.useq.data.Region;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionData;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionScore;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionScoreData;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionScoreText;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionScoreTextData;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionText;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionTextData;
import com.affymetrix.genometryImpl.parsers.useq.data.USeqData;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.symmetry.impl.UcscBedSym;

/**For parsing binary USeq region data into GenViz display objects.
 * @author david.nix@hci.utah.edu*/
public final class USeqRegionParser implements GraphParser {

	private List<SeqSymmetry> symlist = new ArrayList<>();
	private String nameOfTrack = null;
	//private boolean indexNames = false;
	private AnnotatedSeqGroup group;
	private boolean addAnnotationsToSeq;
	private ArchiveInfo archiveInfo;
	public static final Pattern TAB = Pattern.compile("\\t");
	private boolean forwardStrand;
	private BioSeq bioSeq;

	/*chrom, useqArchive, useqData, group, featureName) */
	public List<SeqSymmetry> parse(USeqArchive useqArchive, USeqData[] useqData, AnnotatedSeqGroup group, String stream_name) {		
		this.group = group;
		symlist = new ArrayList<>();
		nameOfTrack = stream_name;
		archiveInfo = useqArchive.getArchiveInfo();

		try {
			//check that they are loading the data into the correct genome build
			String genomeVersion = archiveInfo.getVersionedGenome();
			if (!group.getAllVersions().isEmpty() && group.isSynonymous(genomeVersion) == false){
				throw new IOException ("\nGenome versions differ! Cannot load this useq data from "+genomeVersion+" into the current genome in view. Navigate to the correct genome and reload or add a synonym.\n");
			}

			String dataType = useqArchive.getBinaryDataType();

			//for each USeqData set, +/-/.
			for (USeqData anUseqData : useqData) {
				//any data?
				if (anUseqData == null) {
					continue;
				}
				SliceInfo sliceInfo = anUseqData.getSliceInfo();
				//set strand orientation
				if (sliceInfo.getStrand().equals("-")) {
					forwardStrand = false;
				} else {
					forwardStrand = true;
				}
				//set the BioSeq
				setBioSeq(sliceInfo.getChromosome());
				//Region
				if (USeqUtilities.REGION.matcher(dataType).matches()) {
					parseRegionData((RegionData) anUseqData);
				}
				//RegionScore
				else if (USeqUtilities.REGION_SCORE.matcher(dataType).matches()) {
					parseRegionScoreData((RegionScoreData) anUseqData);
				}
				//RegionText
				else if (USeqUtilities.REGION_TEXT.matcher(dataType).matches()) {
					parseRegionTextData((RegionTextData) anUseqData);
				}
				//RegionScoreText
				else if (USeqUtilities.REGION_SCORE_TEXT.matcher(dataType).matches()) {
					parseRegionScoreTextData((RegionScoreTextData) anUseqData);
				}
				//Position
				else if (USeqUtilities.POSITION.matcher(dataType).matches()) {
					parsePositionData((PositionData) anUseqData);
				}
				//PositionScore
				else if (USeqUtilities.POSITION_SCORE.matcher(dataType).matches()) {
					parsePositionScoreData((PositionScoreData) anUseqData);
				}
				//PositionText
				else if (USeqUtilities.POSITION_TEXT.matcher(dataType).matches()) {
					parsePositionTextData((PositionTextData) anUseqData);
				}
				//PositionScoreText
				else if (USeqUtilities.POSITION_SCORE_TEXT.matcher(dataType).matches()) {
					parsePositionScoreTextData((PositionScoreTextData) anUseqData);
				}
				//unknown!
				else {
					throw new IOException("Unknown USeq data type, '" + dataType + "', for parsing region data from " + nameOfTrack + "\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return symlist;
	}

	public List<SeqSymmetry> parse(InputStream istr, AnnotatedSeqGroup group, String stream_name, boolean addAnnotationsToSeq, ArchiveInfo ai) {		
		this.group = group;
		symlist = new ArrayList<>();
		nameOfTrack = stream_name;
		this.addAnnotationsToSeq = addAnnotationsToSeq;
		this.archiveInfo = ai;

		//open IO
		BufferedInputStream bis = null;
		ZipInputStream zis = null;

		if (istr instanceof ZipInputStream) {
			zis = (ZipInputStream)istr;
		}
		else {
			if (istr instanceof BufferedInputStream) {
				bis = (BufferedInputStream)istr;
			}
			else {
				bis = new BufferedInputStream(istr);
			}
			zis = new ZipInputStream(bis);
		}

		//parse it!
		parse(zis);

		return symlist;
	}

	private void parse(ZipInputStream zis)  {
		//open streams
		DataInputStream dis = new DataInputStream(zis);

		try {
			//make ArchiveInfo from first ZipEntry?
			if (archiveInfo == null){
				zis.getNextEntry();
				this.archiveInfo = new ArchiveInfo(zis, false);
			}

			//check that they are loading the data into the correct genome build
			String genomeVersion = archiveInfo.getVersionedGenome();
			if (!group.getAllVersions().isEmpty() && group.isSynonymous(genomeVersion) == false){
				throw new IOException ("\nGenome versions differ! Cannot load this useq data from "+genomeVersion+" into the current genome in view. Navigate to the correct genome and reload or add a synonym.\n");
			}

			//for each entry parse, will contain all of the same kind of data so just parse first to find out data type 
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null){
				//set SliceInfo
				SliceInfo sliceInfo = new SliceInfo (ze.getName());
				String dataType = sliceInfo.getBinaryType();
				//set strand orientation
				if (sliceInfo.getStrand().equals("-")) {
					forwardStrand = false;
				}
				else {
					forwardStrand = true;
				}
				//set the BioSeq
				setBioSeq(sliceInfo.getChromosome());
				//Region
				if (USeqUtilities.REGION.matcher(dataType).matches()) {
					parseRegionData(new RegionData (dis, sliceInfo));
				}
				//RegionScore
				else if (USeqUtilities.REGION_SCORE.matcher(dataType).matches()) {
					parseRegionScoreData(new RegionScoreData (dis, sliceInfo));
				}
				//RegionText
				else if (USeqUtilities.REGION_TEXT.matcher(dataType).matches()) {
					parseRegionTextData(new RegionTextData (dis, sliceInfo));
				}
				//RegionScoreText
				else if (USeqUtilities.REGION_SCORE_TEXT.matcher(dataType).matches()) {
					parseRegionScoreTextData(new RegionScoreTextData (dis, sliceInfo));
				}
				//Position
				else if(USeqUtilities.POSITION.matcher(dataType).matches()) {
					parsePositionData(new PositionData (dis, sliceInfo));
				}
				//PositionScore
				else if(USeqUtilities.POSITION_SCORE.matcher(dataType).matches()) {
					parsePositionScoreData(new PositionScoreData (dis, sliceInfo));
				}
				//PositionText
				else if(USeqUtilities.POSITION_TEXT.matcher(dataType).matches()) {
					parsePositionTextData(new PositionTextData (dis, sliceInfo));
				}
				//PositionScoreText
				else if(USeqUtilities.POSITION_SCORE_TEXT.matcher(dataType).matches()) {
					parsePositionScoreTextData(new PositionScoreTextData (dis, sliceInfo));
				}
				//unknown!
				else {
					throw new IOException ("Unknown USeq data type, '"+dataType+"', for parsing region data from  -> '"+ze.getName()+"' in "+nameOfTrack +"\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			USeqUtilities.safeClose(dis);
			USeqUtilities.safeClose(zis);
		}
	}

	private void parsePositionData(PositionData pd){
		Position[] r = pd.getPositions();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (Position aR : r) {
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = aR.getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, bioSeq, start, start + 1, null, score, forwardStrand, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start + 1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) {
				bioSeq.addAnnotation(bedline_sym);
			}
		}
		//set max
		if (r[r.length-1].getPosition()+1 > bioSeq.getLength()) {
			bioSeq.setLength(r[r.length-1].getPosition()+1);
		}
	}

	private void parsePositionScoreData(PositionScoreData pd){
		//add syms
		PositionScore[] r = pd.getPositionScores();
		for (PositionScore aR : r) {
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = aR.getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, bioSeq, start, start + 1, null, aR.getScore(), forwardStrand, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start + 1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) {
				bioSeq.addAnnotation(bedline_sym);
			}
		}
		//set max
		if (r[r.length-1].getPosition()+1 > bioSeq.getLength()) {
			bioSeq.setLength(r[r.length-1].getPosition()+1);
		}
	}

	private void parsePositionTextData(PositionTextData pd) {
		//add syms
		PositionText[] r = pd.getPositionTexts();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (PositionText aR : r) {
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = aR.getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, bioSeq, start, start + 1, aR.getText(), score, forwardStrand, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start + 1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) {
				bioSeq.addAnnotation(bedline_sym);
			}
		}
		//set max
		if (r[r.length-1].getPosition()+1 > bioSeq.getLength()) {
			bioSeq.setLength(r[r.length-1].getPosition()+1);
		}
	}

	private void parsePositionScoreTextData(PositionScoreTextData pd) {
		//add syms
		PositionScoreText[] r = pd.getPositionScoreTexts();
		for (PositionScoreText aR : r) {
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = aR.getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, bioSeq, start, start + 1, aR.getText(), aR.getScore(), forwardStrand, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start + 1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) {
				bioSeq.addAnnotation(bedline_sym);
			}
		}
		//set max
		if (r[r.length-1].getPosition()+1 > bioSeq.getLength()) {
			bioSeq.setLength(r[r.length-1].getPosition()+1);
		}
	}

	private void parseRegionData(RegionData pd) {
		//add syms
		Region[] r = pd.getRegions();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (Region aR : r) {
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, bioSeq, aR.getStart(), aR.getStop(), null, score, forwardStrand, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{aR.getStart()}, new int[]{aR.getStop()});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) {
				bioSeq.addAnnotation(bedline_sym);
			}
		}
		//set max
		if (r[r.length-1].getStop() > bioSeq.getLength()) {
			bioSeq.setLength(r[r.length-1].getStop());
		}
	}

	private void parseRegionScoreData(RegionScoreData pd) {
		//add syms
		RegionScore[] r = pd.getRegionScores();
		for (RegionScore aR : r) {
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, bioSeq, aR.getStart(), aR.getStop(), null, aR.getScore(), forwardStrand, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{aR.getStart()}, new int[]{aR.getStop()});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) {
				bioSeq.addAnnotation(bedline_sym);
			}
		}
		//set max
		if (r[r.length-1].getStop() > bioSeq.getLength()) {
			bioSeq.setLength(r[r.length-1].getStop());
		}
	}

	private void parseRegionTextData(RegionTextData pd) {
		//add syms
		RegionText[] r = pd.getRegionTexts();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (RegionText aR : r) {
			SymWithProps bedline_sym;
			//check to see if this is a bed 12
			//bed12?
			String[] tokens = TAB.split(aR.getText());
			//yes
			if (tokens.length == 7) {
				int min = aR.getStart();
				int max = aR.getStop();
				String annot_name = tokens[0];
				// thickStart field
				int thick_min = Integer.parseInt(tokens[1]);
				// thickEnd field
				int thick_max = Integer.parseInt(tokens[2]);
				// itemRgb skip
				// blockCount
				int blockCount = Integer.parseInt(tokens[4]);
				// blockSizes
				int[] blockSizes = BedParser.parseIntArray(tokens[5]);
				if (blockCount != blockSizes.length) {
					System.out.println("WARNING: block count does not agree with block sizes.  Ignoring " + annot_name + " on " + bioSeq);
					return;
				}
				// blockStarts
				int[] blockStarts = BedParser.parseIntArray(tokens[6]);
				if (blockCount != blockStarts.length) {
					System.out.println("WARNING: block size does not agree with block starts.  Ignoring " + annot_name + " on " + bioSeq);
					return;
				}
				int[] blockMins = BedParser.makeBlockMins(min, blockStarts);
				int[] blockMaxs = BedParser.makeBlockMaxs(blockSizes, blockMins);
				bedline_sym = new UcscBedSym(nameOfTrack, bioSeq, min, max, annot_name, score, forwardStrand, thick_min, thick_max, blockMins, blockMaxs);
			}
			//no
			else {
				bedline_sym = new UcscBedSym(nameOfTrack, bioSeq, aR.getStart(), aR.getStop(), aR.getText(), score, forwardStrand, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{aR.getStart()}, new int[]{aR.getStop()});
			}
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) {
				bioSeq.addAnnotation(bedline_sym);
			}
		}
		//set max
		if (r[r.length-1].getStop() > bioSeq.getLength()) {
			bioSeq.setLength(r[r.length-1].getStop());
		}
	}

	private void parseRegionScoreTextData(RegionScoreTextData pd) {
		//add syms
		RegionScoreText[] r = pd.getRegionScoreTexts();
		for (RegionScoreText aR : r) {
			SymWithProps bedline_sym;
			//check to see if this is a bed 12
			String[] tokens = TAB.split(aR.getText());
			//yes
			if (tokens.length == 7) {
				int min = aR.getStart();
				int max = aR.getStop();
				String annot_name = tokens[0];
				// thickStart field
				int thick_min = Integer.parseInt(tokens[1]);
				// thickEnd field
				int thick_max = Integer.parseInt(tokens[2]);
				// itemRgb skip
				// blockCount
				int blockCount = Integer.parseInt(tokens[4]);
				// blockSizes
				int[] blockSizes = BedParser.parseIntArray(tokens[5]);
				if (blockCount != blockSizes.length) {
					System.out.println("WARNING: block count does not agree with block sizes.  Ignoring " + annot_name + " on " + bioSeq);
					return;
				}
				// blockStarts
				int[] blockStarts = BedParser.parseIntArray(tokens[6]);
				if (blockCount != blockStarts.length) {
					System.out.println("WARNING: block size does not agree with block starts.  Ignoring " + annot_name + " on " + bioSeq);
					return;
				}
				int[] blockMins = BedParser.makeBlockMins(min, blockStarts);
				int[] blockMaxs = BedParser.makeBlockMaxs(blockSizes, blockMins);
				bedline_sym = new UcscBedSym(nameOfTrack, bioSeq, min, max, annot_name, aR.getScore(), forwardStrand, thick_min, thick_max, blockMins, blockMaxs);
			}
			//no
			else {
				bedline_sym = new UcscBedSym(nameOfTrack, bioSeq, aR.getStart(), aR.getStop(), aR.getText(), aR.getScore(), forwardStrand, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{aR.getStart()}, new int[]{aR.getStop()});
			}
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) {
				bioSeq.addAnnotation(bedline_sym);
			}
		}
		//set max
		if (r[r.length-1].getStop() > bioSeq.getLength()) {
			bioSeq.setLength(r[r.length-1].getStop());
		}
	}

	/*find BioSeq or make a new one*/
	private void setBioSeq(String chromosome){
		bioSeq = group.getSeq(chromosome);
		if (bioSeq == null)  {
			bioSeq = group.addSeq(chromosome, 0);
		}
	}

	@Override
	public List<? extends SeqSymmetry> parse(InputStream is,
			AnnotatedSeqGroup group, String nameType, String uri,
			boolean annotate_seq) throws Exception {
		if (annotate_seq) {
			return parse(is, group, uri, true, null);
		}
		else {
			//find out what kind of data it is, graph or region, from the ArchiveInfo object
			ZipInputStream zis = new ZipInputStream(is);
			zis.getNextEntry();
			ArchiveInfo archiveInfo = new ArchiveInfo(zis, false);
			if (archiveInfo.getDataType().equals(ArchiveInfo.DATA_TYPE_VALUE_GRAPH)) {
				USeqGraphParser gp = new USeqGraphParser();
				return gp.parseGraphSyms(zis, GenometryModel.getInstance(), uri, archiveInfo);
			}
			return parse(zis, group, uri, false, archiveInfo);
		}
	}

	@Override
	public List<GraphSym> readGraphs(InputStream istr, String stream_name,
			AnnotatedSeqGroup seq_group, BioSeq seq) throws IOException {
		return new USeqGraphParser().parseGraphSyms(istr, GenometryModel.getInstance(), stream_name, null);
	}

	@Override
	public void writeGraphFile(GraphSym gsym, AnnotatedSeqGroup seq_group,
			String file_name) throws IOException {
		// not processed here
	}
}

