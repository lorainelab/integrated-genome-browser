package com.gene.igbscript;

import com.affymetrix.igb.osgi.service.IGBService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * java ScriptEngineFactory to run scripts written in IGB scripting language
 * @see http://today.java.net/pub/a/today/2006/09/21/making-scripting-languages-jsr-223-aware.html
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
    
	@Override
    public String getEngineName() {
        return getScriptEngine().get(ScriptEngine.ENGINE).toString();
    }
    
	@Override
    public String getEngineVersion() {
        return getScriptEngine().get(ScriptEngine.ENGINE_VERSION).toString();
    }
    
	@Override
    public List<String> getExtensions() {
        return extensions;
    }
    
	@Override
    public List<String> getMimeTypes() {
        return mimeTypes;
    }
    
	@Override
    public List<String> getNames() {
        return names;
    }
    
	@Override
    public String getLanguageName() {
		Object o = getScriptEngine().get(ScriptEngine.LANGUAGE);
		if (null == o) {
			return null;
		}
        return o.toString();
    }
    
	@Override
    public String getLanguageVersion() {
		Object o = getScriptEngine().get(ScriptEngine.LANGUAGE_VERSION);
		if (null == o) {
			return null;
		}
        return o.toString();
    }
    
	@Override
    public Object getParameter(String key) {
        return getScriptEngine().get(key);
    }
    
	@Override
    public String getMethodCallSyntax(String obj, String m, String... args)  {
        StringBuilder sb = new StringBuilder();
        sb.append(obj).append(".").append(m).append("(");
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
    
	@Override
    public String getOutputStatement(String toDisplay) {
        return "print(" + toDisplay + ")";
    }
    
	@Override
    public String getProgram(String ... statements) {
        StringBuilder sb = new StringBuilder();
        int len = statements.length;
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(statements[i]);
        }
        return sb.toString();
    }
    
	@Override
    public ScriptEngine getScriptEngine() {
        return igbScriptEngine;
    }
}
