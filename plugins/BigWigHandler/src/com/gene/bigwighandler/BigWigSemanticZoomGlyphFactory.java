package com.gene.bigwighandler;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BBZoomLevelHeader;
import org.broad.tribble.util.SeekableStreamFactory;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.IndexedSemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class BigWigSemanticZoomGlyphFactory extends IndexedSemanticZoomGlyphFactory {
	public static final String BIGWIG_ZOOM_DISPLAYER_EXTENSION = "bw";

	public BigWigSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super(defaultGlyphFactory, graphGlyphFactory);
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

	@Override
	public String getName() {
		return "bigwig semantic zoom " + defaultGlyphFactory.getName();
	}

	@Override
	public boolean isURISupported(String uri) {
		return isBigWig(uri) || hasIndex(uri);
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return isBigWig(uri) || hasIndex(uri);
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style,
		Direction direction, SeqMapViewExtendedI smv) {
		return new BigWigSemanticZoomGlyph(sym, style, direction, smv);
	}

	@Override
	public String getIndexedFileName(String method, Direction direction) {
		return method + "." + BIGWIG_ZOOM_DISPLAYER_EXTENSION;
	}

	// glyph class
	public class BigWigSemanticZoomGlyph extends IndexedSemanticZoomGlyphFactory.IndexedSemanticZoomGlyph {
		private static final int SEGMENT_COUNT = 256;
		private List<BBZoomLevelHeader> levelHeaders;
//		private final List<ViewModeGlyph> levelGlyphs;

		public BigWigSemanticZoomGlyph(SeqSymmetry sym, ITrackStyleExtended style,
			Direction direction, SeqMapViewExtendedI smv) {
			super(sym, style, direction, smv);
		}

		@Override
		public boolean isPreLoaded() {
			return true;
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

		@Override
		public boolean isDetail(ViewI view) {
			return getZoomLevel(view) <= 0;
		}

		@Override
		public void init(SeqSymmetry sym, ITrackStyleExtended trackStyle,
			Direction direction, SeqMapViewExtendedI gviewer) {
			super.init(sym, trackStyle, direction, gviewer);
			BBFileReader bbReader;
			List<BBZoomLevelHeader> _levelHeaders;
			String method = (sym == null) ? trackStyle.getMethodName() : BioSeq.determineMethod(sym);
			if (direction == null) {
				direction = Direction.BOTH;
			}
			boolean isBigWig = BigWigSemanticZoomGlyphFactory.isBigWig(method);
			String bwUrl;
			if (method != null) {
				if (isBigWig) {
					bwUrl = GeneralUtils.fixFileName(method);
				}
				else {
					bwUrl = GeneralUtils.fixFileName(getIndexedFileName(method, direction));
				}
			}
			else {
				bwUrl = null;
			}
			try {
				bbReader = new BBFileReader(bwUrl, SeekableStreamFactory.getStreamFor(bwUrl));
				_levelHeaders = bbReader.getZoomLevels().getZoomLevelHeaders();
//					levelGlyphs = new ArrayList<ViewModeGlyph>(levelHeaders.size());
				URI bwUri = new URI(bwUrl);
				if (isBigWig) {
					detailSymL = new BigWigSymLoader(bwUri, trackStyle.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
				}
				else {
					detailSymL = FileTypeHolder.getInstance().getFileTypeHandlerForURI(method).createSymLoader(new URI(method), trackStyle.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
				}
				summarySymL = new BigWigZoomSymLoader(bwUri, trackStyle.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
		        if (!bbReader.isBigWigFile()) {
					_levelHeaders = null;
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "BigWigSemanticZoom failed because " + bwUrl + " is not a bigwig file");
		        }
			}
			catch (Exception x) {
				_levelHeaders = null;
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "BigWigSemanticZoom failed reading bigwig file", x);
			}
			levelHeaders = _levelHeaders;
		}
	}
	// end glyph class
}
