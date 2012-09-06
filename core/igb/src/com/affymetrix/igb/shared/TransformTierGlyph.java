package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.widget.tieredmap.PaddedPackerI;
import java.util.Collections;
import java.util.HashMap;
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
public final class TransformTierGlyph extends AbstractTransformTierGlyph {
  private int fixedPixHeight = 1;
  private double spacer = 2;
  private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		PREFERENCES = Collections.unmodifiableMap(temp);
	}
  
  public TransformTierGlyph(ITrackStyleExtended style)  {
		setHitable(false);
//		setSpacer(spacer);
		setStyle(style);
  }

  public LinearTransform getTransform() {
    return tier_transform;
  }

  @Override
  protected void setModifiedViewCoords(ViewI view){
	// This works fine too. But for now not modifying it. HV 05/19/12
	// view.transformToCoords(this.getPixelBox(), modified_view_coordbox);
	view.transformToCoords(view.getPixelBox(), modified_view_coordbox);
  }
	
  public void fitToPixelHeight(ViewI view) {
    // use view transform to determine how much "more" scaling must be
    //       done within tier to keep its
    LinearTransform view_transform = view.getTransform();
    double yscale = 0.0d;
    if ( 0.0d != getCoordBox().height ) {
      yscale = fixedPixHeight / getCoordBox().height;
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
