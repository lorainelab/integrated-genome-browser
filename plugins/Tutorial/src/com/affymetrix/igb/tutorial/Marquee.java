package com.affymetrix.igb.tutorial;

import com.affymetrix.igb.swing.SubRegionFinder;
import furbelow.AbstractComponentDecorator;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JComponent;

public class Marquee extends AbstractComponentDecorator {
    final int LINE_WIDTH = 4;
    static Timer timer = new Timer();

    private float phase = 0f;
    private SubRegionFinder subRegionFinder;
    public Marquee(JComponent target) {
    	this(target, null);
    }
    public Marquee(JComponent target, SubRegionFinder subRegionFinder) {
        super(target);
        this.subRegionFinder = subRegionFinder;
        // Make the ants march
        timer.schedule(new TimerTask() {
            public void run() {
                phase += 1.0f;
                repaint();
            }
        }, 0, 50);
    }
    public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics;
        g.setColor(Color.red);
        Rectangle r = getDecorationBounds();
        if (subRegionFinder != null) {
        	if (subRegionFinder.getRegion() == null) {
        		return;
        	}
        	Rectangle region = subRegionFinder.getRegion();
        	region.translate(r.x, r.y);
        	r = region;
        }
        g.setStroke(new BasicStroke(LINE_WIDTH, BasicStroke.CAP_BUTT, 
                                    BasicStroke.JOIN_ROUND, 10.0f, 
                                    new float[]{4.0f}, phase));
        g.drawRect(r.x, r.y, r.width, r.height);
    }
}
