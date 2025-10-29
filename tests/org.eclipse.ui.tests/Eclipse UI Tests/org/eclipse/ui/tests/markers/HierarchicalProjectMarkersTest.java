/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.tests.markers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.internal.views.markers.MarkerResourceUtil;
import org.eclipse.ui.views.markers.internal.MarkerFilter;
import org.eclipse.ui.views.markers.internal.ProblemFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for hierarchical project support in the Problems View.
 * Tests that when a parent project is selected, markers from nested child
 * projects are also shown.
 */
public class HierarchicalProjectMarkersTest {

	private IProject parentProject;
	private IProject childProject;

	@Before
	public void setUp() throws CoreException {
		// Create parent project
		parentProject = ResourcesPlugin.getWorkspace().getRoot().getProject("ParentProject");
		if (!parentProject.exists()) {
			parentProject.create(null);
		}
		parentProject.open(null);

		// Create child project nested under parent
		childProject = ResourcesPlugin.getWorkspace().getRoot().getProject("ChildProject");
		if (!childProject.exists()) {
			// Set child project location to be under parent project
			IPath parentLocation = parentProject.getLocation();
			assertNotNull("Parent project location should not be null", parentLocation);
			IPath childLocation = parentLocation.append("child");
			
			// Create project description with custom location
			org.eclipse.core.resources.IProjectDescription description = 
				ResourcesPlugin.getWorkspace().newProjectDescription("ChildProject");
			description.setLocation(childLocation);
			childProject.create(description, null);
		}
		childProject.open(null);
	}

	@After
	public void tearDown() throws CoreException {
		if (childProject != null && childProject.exists()) {
			childProject.delete(true, true, null);
		}
		if (parentProject != null && parentProject.exists()) {
			parentProject.delete(true, true, null);
		}
	}

	@Test
	public void testGetProjectsAsCollectionIncludesNestedChildren() {
		IResource[] resources = new IResource[] { parentProject };
		Collection<IProject> projects = MarkerResourceUtil.getProjectsAsCollection(resources);
		
		assertNotNull("Projects collection should not be null", projects);
		assertTrue("Projects should contain parent project", projects.contains(parentProject));
		assertTrue("Projects should contain nested child project", projects.contains(childProject));
		assertEquals("Should have 2 projects (parent + child)", 2, projects.size());
	}

	@Test
	public void testGetNestedChildProjects() {
		Collection<IProject> parents = java.util.Collections.singleton(parentProject);
		Collection<IProject> children = MarkerResourceUtil.getNestedChildProjects(parents);
		
		assertNotNull("Nested children collection should not be null", children);
		assertTrue("Should find nested child project", children.contains(childProject));
		assertEquals("Should have 1 nested child", 1, children.size());
	}

	@Test
	public void testMarkerFilterSelectsNestedProjectMarkers() throws CoreException {
		// Create a marker on the child project
		IMarker childMarker = childProject.createMarker(IMarker.PROBLEM);
		childMarker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		childMarker.setAttribute(IMarker.MESSAGE, "Test error in child project");

		// Create a filter configured for "errors/warnings on project"
		ProblemFilter filter = new ProblemFilter("Test Filter");
		filter.setOnResource(MarkerFilter.ON_ANY_IN_SAME_CONTAINER);
		filter.setFocusResource(new IResource[] { parentProject });
		filter.setEnabled(true);

		// Create a ConcreteMarker wrapper for the marker
		// Note: This is a simplified test - in a real scenario, ConcreteMarker
		// would be created by the marker view infrastructure
		org.eclipse.ui.views.markers.internal.ProblemMarker problemMarker = 
			new org.eclipse.ui.views.markers.internal.ProblemMarker(childMarker);

		// The filter should select the marker from the nested child project
		assertTrue("Filter should select marker from nested child project when parent is selected",
				filter.select(problemMarker));
	}

	@Test
	public void testMarkerFilterDoesNotSelectUnrelatedProjectMarkers() throws CoreException {
		// Create an unrelated project
		IProject unrelatedProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("UnrelatedProject");
		if (!unrelatedProject.exists()) {
			unrelatedProject.create(null);
		}
		unrelatedProject.open(null);

		try {
			// Create a marker on the unrelated project
			IMarker unrelatedMarker = unrelatedProject.createMarker(IMarker.PROBLEM);
			unrelatedMarker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			unrelatedMarker.setAttribute(IMarker.MESSAGE, "Test error in unrelated project");

			// Create a filter configured for "errors/warnings on project"
			ProblemFilter filter = new ProblemFilter("Test Filter");
			filter.setOnResource(MarkerFilter.ON_ANY_IN_SAME_CONTAINER);
			filter.setFocusResource(new IResource[] { parentProject });
			filter.setEnabled(true);

			// Create a ConcreteMarker wrapper for the marker
			org.eclipse.ui.views.markers.internal.ProblemMarker problemMarker = 
				new org.eclipse.ui.views.markers.internal.ProblemMarker(unrelatedMarker);

			// The filter should NOT select the marker from the unrelated project
			assertTrue("Filter should not select marker from unrelated project",
					!filter.select(problemMarker));
		} finally {
			if (unrelatedProject.exists()) {
				unrelatedProject.delete(true, true, null);
			}
		}
	}
}
