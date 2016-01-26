/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.taglets;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;
import java.util.Map;

/**
 * inspired by Cytoscape InModuleTaglet.java
 * (https://github.com/cytoscape/cytoscape-api)
 *
 * @author dcnorris
 */
public class ModuleInfoTaglet implements Taglet {

    public static final String NAME = "module.info";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean inConstructor() {
        return false;
    }

    @Override
    public boolean inField() {
        return false;
    }

    @Override
    public boolean inMethod() {
        return false;
    }

    @Override
    public boolean inOverview() {
        return false;
    }

    @Override
    public boolean inPackage() {
        return true;
    }

    @Override
    public boolean inType() {
        return true;
    }

    @Override
    public boolean isInlineTag() {
        return false;
    }

    @Override
    public String toString(Tag tag) {
        return "<hr/><p><b>Module:</b> <code>"
                + tag.text()
                + "</code></p>"
                + "<p>To use this in your app, include the following dependency in your POM:</p>"
                + "<pre>&lt;dependency>\n    &lt;groupId>org.lorainelab.igb&lt;/groupId>\n    &lt;artifactId>"
                + tag.text() + "&lt;/artifactId>\n    &lt;scope&gt;provided&lt;/scope&gt;\n&lt;/dependency></pre>";
    }

    @Override
    public String toString(Tag[] tags) {
        if (tags.length == 0) {
            return "";
        }
        return toString(tags[0]);
    }

    public static void register(Map<String, Taglet> tagletMap) {
        if (tagletMap.containsKey(NAME)) {
            tagletMap.remove(NAME);
        }
        Taglet tag = new ModuleInfoTaglet();
        tagletMap.put(NAME, tag);
    }
}
