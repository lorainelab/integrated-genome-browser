
package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 *
 * @author hiralv
 */
public abstract class TransformViewModeGlyph extends AbstractViewModeGlyph {

	// Variable for transformable tier
	protected LinearTransform tier_transform = new LinearTransform();
	protected Rectangle2D.Double internal_pickRect = new Rectangle2D.Double();
	protected LinearTransform modified_view_transform = new LinearTransform();
	protected final Rectangle2D.Double modified_view_coordbox = new Rectangle2D.Double();
	protected LinearTransform incoming_view_transform;
	protected Rectangle2D.Double incoming_view_coordbox;

	// Need to redo pickTraversal, etc. to take account of transform also...
	@Override
	public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList,
			ViewI view) {

		if (!isVisible() || !intersects(pickRect, view)) {
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
}
