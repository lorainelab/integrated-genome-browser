/*
 * http://today.java.net/pub/a/today/2006/09/21/making-scripting-languages-jsr-223-aware.html
 */

package com.gene.igbscript;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import com.affymetrix.igb.osgi.service.IGBService;

/**
 * java ScriptEngineFactory to run scripts written in IGB scripting language
 */
public class IGBScriptEngineFactory implements ScriptEngineFactory {
    
    private static final String FILEEXT = ".igb";
    
    private static final String [] MIMETYPES = {
        "text/plain",
        "text/igb",
        "application/igb"
    };
    
    private static final String [] NAMES = {
        "IGB Script Language"
    };
    
    private ScriptEngine igbScriptEngine;
    private List<String> extensions;
    private List<String> mimeTypes;
    private List<String> names;
    
    public IGBScriptEngineFactory(IGBService igbService) {
        igbScriptEngine = new IGBScriptEngine(this, igbService);
        extensions = Collections.nCopies(1, FILEEXT);
        mimeTypes = Arrays.asList(MIMETYPES);
        names = Arrays.asList(NAMES);
    }
    
    public String getEngineName() {
        return getScriptEngine().get(ScriptEngine.ENGINE).toString();
    }
    
    public String getEngineVersion() {
        return getScriptEngine().get(ScriptEngine.ENGINE_VERSION).toString();
    }
    
    public List<String> getExtensions() {
        return extensions;
    }
    
    public List<String> getMimeTypes() {
        return mimeTypes;
    }
    
    public List<String> getNames() {
        return names;
    }
    
    public String getLanguageName() {
        return getScriptEngine().get(ScriptEngine.LANGUAGE).toString();
    }
    
    public String getLanguageVersion() {
        return getScriptEngine().get(ScriptEngine.LANGUAGE_VERSION).toString();
    }
    
    public Object getParameter(String key) {
        return getScriptEngine().get(key).toString();
    }
    
    public String getMethodCallSyntax(String obj, String m, String... args)  {
        StringBuffer sb = new StringBuffer();
        sb.append(obj + "." + m + "(");
        int len = args.length;
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(args[i]);
        }
        sb.append(")");
        return sb.toString();
    }
    
    public String getOutputStatement(String toDisplay) {
        return "print(" + toDisplay + ")";
    }
    
    public String getProgram(String ... statements) {
        StringBuffer sb = new StringBuffer();
        int len = statements.length;
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(statements[i]);
        }
        return sb.toString();
    }
    
    public ScriptEngine getScriptEngine() {
        return igbScriptEngine;
    }
}
