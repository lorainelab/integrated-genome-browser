package com.gene.bigwighandler;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.igv.bbfile.BBFileHeader;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BBZoomLevelHeader;
import org.broad.igv.bbfile.BPTreeChildNodeItem;
import org.broad.igv.bbfile.BPTreeLeafNodeItem;
import org.broad.igv.bbfile.BPTreeNode;
import org.broad.igv.bbfile.ZoomDataRecord;
import org.broad.igv.bbfile.ZoomLevelIterator;
import org.broad.tribble.util.SeekableStreamFactory;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.GraphIntervalSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.SynonymLookup;

public class BigWigZoomSymLoader extends SymLoader {
	private static final int SEGMENT_COUNT = 256;
	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}
	private BBFileReader bbReader;
	private BBFileHeader bbFileHdr;
	private final List<BBZoomLevelHeader> levelHeaders;
	private List<BioSeq> chromosomeList;
	private Map<String, String> igbSeq2bwSeq;
	private Map<String, String> bwSeq2igbSeq;
	private BigWigSymLoader defaultSymLoader;

	public BigWigZoomSymLoader(URI uri, String featureName, AnnotatedSeqGroup group){
		super(uri, featureName, group);
		String uriString = GeneralUtils.fixFileName(uri.toString());
		try {
			bbReader = new BBFileReader(uriString, SeekableStreamFactory.getStreamFor(uriString));
		}
		catch (IOException x) {
			Logger.getLogger(BigWigZoomSymLoader.class.getName()).log(Level.WARNING, x.getMessage());
			levelHeaders = null;
			return;
		}
        if (!bbReader.isBigWigFile()) {
        	throw new IllegalStateException("Big Wig processor cannot handle type " + uri.toString());
        }
        bbFileHdr = bbReader.getBBFileHeader();
        if (bbFileHdr.getVersion() < 3) {
			ErrorHandler.errorPanel("file version not supported " + bbFileHdr.getVersion());
			throw new UnsupportedOperationException("file version not supported " + bbFileHdr.getVersion());
        }
		levelHeaders = bbReader.getZoomLevels().getZoomLevelHeaders();
		defaultSymLoader = new BigWigSymLoader(uri, featureName, group);
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		Map<String, BioSeq> seqMap = new HashMap<String, BioSeq>();
		for (BioSeq seq : group.getSeqList()) {
			seqMap.put(seq.getID(), seq);
		}
		chromosomeList = new ArrayList<BioSeq>();
		igbSeq2bwSeq = new HashMap<String, String>();
		bwSeq2igbSeq = new HashMap<String, String>();
		Map<String, Integer> chromosomeNameMap = new HashMap<String, Integer>();
		findAllChromosomeNamesAndSizes(bbReader.getChromosomeIDTree().getRootNode(), chromosomeNameMap);

		for (String bwSeqID : chromosomeNameMap.keySet()) {
			String cleanSeqID = bwSeqID;
			int pos = bwSeqID.indexOf((char)0); // sometimes file has chromosome with hex 00 at the end
			if (pos > -1) {
				cleanSeqID = bwSeqID.substring(0, pos);
			}
			String igbSeqID = SynonymLookup.getChromosomeLookup().getPreferredName(cleanSeqID);
			igbSeq2bwSeq.put(igbSeqID, bwSeqID);
			bwSeq2igbSeq.put(bwSeqID, igbSeqID);
			BioSeq seq = seqMap.get(igbSeqID);
			if (seq == null) {
				chromosomeList.add(group.addSeq(igbSeqID, chromosomeNameMap.get(bwSeqID), uri.toString()));
			}
			else {
				chromosomeList.add(seq);
			}
		}
		this.isInitialized = true;
	}

	@Override
	public List<BioSeq> getChromosomeList(){
		init();
		return chromosomeList;
	}

	@Override
	public List<? extends SeqSymmetry> getGenome() {
		init();
		List<BioSeq> allSeq = getChromosomeList();
		List<SeqSymmetry> retList = new ArrayList<SeqSymmetry>();
		for(BioSeq seq : allSeq){
			retList.addAll(getChromosome(seq));
		}
		return retList;
	}

	@Override
	public List<? extends SeqSymmetry> getChromosome(BioSeq seq) {
		init();
		return getRegion(new SimpleSeqSpan(seq.getMin(), seq.getMax(), seq));
	}

	/* from bbiRead.c */
	private BBZoomLevelHeader bbiBestZoom(int desiredReduction)
	/* Return zoom level that is the closest one that is less than or equal to 
	 * desiredReduction. */
	{
		if (desiredReduction <= 1) {
		    return null;
		}
		int closestDiff = Integer.MAX_VALUE;
		BBZoomLevelHeader closestLevel = null;
	
		for (BBZoomLevelHeader level : levelHeaders)
		{
		    int diff = desiredReduction - level.getReductionLevel();
		    if (diff >= 0 && diff < closestDiff)
		    {
			    closestDiff = diff;
			    closestLevel = level;
			}
		}
		return closestLevel;
	}

	public boolean isDetail(SeqSpan span) {
		int length = span.getLength();
		int basesPerSegment = length / SEGMENT_COUNT;
        BBZoomLevelHeader bestZoom = bbiBestZoom(basesPerSegment);
        return bestZoom == null;
	}

	private GraphIntervalSym getSym(int level, SeqSpan span) {
        int nextStart = -1;
        ZoomDataRecord nextRecord = null;
        ArrayList<Integer> xList = new ArrayList<Integer>();
        ArrayList<Float> yList = new ArrayList<Float>();
        ArrayList<Integer> wList = new ArrayList<Integer>();
        int startBase = span.getMin();
        int endBase = span.getMax();
        BioSeq igbSeq = span.getBioSeq();
        String bwSeq = igbSeq2bwSeq.get(igbSeq.getID());
        if (bwSeq != null) {
	        ZoomLevelIterator zoomIterator = bbReader.getZoomLevelIterator(level,
	        		bwSeq, startBase, bwSeq, endBase, true);
	        while (zoomIterator.hasNext()) {
	            nextRecord = zoomIterator.next();
	            if (nextRecord == null) {
	                break;
	            }
	            if (nextStart != -1 && nextStart != nextRecord.getChromStart()) {
	                xList.add(nextStart);
	                wList.add(nextRecord.getChromStart() - nextStart);
	                yList.add(0.0f);
	            }
	            xList.add(nextRecord.getChromStart());
	            wList.add(nextRecord.getChromEnd() - nextRecord.getChromStart());
	            yList.add(nextRecord.getSumData() / (nextRecord.getChromEnd() - nextRecord.getChromStart()));
	            nextStart = nextRecord.getChromEnd();
	        }
        }
		int[] x = new int[xList.size()];
		for (int i = 0; i < xList.size(); i++) {
			x[i] = xList.get(i);
		}
		int[] w = new int[wList.size()];
		for (int i = 0; i < wList.size(); i++) {
			w[i] = wList.get(i);
		}
		float[] y = new float[yList.size()];
		for (int i = 0; i < yList.size(); i++) {
			y[i] = yList.get(i);
		}
 		return new GraphIntervalSym(x, w, y, featureName, igbSeq);
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan span) {
		init();
        int startBase = span.getMin();
        int endBase = span.getMax();
		int length = endBase - startBase;
		int basesPerSegment = length / SEGMENT_COUNT;
        BBZoomLevelHeader bestZoom = bbiBestZoom(basesPerSegment);
        if (bestZoom == null) {
        	return defaultSymLoader.getRegion(span);
        }
        final int level = bestZoom.getZoomLevel();
        GraphIntervalSym gsym = getSym(level, span);
 		List<SeqSymmetry> regions = new ArrayList<SeqSymmetry>();
        regions.add(gsym);
		return regions;
	}

	@Override
	public List<String> getFormatPrefList() {
		return BigWigHandler.getFormatPrefList();
	}

	/**
	 * copied from BPTree.findAllChromosomeNames()
	 * @param thisNode BPTree root node
	 * @param chromosomeMap passed in map
	 */
	public void findAllChromosomeNamesAndSizes( BPTreeNode thisNode, Map<String, Integer> chromosomeMap){

        // search down the tree recursively starting with the root node
        if(thisNode.isLeaf())
        {
           // add all leaf names
           int nLeaves = thisNode.getItemCount();
           for(int index = 0; index < nLeaves; ++index){

               BPTreeLeafNodeItem leaf = (BPTreeLeafNodeItem)thisNode.getItem(index);
               chromosomeMap.put(leaf.getChromKey(), leaf.getChromSize());
           }
        }
        else {
           // get all child nodes
           int nNodes = thisNode.getItemCount();
           for(int index = 0; index < nNodes; ++index){

               BPTreeChildNodeItem childItem = (BPTreeChildNodeItem)thisNode.getItem(index);
               BPTreeNode childNode = childItem.getChildNode();

               // keep going until leaf items are extracted
               findAllChromosomeNamesAndSizes(childNode, chromosomeMap);
           }
        }
    }

	/** Returns "text/bw". */
	public String getMimeType() {
		return "text/bw";
	}
}
