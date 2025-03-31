package org.eclipse.swt.imagej.ui;

import java.util.Map;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import ij.ImageJ;

public class Application implements IApplication {
	Display display = PlatformUI.createDisplay();
 @Override
 public Object start(final IApplicationContext context) throws Exception {
	 
	    try {
	    	
	    
	    	
	        int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
	        if (returnCode == PlatformUI.RETURN_RESTART) {
	            return IApplication.EXIT_RESTART;
	        }
	        
	        return IApplication.EXIT_OK;
	    } finally {
	        /*
	         * Close display to free all SWT resource. Important for MacOSX (former version
	         * did not close display for JOGL which has now been ported to NewtSWTCanvas).
	         * If not closed Java crashes at exit!
	         */
	        display.dispose();

	        
	    }
	}

	@Override 
	public void stop() {
	    if (!PlatformUI.isWorkbenchRunning())
	        return;
	    final IWorkbench workbench = (IWorkbench) PlatformUI.getWorkbench();
	    display.syncExec(() -> {
	        if (!display.isDisposed())
	            workbench.close();
	    });
	}

}
 