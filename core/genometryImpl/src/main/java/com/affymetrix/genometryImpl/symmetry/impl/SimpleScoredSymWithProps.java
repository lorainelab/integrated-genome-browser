package com.affymetrix.genometryImpl.symmetry.impl;

import com.affymetrix.genometryImpl.Scored;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class SimpleScoredSymWithProps extends SimpleSymWithProps implements Scored {

    final static String SCORE = "score";
    private final float score;

    public SimpleScoredSymWithProps(float score) {
        super();
        this.score = score;
    }

    @Override
    public float getScore() {
        return score;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = super.getProperties();
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        properties.put(SCORE, score);

        return properties;
    }

    @Override
    public Object getProperty(String name) {
        if (SCORE.equalsIgnoreCase(name)) {
            return getScore();
        }
        return super.getProperty(name);
    }
}
