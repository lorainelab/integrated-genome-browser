package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.widget.tieredmap.PaddedPackerI;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *  TransformTierGlyph.
 *  only use transform for operations on children.
 *    coordinates of the tier itself are maintained in coordinate system
 *    of the incoming view...
 *
 *  currently assuming no modifications to tier_transform, etc. are made between
 *     call to modifyView(view) and call to restoreView(view);
 *
 *  Note that if the tier has any "middleground" glyphs, 
 *     these are _not_ considered children, so transform does not apply to them
 *
 */
public final class TransformTierGlyph extends AbstractViewModeGlyph {
  private int fixedPixHeight = 1;
  private double spacer = 2;
  private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		PREFERENCES = Collections.unmodifiableMap(temp);
	}

  private final LinearTransform tier_transform = new LinearTransform();

  private LinearTransform modified_view_transform = new LinearTransform();
  private final Rectangle2D.Double modified_view_coordbox = new Rectangle2D.Double();

  private LinearTransform incoming_view_transform;
  private Rectangle2D.Double incoming_view_coordbox;

  // for caching in pickTraversal() methods
  private final Rectangle2D.Double internal_pickRect = new Rectangle2D.Double();
  
  public TransformTierGlyph(ITrackStyleExtended style)  {
		setHitable(false);
//		setSpacer(spacer);
		setStyle(style);
  }

  public LinearTransform getTransform() {
    return tier_transform;
  }

	/**
	 * Overridden to allow background shading by a collection of non-child
	 * "middle ground" glyphs.
	 * These are rendered after the solid background
	 * but before all of the children
	 * (which could be considered the "foreground").
	 */
	@Override
	public void draw(ViewI view) {
		view.transformToPixels(getCoordBox(), getPixelBox());

		getPixelBox().width = Math.max(getPixelBox().width, getMinPixelsWidth());
		getPixelBox().height = Math.max(getPixelBox().height, getMinPixelsHeight());

		Graphics g = view.getGraphics();
		Rectangle vbox = view.getPixelBox();
		setPixelBox(getPixelBox().intersection(vbox));

		if (middle_glyphs.isEmpty()) { // no middle glyphs, so use fill color to fill entire tier
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				//Hack : Add one to height to resolve black line bug.
				g.fillRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height+1);
			}
		} else {
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				//Hack : Add one to height to resolve black line bug.
				g.fillRect(getPixelBox().x, getPixelBox().y, 2 * getPixelBox().width, getPixelBox().height+1);
			}

			// cycle through "middleground" glyphs,
			//   make sure their coord box y and height are set to same as TierGlyph,
			//   then call mglyph.draw(view)
			// TODO: This will draw middle glyphs on the Whole Genome, which appears to cause problems due to coordinates vs. pixels
			// See bug 3032785
			if(other_fill_color != null){
				for (GlyphI mglyph : middle_glyphs) {
					Rectangle2D.Double mbox = mglyph.getCoordBox();
					mbox.setRect(mbox.x, getCoordBox().y, mbox.width, getCoordBox().height);
					mglyph.setColor(other_fill_color);
					mglyph.drawTraversal(view);
				}
			}
		}
/*
		if (!style.isGraphTier()) {
			// graph tiers take care of drawing their own handles and labels.
			if (shouldDrawLabel()) {
				drawLabelLeft(view);
			}
		}
*/

		super.draw(view);
	}

  @Override
  public void drawChildren(ViewI view) {

    // MODIFY VIEW
    incoming_view_transform = view.getTransform();
    incoming_view_coordbox = view.getCoordBox();

    // figure out draw transform by combining tier transform with view transform
    // should allow for arbitrarily deep nesting of transforms too, since cumulative
    //     transform is set to be view transform, and view transform is restored after draw...

    AffineTransform trans2D = new AffineTransform();
    trans2D.translate(0.0, incoming_view_transform.getTranslateY());
    trans2D.scale(1.0, incoming_view_transform.getScaleY());
    trans2D.translate(1.0, tier_transform.getTranslateY());
    trans2D.scale(1.0, tier_transform.getScaleY());

    modified_view_transform = new LinearTransform();
	modified_view_transform.setTransform(
			incoming_view_transform.getScaleX(),0,0,trans2D.getScaleY(),
			incoming_view_transform.getTranslateX(),trans2D.getTranslateY());
    view.setTransform(modified_view_transform);

    // need to set view coordbox based on nested transformation
    //   (for methods like withinView(), etc.)
    view.transformToCoords(view.getPixelBox(), modified_view_coordbox);
    view.setCoordBox(modified_view_coordbox);

    // CALL NORMAL DRAWCHILDREN(), BUT WITH MODIFIED VIEW
    super.drawChildren(view);

    // RESTORE ORIGINAL VIEW
    view.setTransform(incoming_view_transform);
    view.setCoordBox(incoming_view_coordbox);
  }

  public void fitToPixelHeight(ViewI view) {
    // use view transform to determine how much "more" scaling must be
    //       done within tier to keep its
    LinearTransform view_transform = view.getTransform();
    double yscale = 0.0d;
    if ( 0.0d != getCoordBox().height ) {
      yscale = (double)fixedPixHeight / getCoordBox().height;
    }
	if ( 0.0d != view_transform.getScaleY() ) {
	  yscale = yscale / view_transform.getScaleY();
	}
    tier_transform.setTransform(tier_transform.getScaleX(),0,0,tier_transform.getScaleY() * yscale,tier_transform.getTranslateX(),tier_transform.getTranslateY());
    getCoordBox().height = getCoordBox().height * yscale;
  }


  @Override
  public void setStyle(ITrackStyleExtended style) {
	super.setStyle(style);
	FasterExpandPacker expand_packer = new FasterExpandPacker();
	((PaddedPackerI) expand_packer).setParentSpacer(spacer);
	setPacker(expand_packer);
  }

  // Need to redo pickTraversal, etc. to take account of transform also...
  @Override
  public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList,
                            ViewI view)  {

		if (!isVisible() || !intersects(pickRect,view)) {
			return;
		}
		if (hit(pickRect, view)) {
			if (!pickList.contains(this)) {
				pickList.add(this);
			}
		}

		if (getChildren() != null) {
			// modify pickRect on the way in
			//   (transform from view coords to local (tier) coords)
			//    [ an inverse transform? ]
			LinearTransform.inverseTransform(tier_transform, pickRect, internal_pickRect);

			for (GlyphI child : getChildren()) {
				child.pickTraversal(internal_pickRect, pickList, view);
			}
		}
	}


  // Don't move children! Just change tier's transform offset.
  @Override
  public void moveRelative(double diffx, double diffy) {
   getCoordBox().x += diffx;
   getCoordBox().y += diffy;
   tier_transform.setTransform(tier_transform.getScaleX(), 0, 0,
		   tier_transform.getScaleY(), tier_transform.getTranslateX(),
		   tier_transform.getTranslateY() + diffy);
  }

  public void setFixedPixHeight(int pix_height) {
    fixedPixHeight = pix_height;
  }

  public int getFixedPixHeight() {
    return fixedPixHeight;
  }

  /**
   * Should not be called.
   */
  @Override
  public void setPreferredHeight(double height, ViewI view) {
    throw new UnsupportedOperationException("Transform tiers cannot change height.");
  }

  @Override
  public int getActualSlots() {
	return 1;
  }

  @Override
  public Map<String, Class<?>> getPreferences() {
	return new HashMap<String, Class<?>>(PREFERENCES);
  }

  @Override
  public void setPreferences(Map<String, Object> preferences) {
  }

  @Override
  public boolean isManuallyResizable() {
    return false;
  }

}
