package com.gene.bigwighandler;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.igv.bbfile.BBFileHeader;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BPTreeChildNodeItem;
import org.broad.igv.bbfile.BPTreeLeafNodeItem;
import org.broad.igv.bbfile.BPTreeNode;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.WigItem;
import org.broad.tribble.util.SeekableStreamFactory;

import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.impl.GraphIntervalSym;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;

public class BigWigSymLoader extends SymLoader {
	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.AUTOLOAD);
	}
	private BBFileReader bbReader;
	private BBFileHeader bbFileHdr;
	private List<BioSeq> chromosomeList;
	private Map<String, String> realSeq2Seq;

	public BigWigSymLoader(URI uri, String featureName, AnnotatedSeqGroup group){
		super(uri, featureName, group);
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
		
		initbbReader();
		
		Map<String, BioSeq> seqMap = new HashMap<String, BioSeq>();
		for (BioSeq seq : group.getSeqList()) {
			seqMap.put(seq.getID(), seq);
		}
		chromosomeList = new ArrayList<BioSeq>();
		realSeq2Seq = new HashMap<String, String>();
		Map<String, Integer> chromosomeNameMap = new HashMap<String, Integer>();
		findAllChromosomeNamesAndSizes(bbReader.getChromosomeIDTree().getRootNode(), chromosomeNameMap);

		for (String seqID : chromosomeNameMap.keySet()) {
			String cleanSeqID = seqID;
			int pos = seqID.indexOf((char)0); // sometimes file has chromosome with hex 00 at the end
			if (pos > -1) {
				cleanSeqID = seqID.substring(0, pos);
			}
			BioSeq seq = group.addSeq(cleanSeqID, chromosomeNameMap.get(seqID), uri.toString());
			chromosomeList.add(seq);
			realSeq2Seq.put(seq.getID(), seqID);
		}
		this.isInitialized = true;
	}

	
	private void initbbReader() {
		String uriString = GeneralUtils.fixFileName(uri.toString());
		try {
			bbReader = new BBFileReader(uriString, SeekableStreamFactory.getStreamFor(uriString));
		}
		catch (IOException x) {
			Logger.getLogger(BigWigSymLoader.class.getName()).log(Level.WARNING, x.getMessage());
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
		String seqString = realSeq2Seq.get(seq.getID());
		return parse(seq, bbReader.getBigWigIterator(seqString, 0, seqString, Integer.MAX_VALUE, true));
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan span) {
		List<? extends SeqSymmetry> regions = null;
		init();
		String seqString = realSeq2Seq.get(span.getBioSeq().getID());
		try {
			regions = parse(span.getBioSeq(), bbReader.getBigWigIterator(seqString, span.getStart(), seqString, span.getEnd(), true));
		}
		catch (RuntimeException x) {
			if (x.getMessage().startsWith("No wig data found")) {
				Logger.getLogger(BigWigSymLoader.class.getName()).log(Level.WARNING, x.getMessage());
				regions = new ArrayList<SeqSymmetry>();
			}
			else {
				throw x;
			}
		}
		return regions;
	}

	private List<? extends SeqSymmetry> parse(BioSeq seq, BigWigIterator wigIterator){
		IntArrayList xData = new IntArrayList();
		FloatArrayList yData = new FloatArrayList();
		IntArrayList wData = new IntArrayList();
		try {
	        WigItem wigItem = null;
	        while (wigIterator.hasNext() && (!Thread.currentThread().isInterrupted())) {
	        	wigItem = wigIterator.next();
	            if (wigItem == null) {
	                break;
	            }
	    		xData.add(wigItem.getStartBase());
	    		yData.add(wigItem.getWigValue());
	    		wData.add(wigItem.getEndBase() - wigItem.getStartBase());
	        }
		} catch (Exception ex) {
			Logger.getLogger(BigWigSymLoader.class.getName()).log(Level.SEVERE, "error parsing BigWig file", ex);
		}
		int dataSize = xData.size();
		int[] xList = Arrays.copyOf(xData.elements(), dataSize);
		float[] yList = Arrays.copyOf(yData.elements(), dataSize);
		int[] wList =  Arrays.copyOf(wData.elements(), dataSize);
		GraphIntervalSym graphIntervalSym = new GraphIntervalSym(xList, wList, yList, featureName, seq);
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>();
		symList.add(graphIntervalSym);
		return symList;
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
