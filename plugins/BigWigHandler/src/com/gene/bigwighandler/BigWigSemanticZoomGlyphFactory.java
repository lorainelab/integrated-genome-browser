package com.gene.bigwighandler;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BBZoomLevelHeader;
import org.broad.igv.bbfile.ZoomDataRecord;
import org.broad.igv.bbfile.ZoomLevelIterator;
import org.broad.tribble.util.SeekableStreamFactory;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphIntervalSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.SemanticZoomRule;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class BigWigSemanticZoomGlyphFactory extends SemanticZoomGlyphFactory {
	private final MapViewGlyphFactoryI defaultGlyphFactory;
	private final MapViewGlyphFactoryI graphGlyphFactory;
	private static final int SEGMENT_COUNT = 256;
	public static final String BIGWIG_ZOOM_DISPLAYER_EXTENSION = "bw";
	private IGBService igbService;

	private class BigWigSemanticZoomGlyph extends SemanticZoomGlyph {
		private final SeqMapViewExtendedI smv;
		private boolean mousePressed;
		private ViewI detailNeedsDrawingView;
		protected BigWigSemanticZoomGlyph(SeqSymmetry sym, ITrackStyleExtended style, Direction direction, SemanticZoomRule rule, SeqMapViewExtendedI smv) {
			super(sym, style, direction, rule);
			this.smv = smv;
			detailNeedsDrawingView = null;
			Toolkit.getDefaultToolkit().addAWTEventListener(
				new AWTEventListener() {
					public void eventDispatched(AWTEvent e) {
						if (e.getID() == MouseEvent.MOUSE_PRESSED) {
							mousePressed = true;
						}
						else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
							mousePressed = false;
							if (detailNeedsDrawingView != null) {
								drawTraversal(detailNeedsDrawingView);
								detailNeedsDrawingView = null;
							}
						}
					}
				},
				AWTEvent.MOUSE_EVENT_MASK
			);	
		}
		@Override
		public void drawTraversal(ViewI view) {
			if (((BigWigSemanticZoomRule)rule).isDetail(view)) {
				if (mousePressed) {
					detailNeedsDrawingView = view;
				}
				else {
					igbService.loadAndDisplaySpan(smv.getVisibleSpan(), style.getFeature());
				}
			}
			super.drawTraversal(view);
		}
	}

	private class BigWigSemanticZoomRule implements SemanticZoomRule {
		private final String url;
		private final BBFileReader bbReader;
//		private final SeqSymmetry sym;
		private final ITrackStyleExtended style;
//		private final Direction direction;
		private final List<BBZoomLevelHeader> levelHeaders;
//		private final List<ViewModeGlyph> levelGlyphs;
		private final ViewModeGlyph annotationGlyph; 
		private final Map<String, ViewModeGlyph> allViewModeGlyphs;
		private final SeqMapViewExtendedI smv;
		private BigWigSemanticZoomRule(SeqSymmetry sym, ITrackStyleExtended style,
			Direction direction, SeqMapViewExtendedI smv) {
			super();
//			this.sym = sym;
			this.style = style;
//			this.direction = direction;
			this.smv = smv;
			String method = (sym == null) ? style.getMethodName() : BioSeq.determineMethod(sym);
			if (direction == null) {
				direction = Direction.BOTH;
			}
			url = method == null ? null : GeneralUtils.fixFileName(getBigWigFileName(method, direction));
			BBFileReader _bbReader;
			try {
				_bbReader = new BBFileReader(url, SeekableStreamFactory.getStreamFor(url));
			}
			catch (IOException x) {
				_bbReader = null;
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "BigWigSemanticZoom failed reading bigwig file", x);
			}
	        if (!_bbReader.isBigWigFile()) {
				_bbReader = null;
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "BigWigSemanticZoom failed because " + url + " is not a bigwig file");
	        }
			bbReader = _bbReader;
			levelHeaders = bbReader.getZoomLevels().getZoomLevelHeaders();
