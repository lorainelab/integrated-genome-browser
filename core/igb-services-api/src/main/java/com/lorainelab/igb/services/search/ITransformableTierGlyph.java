package org.lorainelab.igb.igb.services.search;

/**
 *
 * @author hiralv
 */
public interface ITransformableTierGlyph {

    public void setOffset(int offset);

    public int getOffset();

    public void setScale(float scale);

    public float getScale();

    public boolean isScrollingAllowed();
}
