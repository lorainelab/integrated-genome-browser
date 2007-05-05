package com.affymetrix.igb.event;

import java.awt.Component;
import java.awt.Rectangle;
import com.affymetrix.genoviz.event.NeoRubberBandEvent;

public class VirtualRubberBandEvent extends NeoRubberBandEvent {
  Rectangle pixbox;

  public VirtualRubberBandEvent(Component source, int id, long when,
                                int modifiers, int x, int y, int clickCount,
                                boolean popupTrigger, Rectangle pbox) {
    super(source, id, when, modifiers, x, y, clickCount, popupTrigger, null);
    pixbox = pbox;
  }

  public Rectangle getPixelBox() {
    return pixbox;
  }

}
