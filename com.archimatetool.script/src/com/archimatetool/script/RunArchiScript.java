/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.script;

import java.io.File;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.script.commands.CommandHandler;
import com.archimatetool.script.dom.IArchiScriptDOMFactory;
import com.archimatetool.script.views.console.ConsoleOutput;


/**
 * Script Runner
 */
public class RunArchiScript {
	private File file;
	private String script;

	public RunArchiScript(File file) {
		this.file = file;
	}
	
	public RunArchiScript(String script) {
	    this.script = script;
	}
	
	public void run() {
	    IScriptEngineProvider provider;
	    
        // Get the provider for this file type
	    if(file != null) {
	        provider = IScriptEngineProvider.INSTANCE.getProviderForFile(file);
	    }
	    else {
	        // TODO get a suitable provider for the current language
	        provider = IScriptEngineProvider.INSTANCE.getProviderByID(JSProvider.ID);
	    }
        
	    if(provider == null) {
	        throw new RuntimeException(NLS.bind("Script Provider not found for file: {0}", file)); //$NON-NLS-1$
	    }
	    
	    ScriptEngine engine = provider.createScriptEngine();
        
        defineGlobalVariables(engine);
        defineExtensionGlobalVariables(engine);
        setBindings(engine);
        
        // Start the console *after* the script engine has been created to avoid showing warning messages
        ConsoleOutput.start();

        // Initialise CommandHandler
        CommandHandler.init();

        // Initialise RefreshUIHandler
        RefreshUIHandler.init();

        try {
            if(file != null) {
                if(ScriptFiles.isLinkedFile(file)) {
                    file = ScriptFiles.resolveLinkFile(file);
                }
                provider.run(file, engine);
            }
            else {
                provider.run(script, engine);
            }
         }
        catch(Throwable ex) {
            error(ex);
        }
        finally {
            ConsoleOutput.end();
            
            RefreshUIHandler.finalise();
            
            // Add Commands to UI
            CommandHandler.finalise(file != null ? FileUtils.getFileNameWithoutExtension(file) : "Local Script"); //$NON-NLS-1$
        }
	}
	
    /**
     * Global Variables
     */
    private void defineGlobalVariables(ScriptEngine engine) {
        // Eclipse ones - these are needed for calling UI methods such as opening dialogs, windows, etc
        if(PlatformUI.isWorkbenchRunning()) {
            engine.put("workbench", PlatformUI.getWorkbench()); //$NON-NLS-1$
            engine.put("workbenchwindow", PlatformUI.getWorkbench().getActiveWorkbenchWindow()); //$NON-NLS-1$
            engine.put("shell", PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()); //$NON-NLS-1$
            
            // directory of user scripts folder
            engine.put("__SCRIPTS_DIR__", ArchiScriptPlugin.INSTANCE.getUserScriptsFolder().getAbsolutePath() + File.separator); //$NON-NLS-1$
        }
    }
    
    /**
     * Set/Remove some JS global bindings
     */
    private void setBindings(ScriptEngine engine) {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        
        // Remove these
        bindings.remove("exit"); //$NON-NLS-1$
        bindings.remove("quit"); //$NON-NLS-1$
    }

    /**
     * Declared DOM extensions are registered
     */
    private void defineExtensionGlobalVariables(ScriptEngine engine) {
        IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(IArchiScriptDOMFactory.EXTENSION_ID);
        
        for(IExtension extension : point.getExtensions()) {
            for(IConfigurationElement element : extension.getConfigurationElements()) {
                try { 
                    String variableName = element.getAttribute("variableName"); //$NON-NLS-1$
                    Object domObject = element.createExecutableExtension("class"); //$NON-NLS-1$

                    // If the class object implements IArchiScriptDOMFactory then call its getDOMroot() method as a proxy.
                    // Useful if the factory needs to instantiate the dom class object in a non-simple way.
                    if(domObject instanceof IArchiScriptDOMFactory) {
                        domObject = ((IArchiScriptDOMFactory)domObject).getDOMroot();
                    }

                    if(variableName != null && domObject != null) {
                        engine.put(variableName, domObject);
                    }
                }
                catch(CoreException ex) {
                    ex.printStackTrace();
                } 
            }
        }
    }

	private void error(Throwable ex) {
	    // The init.js function exit() works by throwing an exception with message "__EXIT__"
	    if(ex instanceof ScriptException && ex.getMessage().contains("__EXIT__")) { //$NON-NLS-1$
	        System.out.println("Exited"); //$NON-NLS-1$
	    }
	    // Other exception
	    else {
	        System.err.println("Script Error: " + ex.toString());  //$NON-NLS-1$
	    }
	}
}
