/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.tests.performance;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.eclipse.ui.tests.util.UITestCase;

/**
 * Baseclass for simple performance tests.
 * 
 * @since 3.1
 */
public abstract class BasicPerformanceTest extends UITestCase {

	private PerformanceTester tester;
    private IProject testProject;
    final private boolean tagAsGlobalSummary;

    public BasicPerformanceTest(String testName) {
    	this(testName, false);
    }
    
    /**
     * @param testName
     */
    public BasicPerformanceTest(String testName, boolean tagAsGlobalSummary) {
        super(testName);
        this.tagAsGlobalSummary = tagAsGlobalSummary;
    }
    
    /**
     * Answers whether this test should be tagged globally.
     * 
     * @return whether this test should be tagged globally
     */
    protected boolean shouldGloballyTag() {
    	return tagAsGlobalSummary;
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.ui.tests.util.UITestCase#doSetUp()
	 */
	protected void doSetUp() throws Exception {
	    super.doSetUp();
	    
	    fWorkbench.getActiveWorkbenchWindow().getActivePage().setPerspective(
                fWorkbench.getPerspectiveRegistry().findPerspectiveWithId(
                        UIPerformanceTestSetup.PERSPECTIVE));
	    
	    tester = new PerformanceTester(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.tests.util.UITestCase#doTearDown()
	 */
	protected void doTearDown() throws Exception {
	    super.doTearDown();
	    tester.dispose();
	}
	
	protected IProject getProject() {
	    if (testProject == null) {
	        IWorkspace workspace = ResourcesPlugin.getWorkspace();
	        testProject = workspace.getRoot().getProject(UIPerformanceTestSetup.PROJECT_NAME);
	    }
	    return testProject;
	}	

	/**
	 * Asserts default properties of the measurements captured for this test
	 * case.
	 * 
	 * @throws RuntimeException
	 *             if the properties do not hold
	 */
	public void assertPerformance() {
		tester.assertPerformance();
	}

	/**
	 * Asserts that the measurement specified by the given dimension is within a
	 * certain range with respect to some reference value. If the specified
	 * dimension isn't available, the call has no effect.
	 * 
	 * @param dim
	 *            the Dimension to check
	 * @param lowerPercentage
	 *            a negative number indicating the percentage the measured value
	 *            is allowed to be smaller than some reference value
	 * @param upperPercentage
	 *            a positive number indicating the percentage the measured value
	 *            is allowed to be greater than some reference value
	 * @throws RuntimeException
	 *             if the properties do not hold
	 */
	public void assertPerformanceInRelativeBand(Dimension dim,
			int lowerPercentage, int upperPercentage) {
		tester.assertPerformanceInRelativeBand(dim, lowerPercentage, upperPercentage);
	}

	public void commitMeasurements() {
		tester.commitMeasurements();
	}

	/**
	 * Called from within a test case immediately before the code to measure is
	 * run. It starts capturing of performance data. Must be followed by a call
	 * to {@link PerformanceTestCase#stopMeasuring()}before subsequent calls to
	 * this method or {@link PerformanceTestCase#commitMeasurements()}.
	 */
	public void startMeasuring() {
		tester.startMeasuring();
	}

	public void stopMeasuring() {
		tester.stopMeasuring();
	}

	/**
	 * Mark the scenario of this test case to be included into the global
	 * performance summary. The summary shows the given dimension of the
	 * scenario and labels the scenario with the short name.
	 * 
	 * @param shortName
	 *            a short (shorter than 40 characters) descritive name of the
	 *            scenario
	 * @param dimension
	 *            the dimension to show in the summary
	 */
	public void tagAsGlobalSummary(String shortName, Dimension dimension) {
		tester.tagAsGlobalSummary(shortName, dimension);
	}

	/**
	 * Mark the scenario represented by the given PerformanceMeter to be
	 * included into the global performance summary. The summary shows the given
	 * dimensions of the scenario and labels the scenario with the short name.
	 * 
	 * @param shortName
	 *            a short (shorter than 40 characters) descritive name of the
	 *            scenario
	 * @param dimensions
	 *            an array of dimensions to show in the summary
	 */
	public void tagAsGlobalSummary(String shortName, Dimension[] dimensions) {
		tester.tagAsGlobalSummary(shortName, dimensions);
	}
	
}
