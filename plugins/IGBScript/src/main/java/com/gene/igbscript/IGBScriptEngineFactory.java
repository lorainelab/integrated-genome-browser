package com.gene.igbscript;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.image.exporter.service.ImageExportService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * java ScriptEngineFactory to run scripts written in IGB scripting language
 *
 * @see
 * http://today.java.net/pub/a/today/2006/09/21/making-scripting-languages-jsr-223-aware.html
 */
@Component(name = IGBScriptEngineFactory.COMPONENT_NAME, immediate = true, provide = IGBScriptEngineFactory.class)
public class IGBScriptEngineFactory implements ScriptEngineFactory {

    public static final String COMPONENT_NAME = "IGBScriptEngineFactory";
    private static final String FILEEXT = ".igb";

    private static final String[] MIMETYPES = {
        "text/plain",
        "text/igb",
        "application/igb"
    };

    private static final String[] NAMES = {
        "IGB Script Language"
    };

    private ScriptEngine igbScriptEngine;
    private List<String> extensions;
    private List<String> mimeTypes;
    private List<String> names;
    private IgbService igbService;
    private ImageExportService imageExportService;

    public IGBScriptEngineFactory() {
        extensions = Collections.nCopies(1, FILEEXT);
        mimeTypes = Arrays.asList(MIMETYPES);
        names = Arrays.asList(NAMES);
    }

    @Activate
    public void activate() {
        igbScriptEngine = new IGBScriptEngine(this, igbService, imageExportService);
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Reference
    public void setImageExportService(ImageExportService imageExportService) {
        this.imageExportService = imageExportService;
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
    public String getMethodCallSyntax(String obj, String m, String... args) {
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
    public String getProgram(String... statements) {
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
