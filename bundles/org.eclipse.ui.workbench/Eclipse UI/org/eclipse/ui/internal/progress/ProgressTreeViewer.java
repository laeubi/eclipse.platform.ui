/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.progress;
import java.util.Iterator;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.*;
/**
 * The ProgressTreeViewer is a tree viewer that handles the coloring of text.
 */
class ProgressTreeViewer extends TreeViewer {
	/*
	 * (non-Javadoc)
	 * 
	 * @param item
	 * 			An item in the tree.
	 * 
	 * @param element
	 * 			The item's information.
	 */
	protected void doUpdateItem(Item item, Object element) {
		super.doUpdateItem(item, element);
		if (element instanceof JobTreeElement) {
			if (item != null && item instanceof TreeItem) {
				TreeItem treeItem = (TreeItem) item;
				updateColors(treeItem, (JobTreeElement) element);
				if (treeItem.getItemCount() > 0)
					treeItem.setExpanded(true);
			}
		}
	}
	/**
	 * This method updates the colors for the treeItem.
	 * 
	 * @param treeItem
	 * 			The tree item to be updated.
	 * 
	 * @param info
	 * 			The information for that tree item.
	 */
	protected void updateColors(TreeItem treeItem, JobTreeElement element) {
		if (element.isJobInfo()) {
			JobInfo info = (JobInfo) element;
			if (info.getJob().getState() != Job.RUNNING) {
				setNotRunningColor(treeItem);
				return;
			}
		}
		treeItem.setForeground(treeItem.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
	}
	/**
	 * Set the color of the treeItem to be the color for not running.
	 * @param treeItem
	 */
	protected void setNotRunningColor(TreeItem treeItem) {
		treeItem.setForeground(JFaceColors.getActiveHyperlinkText(treeItem.getDisplay()));
	}
	/**
	 * Create a new instance of the receiver with the supplied parent and
	 * style.
	 * 
	 * @param parent
	 * 			The parent Composite.
	 * 
	 * @param style
	 * 			The style the SWT style bits used to create the tree.
	 * 			
	 */
	public ProgressTreeViewer(Composite parent, int style) {
		super(parent, style);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#createChildren(org.eclipse.swt.widgets.Widget)
	 */
	protected void createChildren(Widget widget) {
		super.createChildren(widget);
		getTree().addKeyListener(new KeyAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.KeyAdapter#keyPressed(org.eclipse.swt.events.KeyEvent)
			 */
			public void keyPressed(KeyEvent e) {
				//Bind escape to cancel
				if (e.keyCode == SWT.DEL) {
					cancelSelection();
				}
			}
		});
	}
	/**
	 * Cancels the selected elements.
	 */
	public void cancelSelection() {
		ISelection selection = getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			Iterator elements = structured.iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				((JobTreeElement) next).cancel();
			}
		}
	}
}
