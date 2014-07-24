package com.gene.bigbedhandler;

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
import org.broad.igv.bbfile.BPTreeChildNodeItem;
import org.broad.igv.bbfile.BPTreeLeafNodeItem;
import org.broad.igv.bbfile.BPTreeNode;
import org.broad.igv.bbfile.BedFeature;
import org.broad.igv.bbfile.BigBedIterator;
import org.broad.tribble.util.SeekableStreamFactory;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;

public class BigBedSymLoader extends SymLoader {
	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}
	private BBFileReader bbReader;
	private BBFileHeader bbFileHdr;
	private List<BioSeq> chromosomeList;
	private Map<String, String> cleanSeq2Seq;

	public BigBedSymLoader(URI uri, String featureName, AnnotatedSeqGroup group){
		super(uri, featureName, group);
	}

	private void initbbReader(){
		String uriString = GeneralUtils.fixFileName(uri.toString());
		try {
			bbReader = new BBFileReader(uriString, SeekableStreamFactory.getStreamFor(uriString));
		} catch (IOException x) {
			Logger.getLogger(BigBedSymLoader.class.getName()).log(Level.WARNING, x.getMessage());
		}
		if (!bbReader.isBigBedFile()) {
			throw new IllegalStateException("Big Bed processor cannot open " + uri.toString());
		}
		bbFileHdr = bbReader.getBBFileHeader();
		if (bbFileHdr.getVersion() < 3) {
			ErrorHandler.errorPanel("file version not supported " + bbFileHdr.getVersion());
			throw new UnsupportedOperationException("file version not supported " + bbFileHdr.getVersion());
		}
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
		cleanSeq2Seq = new HashMap<String, String>();
		Map<String, Integer> chromosomeNameMap = new HashMap<String, Integer>();
		findAllChromosomeNamesAndSizes(bbReader.getChromosomeIDTree().getRootNode(), chromosomeNameMap);

		for (String seqID : chromosomeNameMap.keySet()) {
			String cleanSeqID = seqID;
			int pos = seqID.indexOf((char)0);
			if (pos > -1) {
				cleanSeqID = seqID.substring(0, pos);
			}
			cleanSeq2Seq.put(cleanSeqID, seqID);
			BioSeq seq = seqMap.get(cleanSeqID);
			if (seq == null) {
				chromosomeList.add(group.addSeq(cleanSeqID, chromosomeNameMap.get(seqID), uri.toString()));
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
		String seqString = cleanSeq2Seq.get(seq.getID());
		return parse(seq, bbReader.getBigBedIterator(seqString, 0, seqString, Integer.MAX_VALUE, true));
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan span) {
		List<? extends SeqSymmetry> regions = null;
		init();
		String seqString = cleanSeq2Seq.get(span.getBioSeq().getID());
		try {
			regions = parse(span.getBioSeq(), bbReader.getBigBedIterator(seqString, span.getStart(), seqString, span.getEnd(), true));
		}
		catch (RuntimeException x) {
			if (x.getMessage().startsWith("No wig data found")) {
				Logger.getLogger(BigBedSymLoader.class.getName()).log(Level.WARNING, x.getMessage());
				regions = new ArrayList<SeqSymmetry>();
			}
			else {
				throw x;
			}
		}
		return regions;
	}

	private List<? extends SeqSymmetry> parse(BioSeq seq, BigBedIterator bedIterator){
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>();
		try {
	        BedFeature bedFeature = null;
	        while (bedIterator.hasNext() && (!Thread.currentThread().isInterrupted())) {
	        	bedFeature = bedIterator.next();
	            if (bedFeature == null) {
	                break;
	            }
	            SimpleSymWithProps sym = new SimpleSymWithProps();
	            SeqSpan span = new SimpleSeqSpan(bedFeature.getStartBase(), bedFeature.getEndBase(), seq);
	            sym.addSpan(span);
	            sym.setProperty("type", featureName);
	            String[] restOfFields = bedFeature.getRestOfFields();
	            if (restOfFields != null) {
	            	for (int i = 0; i < restOfFields.length; i++) {
	            		if (restOfFields[i] != null && restOfFields[i].trim().length() > 0) {
	            			sym.setProperty("restOfFields " + (i + 1), restOfFields[i]);
	            		}
	            	}
	            }
	            symList.add(sym);
	        }
		} catch (Exception ex) {
			Logger.getLogger(BigBedSymLoader.class.getName()).log(Level.SEVERE, null, ex);
		}
		return symList;
	}

	@Override
	public List<String> getFormatPrefList() {
		return BigBedHandler.getFormatPrefList();
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

	/** Returns "text/bb". */
	public String getMimeType() {
		return "text/bb";
	}
}
