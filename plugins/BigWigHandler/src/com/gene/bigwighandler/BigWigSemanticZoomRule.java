package com.gene.bigwighandler;

import java.awt.geom.Rectangle2D;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BBZoomLevelHeader;
import org.broad.tribble.util.SeekableStreamFactory;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.GraphIntervalSym;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SemanticZoomRule;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;

public class BigWigSemanticZoomRule implements SemanticZoomRule {
	private static final int SEGMENT_COUNT = 256;
//	private final MapViewGlyphFactoryI defaultGlyphFactory;
	private final MapViewGlyphFactoryI graphGlyphFactory;
	private final String url;
//	private final SeqSymmetry sym;
	private final ITrackStyleExtended style;
//	private final Direction direction;
	private final List<BBZoomLevelHeader> levelHeaders;
//	private final List<ViewModeGlyph> levelGlyphs;
	private final ViewModeGlyph defaultGlyph; 
	private final Map<String, ViewModeGlyph> allViewModeGlyphs;
	private final SeqMapViewExtendedI smv;
	private SymLoader detailSymL;
	private BigWigZoomSymLoader aggregateSymL;
	private Rectangle2D.Double coordbox;
	private Map<BioSeq, AbstractGraphGlyph> glyphCache;

	public BigWigSemanticZoomRule(SeqSymmetry sym, ITrackStyleExtended style,
		Direction direction, SeqMapViewExtendedI smv,
		MapViewGlyphFactoryI defaultGlyphFactory,
		MapViewGlyphFactoryI graphGlyphFactory) {
		super();
//		this.sym = sym;
		this.style = style;
//		this.direction = direction;
		this.smv = smv;
//		this.defaultGlyphFactory = defaultGlyphFactory;
		this.graphGlyphFactory = graphGlyphFactory;
		glyphCache = new HashMap<BioSeq, AbstractGraphGlyph>();
		String method = (sym == null) ? style.getMethodName() : BioSeq.determineMethod(sym);
		if (direction == null) {
			direction = Direction.BOTH;
		}
		boolean isBigWig = BigWigSemanticZoomGlyphFactory.isBigWig(method);
		if (method != null) {
			if (isBigWig) {
				url = GeneralUtils.fixFileName(method);
			}
			else {
				url = GeneralUtils.fixFileName(BigWigSemanticZoomGlyphFactory.getBigWigFileName(method, direction));
			}
		}
		else {
			url = null;
		}
		BBFileReader bbReader;
		List<BBZoomLevelHeader> _levelHeaders;
		ViewModeGlyph _annotationGlyph; 
		allViewModeGlyphs = new HashMap<String, ViewModeGlyph>();
		try {
			bbReader = new BBFileReader(url, SeekableStreamFactory.getStreamFor(url));
			_levelHeaders = bbReader.getZoomLevels().getZoomLevelHeaders();
//			levelGlyphs = new ArrayList<ViewModeGlyph>(levelHeaders.size());
			_annotationGlyph = defaultGlyphFactory.getViewModeGlyph(sym, style, direction, smv);
			allViewModeGlyphs.put("annotation", _annotationGlyph);
			URI uri = new URI(url);
			if (isBigWig) {
				detailSymL = new BigWigSymLoader(uri, style.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
			}
			else {
				detailSymL = FileTypeHolder.getInstance().getFileTypeHandlerForURI(method).createSymLoader(new URI(method), style.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
			}
			aggregateSymL = new BigWigZoomSymLoader(uri, style.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
	        if (!bbReader.isBigWigFile()) {
				_levelHeaders = null;
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "BigWigSemanticZoom failed because " + url + " is not a bigwig file");
	        }
		}
		catch (Exception x) {
			_levelHeaders = null;
			_annotationGlyph = null;
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "BigWigSemanticZoom failed reading bigwig file", x);
		}
		levelHeaders = _levelHeaders;
		BioSeq seq = smv.getAnnotatedSeq();
		defaultGlyph = getEmptyGraphGlyph(seq, seq.getMin(), seq.getMax());
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

	private int getZoomLevel(ViewI view) {
		int length = (int)Math.round(view.getCoordBox().getWidth());
		int basesPerSegment = length / SEGMENT_COUNT;
        BBZoomLevelHeader bestZoom = bbiBestZoom(basesPerSegment);
        final int level = (bestZoom == null) ? -1 : bestZoom.getZoomLevel();
        return level;
	}

	public boolean isDetail(ViewI view) {
		return getZoomLevel(view) <= 0;
	}

	public void setCoordBox(Rectangle2D.Double coordBox) {
		this.coordbox = coordBox;
	}

	private AbstractGraphGlyph getEmptyGraphGlyph(BioSeq seq, int startBase, int endBase) {
		GraphSym graf = new GraphSym(new int[]{startBase}, new int[]{endBase - startBase}, new float[]{0}, "", seq);
		return (AbstractGraphGlyph)graphGlyphFactory.getViewModeGlyph(graf, style, Direction.BOTH, smv);
	}

	@Override
	public ViewModeGlyph getGlyph(ViewI view) {
		BioSeq seq = smv.getAnnotatedSeq();
        int startBase = (int)Math.round(view.getCoordBox().getX());
		int length = (int)Math.round(view.getCoordBox().getWidth());
        int endBase = startBase + length;
		SymLoader symL = isDetail(view) ? detailSymL : aggregateSymL;
		try {
			List<? extends SeqSymmetry> symList = symL.getRegion(new SimpleSeqSpan(startBase, endBase, seq));
			AbstractGraphGlyph graph_glyph = null;
			if (symList.size() > 0) {
				GraphIntervalSym gsym = (GraphIntervalSym)symList.get(0);
				graph_glyph = (AbstractGraphGlyph)graphGlyphFactory.getViewModeGlyph(gsym, style, Direction.BOTH, smv);
			}
			if (graph_glyph == null) {
				graph_glyph = getEmptyGraphGlyph(seq, startBase, endBase);
			}
			if (graph_glyph != null) {
				graph_glyph.setLabel(graph_glyph.getLabel() + " at zoom " + getZoomLevel(view));
				graph_glyph.drawHandle(false);
				graph_glyph.setSelectable(false);
				double y = graph_glyph.getCoordBox().y;
				if (y == 0) {
					y = coordbox.y;
				}
				double height = graph_glyph.getCoordBox().height;
				if (height == 0) {
					height = coordbox.height;
				}
				coordbox.setRect(graph_glyph.getCoordBox().x, y, graph_glyph.getCoordBox().width, height);
				graph_glyph.setCoordBox(coordbox);
				graph_glyph.setVisibility(true);
			}
			return graph_glyph;
		}
		catch (Exception x) {
			Logger logger = Logger.getLogger(this.getClass().getName());
			logger.log(Level.SEVERE, "Error in BigWig Semantic zoom", x);
			return null;
		}
	}

	@Override
	public Map<String, ViewModeGlyph> getAllViewModeGlyphs() {
		return allViewModeGlyphs;
	}

	@Override
	public ViewModeGlyph getDefaultGlyph() {
		return defaultGlyph;
	}
}
