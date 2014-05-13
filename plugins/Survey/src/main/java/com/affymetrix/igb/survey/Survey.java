package com.affymetrix.igb.survey;

import java.util.Date;

/**
 *
 * @author hiralv
 */
public class Survey {

    private final static String ID_PREFIX = "survey_";
    private final String id, name, description, link;
    private final Date start, end;

    public Survey(String id, String name, String description, String link,
            Date start, Date end) {
        this.id = ID_PREFIX + id;
        this.name = name;
        this.description = description;
        this.link = link;
        this.start = start;
        this.end = end;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }
}
