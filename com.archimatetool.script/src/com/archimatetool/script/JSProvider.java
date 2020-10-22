/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.script;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.archimatetool.editor.utils.PlatformUtils;
import com.archimatetool.script.preferences.IPreferenceConstants;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;


/**
 * JS Provider
 */
@SuppressWarnings("removal")
public class JSProvider implements IScriptEngineProvider {
    
    public static String ID = "com.archimatetool.script.provider.js"; //$NON-NLS-1$
    
    @Override
    public void run(File file, ScriptEngine engine) throws IOException, ScriptException {
        // Init script
        init(engine);
        
        // Normalize filename so that nashorn's load() can run it
        String scriptPath = PlatformUtils.isWindows() ? file.getAbsolutePath().replace('\\', '/') : file.getAbsolutePath();

        // Evaluate the script
        engine.eval("load('" + scriptPath + "')");  //$NON-NLS-1$//$NON-NLS-2$
	}

    @Override
    public void run(String script, ScriptEngine engine) throws IOException, ScriptException {
        init(engine);
        engine.eval(script);
    }
    
    // Initialize jArchi using the provided init.js script
    private void init(ScriptEngine engine) throws IOException, ScriptException {
        URL initURL = ArchiScriptPlugin.INSTANCE.getBundle().getEntry("js/init.js"); //$NON-NLS-1$
        try(InputStreamReader initReader = new InputStreamReader(initURL.openStream());) {
            engine.eval(initReader);
        }
    }
    
    @Override
    public ScriptEngine createScriptEngine() {
        if(ArchiScriptPlugin.INSTANCE.getPreferenceStore().getInt(IPreferenceConstants.PREFS_JS_ENGINE) == 0) {
            return new ScriptEngineManager().getEngineByName("JavaScript"); //$NON-NLS-1$
        }
        else {
            return new NashornScriptEngineFactory().getScriptEngine("--language=es6"); //$NON-NLS-1$
        }
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getName() {
        return "jArchi"; //$NON-NLS-1$
    }
    
    @Override
    public String[] getSupportedFileExtensions() {
        return new String[] { ".ajs" }; //$NON-NLS-1$
    }
    
    @Override
    public Image getImage() {
        return IArchiScriptImages.ImageFactory.getImage(IArchiScriptImages.ICON_SCRIPT);
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return IArchiScriptImages.ImageFactory.getImageDescriptor(IArchiScriptImages.ICON_SCRIPT);
    }

    @Override
    public URL getNewFile() {
        return ArchiScriptPlugin.INSTANCE.getBundle().getEntry("templates/new.ajs"); //$NON-NLS-1$
    }
}
