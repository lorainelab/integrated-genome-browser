package com.affymetrix.genoviz.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.Border;

public class PartialLineBorder
		implements Border {

	int lineThickness;
	Insets insets;
	Color color;

	public PartialLineBorder(Color lineColor, int lineThickness, String edges) {
		this.color = lineColor;
		this.lineThickness = lineThickness;
		this.insets = new Insets(0, 0, 0, 0);
		edges = edges.toUpperCase();
		if (edges.contains("T")) { //Top
			this.insets.top = lineThickness;
		}
		if (edges.contains("B")) { //Bottom
			this.insets.bottom = lineThickness;
		}
		if (edges.contains("L")) { //Left
			this.insets.left = lineThickness;
		}
		if (edges.contains("R")) {//Right
			this.insets.right = lineThickness;
		}

	}

	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Color oldColor = g.getColor();
		g.setColor(this.color);

		for (int i = 0; i < this.lineThickness; i++) {
			if (this.insets.left != 0) {
				g.drawLine(x + i, y, x + i, y + height - 1);
			}
			if (this.insets.right != 0) {
				g.drawLine(x + width - i - 1, y, x + width - i - 1, y + height - 1);
			}
			if (this.insets.top != 0) {
				g.drawLine(x, y + i, x + width - 1, y + i);
			}
			if (this.insets.bottom != 0) {
				g.drawLine(x, y + height - 1 - i, x + width - 1, y + height - 1 - i);
			}
		}

		g.setColor(oldColor);
	}

	public boolean isBorderOpaque() {
		return true;
	}

	public Insets getBorderInsets(Component c) {
		return new Insets(this.insets.top, this.insets.left, this.insets.bottom, this.insets.right);
	}

	public Insets getBorderInsets(Component c, Insets insets) {
		insets.left = this.insets.left;
		insets.top = this.insets.top;
		insets.right = this.insets.right;
		insets.bottom = this.insets.bottom;
		return insets;
	}
}