//			levelGlyphs = new ArrayList<ViewModeGlyph>(levelHeaders.size());
			allViewModeGlyphs = new HashMap<String, ViewModeGlyph>();
			annotationGlyph = defaultGlyphFactory.getViewModeGlyph(sym, style, direction, smv);
			allViewModeGlyphs.put("annotation", annotationGlyph);
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

		public boolean isDetail(ViewI view) {
			int length = (int)Math.round(view.getCoordBox().getWidth());
			int basesPerSegment = length / SEGMENT_COUNT;
	        BBZoomLevelHeader bestZoom = bbiBestZoom(basesPerSegment);
	        return bestZoom == null;
		}

		@Override
		public ViewModeGlyph getGlyph(ViewI view) {
			BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
	        int startBase = (int)Math.round(view.getCoordBox().getX());
			int length = (int)Math.round(view.getCoordBox().getWidth());
	        int endBase = startBase + length;
			int basesPerSegment = length / SEGMENT_COUNT;
	        BBZoomLevelHeader bestZoom = bbiBestZoom(basesPerSegment);
	        if (bestZoom == null) {
	        	return annotationGlyph;
	        }
	        final int level = bestZoom.getZoomLevel();
	        int nextStart = -1;
	        ZoomDataRecord nextRecord = null;
	        ArrayList<Integer> xList = new ArrayList<Integer>();
	        ArrayList<Float> yList = new ArrayList<Float>();
	        ArrayList<Integer> wList = new ArrayList<Integer>();
	        ZoomLevelIterator zoomIterator = bbReader.getZoomLevelIterator(level, seq.getID(),
	        		startBase, seq.getID(), endBase, true);
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
			String id = "???";//tier.getLabel();
	        GraphIntervalSym gsym = new GraphIntervalSym(x, w, y, id, seq);
			AbstractGraphGlyph graph_glyph = (AbstractGraphGlyph)graphGlyphFactory.getViewModeGlyph(gsym, style, Direction.BOTH, smv);
			graph_glyph.setLabel(graph_glyph.getLabel() + " at zoom " + level);
			graph_glyph.drawHandle(false);
			graph_glyph.setSelectable(false);
//			graph_glyph.setCoords(startBase, 0, length, tier.getCoordBox().getHeight());
			graph_glyph.setCoordBox(annotationGlyph.getCoordBox());
			graph_glyph.setVisibility(true);
			return graph_glyph;
		}

		@Override
		public Map<String, ViewModeGlyph> getAllViewModeGlyphs() {
			return allViewModeGlyphs;
		}

		@Override
		public ViewModeGlyph getDefaultGlyph() {
			return annotationGlyph;
		}
	}

	public BigWigSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super();
		this.defaultGlyphFactory = defaultGlyphFactory;
		this.graphGlyphFactory = graphGlyphFactory;
	}

	private String getBigWigFileName(String method, Direction direction) {
		String suffix = (direction == Direction.BOTH) ? "" : "." + direction.toString().toLowerCase();
		if (direction == Direction.FORWARD) {
			suffix = ".plus";
		}
		return method + suffix + "." + BIGWIG_ZOOM_DISPLAYER_EXTENSION;
	}

	private boolean hasBigWig(String uri) {
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
	protected SemanticZoomRule getRule(SeqSymmetry sym, ITrackStyleExtended style,
		Direction direction, SeqMapViewExtendedI smv) {
		return new BigWigSemanticZoomRule(sym, style, direction, smv);
	}

	@Override
	public boolean isURISupported(String uri) {
		return hasBigWig(uri);
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return hasBigWig(uri);
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style,
		Direction direction, SeqMapViewExtendedI smv) {
		SemanticZoomRule rule = getRule(sym, style, direction, smv);
		return new BigWigSemanticZoomGlyph(sym, style, direction, rule, smv);
	}

	public void setIgbService(IGBService igbService) {
		this.igbService = igbService;
	}
}
