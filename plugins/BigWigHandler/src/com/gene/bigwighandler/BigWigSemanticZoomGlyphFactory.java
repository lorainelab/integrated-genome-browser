package com.gene.bigwighandler;

import java.awt.geom.Rectangle2D;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BBZoomLevelHeader;
import org.broad.tribble.util.SeekableStreamFactory;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.GraphIntervalSym;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class BigWigSemanticZoomGlyphFactory extends SemanticZoomGlyphFactory {
	private final MapViewGlyphFactoryI defaultGlyphFactory;
	private final MapViewGlyphFactoryI graphGlyphFactory;
	public static final String BIGWIG_ZOOM_DISPLAYER_EXTENSION = "bw";

	public BigWigSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super();
		this.defaultGlyphFactory = defaultGlyphFactory;
		this.graphGlyphFactory = graphGlyphFactory;
	}

	static boolean isBigWig(String uri) {
		if (uri == null) {
			return false;
		}
		for (String extension : BigWigHandler.EXTENSIONS) {
			if (uri.endsWith("." + extension)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasBigWig(String uri) {
		if (uri == null) {
			return false;
		}
		return GeneralUtils.urlExists(getBigWigFileName(uri, Direction.BOTH));
	}

	@Override
	public String getName() {
		return "bigwig semantic zoom " + defaultGlyphFactory.getName();
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory category) {
		return defaultGlyphFactory.isCategorySupported(category);
	}

	@Override
	public boolean isURISupported(String uri) {
		return isBigWig(uri) || hasBigWig(uri);
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return isBigWig(uri) || hasBigWig(uri);
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style,
		Direction direction, SeqMapViewExtendedI smv) {
		return new BigWigSemanticZoomGlyph(sym, style, direction, smv);
	}

	public static String getBigWigFileName(String method, Direction direction) {
		return method + "." + BIGWIG_ZOOM_DISPLAYER_EXTENSION;
	}

	// glyph class
	public class BigWigSemanticZoomGlyph extends SemanticZoomGlyphFactory.SemanticZoomGlyph {
		private static final int SEGMENT_COUNT = 256;
		private String bwUrl;
//		private final SeqSymmetry sym;
		private final ITrackStyleExtended style;
//		private final Direction direction;
		private List<BBZoomLevelHeader> levelHeaders;
//		private final List<ViewModeGlyph> levelGlyphs;
		private ViewModeGlyph defaultGlyph; 
		private final SeqMapViewExtendedI smv;
		private SymLoader detailSymL;
		private BigWigZoomSymLoader aggregateSymL;
//		private Map<BioSeq, AbstractGraphGlyph> glyphCache;

		public BigWigSemanticZoomGlyph(SeqSymmetry sym, ITrackStyleExtended style,
			Direction direction, SeqMapViewExtendedI smv) {
			super(sym, style, direction, smv);
//			this.sym = sym;
			this.style = style;
//			this.direction = direction;
			this.smv = smv;
//			glyphCache = new HashMap<BioSeq, AbstractGraphGlyph>();
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

		private AbstractGraphGlyph getEmptyGraphGlyph(BioSeq seq, int startBase, int endBase, ITrackStyleExtended trackStyle, SeqMapViewExtendedI gviewer) {
			GraphSym graf = new GraphSym(new int[]{startBase}, new int[]{endBase - startBase}, new float[]{0}, trackStyle.getMethodName(), seq);
			return (AbstractGraphGlyph)graphGlyphFactory.getViewModeGlyph(graf, trackStyle, Direction.BOTH, gviewer);
		}

		@Override
		public ViewModeGlyph getGlyph(ViewI view) {
			BioSeq seq = smv.getAnnotatedSeq();
	        int startBase = (int)Math.round(view.getCoordBox().getX());
			int length = (int)Math.round(view.getCoordBox().getWidth());
	        int endBase = startBase + length;
			try {
				ViewModeGlyph resultGlyph = null;
				if (isDetail(view)) {
					List<? extends SeqSymmetry> symList = detailSymL.getRegion(new SimpleSeqSpan(startBase, endBase, seq));
					RootSeqSymmetry rootSym;
					if (symList.size() == 1 && symList.get(0) instanceof RootSeqSymmetry) {
						rootSym = (RootSeqSymmetry)symList.get(0);
					}
					else {
						rootSym = new TypeContainerAnnot(style.getMethodName());
						for (SeqSymmetry sym : symList) {
							rootSym.addChild(sym);
						}
					}
					resultGlyph = defaultGlyphFactory.getViewModeGlyph(rootSym, style, Direction.BOTH, smv);
				}
				else {
					List<? extends SeqSymmetry> symList = aggregateSymL.getRegion(new SimpleSeqSpan(startBase, endBase, seq));
					if (symList.size() > 0) {
						GraphIntervalSym gsym = (GraphIntervalSym)symList.get(0);
						resultGlyph = (AbstractGraphGlyph)graphGlyphFactory.getViewModeGlyph(gsym, style, Direction.BOTH, smv);
					}
					if (resultGlyph == null) {
						resultGlyph = getEmptyGraphGlyph(seq, startBase, endBase, style, smv);
					}
					((AbstractGraphGlyph)resultGlyph).drawHandle(false);
				}
				if (resultGlyph == null) {
					resultGlyph = getEmptyGraphGlyph(seq, startBase, endBase, style, smv);
				}
				resultGlyph.setLabel(resultGlyph.getLabel() + " at zoom " + getZoomLevel(view));
				resultGlyph.setSelectable(false);
				double y = resultGlyph.getCoordBox().y;
				if (y == 0) {
					y = getCoordBox().y;
				}
				double height = resultGlyph.getCoordBox().height;
				if (height == 0) {
					height = getCoordBox().height;
				}
				getCoordBox().setRect(startBase, y, length, height);
				resultGlyph.setCoordBox(getCoordBox());
				resultGlyph.setVisibility(true);
				return resultGlyph;
			}
			catch (Exception x) {
				Logger logger = Logger.getLogger(this.getClass().getName());
				logger.log(Level.SEVERE, "Error in BigWig Semantic zoom", x);
				return null;
			}
		}

		@Override
		public void init(SeqSymmetry sym, ITrackStyleExtended trackStyle,
			Direction direction, SeqMapViewExtendedI gviewer) {
			BBFileReader bbReader;
			List<BBZoomLevelHeader> _levelHeaders;
			ViewModeGlyph _annotationGlyph; 
			viewModeGlyphs = new HashMap<String, ViewModeGlyph>();
			String method = (sym == null) ? trackStyle.getMethodName() : BioSeq.determineMethod(sym);
			if (direction == null) {
				direction = Direction.BOTH;
			}
			boolean isBigWig = BigWigSemanticZoomGlyphFactory.isBigWig(method);
			if (method != null) {
				if (isBigWig) {
					bwUrl = GeneralUtils.fixFileName(method);
				}
				else {
					bwUrl = GeneralUtils.fixFileName(BigWigSemanticZoomGlyphFactory.getBigWigFileName(method, direction));
				}
			}
			else {
				bwUrl = null;
			}
			try {
				bbReader = new BBFileReader(bwUrl, SeekableStreamFactory.getStreamFor(bwUrl));
				_levelHeaders = bbReader.getZoomLevels().getZoomLevelHeaders();
//					levelGlyphs = new ArrayList<ViewModeGlyph>(levelHeaders.size());
				_annotationGlyph = defaultGlyphFactory.getViewModeGlyph(sym, trackStyle, direction, gviewer);
				viewModeGlyphs.put("annotation", _annotationGlyph);
				URI bwUri = new URI(bwUrl);
				if (isBigWig) {
					detailSymL = new BigWigSymLoader(bwUri, trackStyle.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
				}
				else {
					detailSymL = FileTypeHolder.getInstance().getFileTypeHandlerForURI(method).createSymLoader(new URI(method), trackStyle.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
				}
				aggregateSymL = new BigWigZoomSymLoader(bwUri, trackStyle.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
		        if (!bbReader.isBigWigFile()) {
					_levelHeaders = null;
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "BigWigSemanticZoom failed because " + bwUrl + " is not a bigwig file");
		        }
			}
			catch (Exception x) {
				_levelHeaders = null;
				_annotationGlyph = null;
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "BigWigSemanticZoom failed reading bigwig file", x);
			}
			levelHeaders = _levelHeaders;
			BioSeq seq = gviewer.getAnnotatedSeq();
			defaultGlyph = getEmptyGraphGlyph(seq, seq.getMin(), seq.getMax(), trackStyle, gviewer);
		}

		@Override
		public ViewModeGlyph getDefaultGlyph() {
			return defaultGlyph;
		}

		@Override
		public void processParentCoordBox(Rectangle2D.Double parentCoordBox) {
			super.processParentCoordBox(parentCoordBox);
			if (defaultGlyph != null) {
				defaultGlyph.setCoordBox(parentCoordBox);
			}
		}
	}
}
