package com.affymetrix.genoviz.event;

import java.util.EventObject;

/**
 *
 * @author hiralv
 */
public class NeoCanvasDragEvent extends EventObject {
	private static final long serialVersionUID = 1L;
	protected final int xdirection, ydirection, width, height;

	public NeoCanvasDragEvent(Object source, int xdirection, int ydirection, int width, int height) {
		super(source);
		this.xdirection = xdirection;
		this.ydirection = ydirection;
		this.width = width;
		this.height = height;
	}

	public int getXDirection(){
		return xdirection;
	}

	public int getYDirection(){
		return ydirection;
	}

	public int getWidth(){
		return width;
	}

	public int getHeight(){
		return height;
	}
}
