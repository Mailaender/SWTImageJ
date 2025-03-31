package org.eclipse.swt.imagej.ui;

import org.eclipse.ui.application.WorkbenchAdvisor;

import ij.ImageJ;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	@Override
	public void postStartup() {
		ImageJ ij = new ImageJ();
    	ij.exitWhenQuitting(true);
	}
	@Override
	public String getInitialWindowPerspectiveId() {
		// TODO Auto-generated method stub
		return null;
	}

}
