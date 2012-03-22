package com.gene.bigwighandler;

import java.awt.geom.Rectangle2D.Double;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.SemanticZoomRule;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class BigWigSemanticZoomGlyphFactory extends SemanticZoomGlyphFactory {
	private final MapViewGlyphFactoryI defaultGlyphFactory;
	private final MapViewGlyphFactoryI graphGlyphFactory;
	public static final String BIGWIG_ZOOM_DISPLAYER_EXTENSION = "bw";
	private IGBService igbService;

	private class BigWigSemanticZoomGlyph extends SemanticZoomGlyph {
//		private final SeqMapViewExtendedI smv;
//		private boolean mousePressed;
//		private ViewI detailNeedsDrawingView;
		protected BigWigSemanticZoomGlyph(SeqSymmetry sym, ITrackStyleExtended style, Direction direction, SemanticZoomRule rule, SeqMapViewExtendedI smv) {
			super(sym, style, direction, rule);
//			this.smv = smv;
//			detailNeedsDrawingView = null;
			/*
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
			*/
			
		}
/*
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
*/
		public void processParentCoordBox(Double coordbox) {
			if (coordbox.height == 0) {
				coordbox.setRect(coordbox.x, coordbox.y, coordbox.width, style.getHeight());
			}
			((BigWigSemanticZoomRule)rule).setCoordBox(coordbox);
		}
	}
	// end glyph class

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
	protected SemanticZoomRule getRule(SeqSymmetry sym, ITrackStyleExtended style,
		Direction direction, SeqMapViewExtendedI smv) {
		return new BigWigSemanticZoomRule(sym, style, direction, smv, defaultGlyphFactory, graphGlyphFactory);
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
		SemanticZoomRule rule = getRule(sym, style, direction, smv);
		return new BigWigSemanticZoomGlyph(sym, style, direction, rule, smv);
	}

	public void setIgbService(IGBService igbService) {
		this.igbService = igbService;
	}

	public static String getBigWigFileName(String method, Direction direction) {
		return method + "." + BIGWIG_ZOOM_DISPLAYER_EXTENSION;
	}
}
