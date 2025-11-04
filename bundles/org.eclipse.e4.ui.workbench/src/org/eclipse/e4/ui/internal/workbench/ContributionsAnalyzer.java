/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
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
 *      Maxime Porhel <maxime.porhel@obeo.fr> Obeo - Bug 435949
 *      Lars Vogel <Lars.Vogel@vogella.com> - Bug 472654
 *      Simon Scholz <simon.scholz@vogella.com> - Bug 484398, 546815
 ******************************************************************************/

package org.eclipse.e4.ui.internal.workbench;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.ReferenceExpression;
import org.eclipse.e4.core.commands.ExpressionContext;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.InjectorFactory;
import org.eclipse.e4.core.di.annotations.Evaluate;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;
import org.eclipse.e4.core.internal.contexts.ContextObjectSupplier;
import org.eclipse.e4.core.internal.di.InjectorImpl;
import org.eclipse.e4.core.services.contributions.IContributionFactory;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.ui.MCoreExpression;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MExpression;
import org.eclipse.e4.ui.model.application.ui.MImperativeExpression;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuSeparator;
import org.eclipse.e4.ui.model.application.ui.menu.MPopupMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarSeparator;
import org.eclipse.e4.ui.model.application.ui.menu.MTrimContribution;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * Utility class for analyzing, gathering, merging, and processing UI contributions
 * in the Eclipse 4 workbench model.
 * <p>
 * This class provides functionality for:
 * <ul>
 * <li>Gathering menu, toolbar, and trim contributions based on parent IDs and visibility conditions</li>
 * <li>Evaluating visibility expressions (both core expressions and imperative expressions)</li>
 * <li>Merging multiple contributions into consolidated structures</li>
 * <li>Adding contributions to their target UI elements (menus, toolbars, trim bars)</li>
 * <li>Managing contribution positioning and ordering within parent containers</li>
 * </ul>
 * <p>
 * The class handles the Eclipse 4 contribution system which allows UI elements
 * to be dynamically added to menus, toolbars, and other UI containers through
 * declarative contributions that can have visibility conditions and specific
 * positioning requirements.
 *
 * @since 1.0
 */
public final class ContributionsAnalyzer {

	private static final Object missingEvaluate = new Object();

	/**
	 * Writes a trace message to the debug log if DEBUG is enabled.
	 *
	 * @param msg the message to trace
	 * @param error the error to trace, or {@code null} if no error
	 */
	public static void trace(String msg, Throwable error) {
		if (DEBUG) {
			Activator.trace(Policy.DEBUG_MENUS_FLAG, msg, error);
		}
	}

	private static boolean DEBUG = Policy.DEBUG_MENUS;

	private static void trace(String msg, Object menu, Object menuModel) {
		trace(msg + ": " + menu + ": " + menuModel, null); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Gathers trim contributions that should be applied to a trim bar with the specified element ID.
	 * <p>
	 * This method filters the provided list of trim contributions and adds those that:
	 * <ul>
	 * <li>Have a parent ID matching the given element ID</li>
	 * <li>Are marked as "to be rendered"</li>
	 * </ul>
	 *
	 * @param trimContributions the list of all available trim contributions to search through
	 * @param elementId the ID of the trim bar element to gather contributions for
	 * @param toContribute the output list where matching contributions will be added
	 */
	public static void gatherTrimContributions(List<MTrimContribution> trimContributions, String elementId,
			ArrayList<MTrimContribution> toContribute) {
		if (elementId == null || elementId.isEmpty()) {
			return;
		}
		for (MTrimContribution contribution : trimContributions) {
			String parentId = contribution.getParentId();
			if (!elementId.equals(parentId) || !contribution.isToBeRendered()) {
				continue;
			}
			toContribute.add(contribution);
		}
	}


	/**
	 * Gathers toolbar contributions that should be applied to a toolbar with the specified ID.
	 * <p>
	 * This method filters the provided list of toolbar contributions and adds those that:
	 * <ul>
	 * <li>Have a parent ID matching the given ID</li>
	 * <li>Are marked as "to be rendered"</li>
	 * </ul>
	 * <p>
	 * Note: This is a legacy method prefixed with "XXX". It does not evaluate visibility expressions.
	 * It is kept for backward compatibility with existing code.
	 *
	 * @param toolbarContributionList the list of all available toolbar contributions to search through
	 * @param id the ID of the toolbar to gather contributions for
	 * @param toContribute the output list where matching contributions will be added
	 */
	public static void XXXgatherToolBarContributions(final List<MToolBarContribution> toolbarContributionList,
			final String id,
			final ArrayList<MToolBarContribution> toContribute) {
		if (id == null || id.isEmpty()) {
			return;
		}
		for (MToolBarContribution toolBarContribution : toolbarContributionList) {
			String parentID = toolBarContribution.getParentId();
			if (!id.equals(parentID) || !toolBarContribution.isToBeRendered()) {
				continue;
			}
			toContribute.add(toolBarContribution);
		}
	}

	/**
	 * Gathers menu contributions that should be applied to a menu with the specified ID.
	 * <p>
	 * This method filters the provided list of menu contributions based on:
	 * <ul>
	 * <li>Parent ID matching</li>
	 * <li>Popup menu handling (if includePopups is true)</li>
	 * <li>Menu vs popup filtering based on tags</li>
	 * <li>Render visibility</li>
	 * </ul>
	 * <p>
	 * Special handling includes:
	 * <ul>
	 * <li>Support for popup menu IDs from the menu model's tags (e.g., "popup:*")</li>
	 * <li>Support for POPUP_PARENT_ID ("popup") for contributions to any popup menu</li>
	 * <li>Filtering based on MC_MENU and MC_POPUP tags to prevent cross-contamination</li>
	 * </ul>
	 * <p>
	 * Note: This is a legacy method prefixed with "XXX". It does not evaluate visibility expressions.
	 * Consider using {@link #gatherMenuContributions(MMenu, List, String, ArrayList, ExpressionContext, boolean)}
	 * for visibility-aware contribution gathering.
	 *
	 * @param menuModel the menu model to gather contributions for
	 * @param menuContributionList the list of all available menu contributions
	 * @param id the ID of the menu to gather contributions for
	 * @param toContribute the output list where matching contributions will be added
	 * @param includePopups whether to include popup-specific contributions
	 */
	public static void XXXgatherMenuContributions(final MMenu menuModel,
			final List<MMenuContribution> menuContributionList, final String id,
			final ArrayList<MMenuContribution> toContribute, boolean includePopups) {
		if (id == null || id.isEmpty()) {
			return;
		}
		ArrayList<String> popupIds = new ArrayList<>();
		if (includePopups) {
			popupIds.add(id);
			for (String tag : menuModel.getTags()) {
				if (tag.startsWith("popup:")) { //$NON-NLS-1$
					String tmp = tag.substring("popup:".length()); //$NON-NLS-1$
					if (!popupIds.contains(tmp)) {
						popupIds.add(tmp);
					}
				}
			}
		}
		ArrayList<MMenuContribution> includedPopups = new ArrayList<>();
		for (MMenuContribution menuContribution : menuContributionList) {
			String parentID = menuContribution.getParentId();
			if (parentID == null) {
				// it doesn't make sense for this to be null, temporary workaround for bug
				// 320790
				continue;
			}
			boolean popupTarget = includePopups && popupIds.contains(parentID);
			boolean popupAny = includePopups && menuModel instanceof MPopupMenu && POPUP_PARENT_ID.equals(parentID);
			boolean filtered = isFiltered(menuModel, menuContribution, includePopups);
			if (!filtered && menuContribution.isToBeRendered() && popupAny) {
				// process POPUP_ANY first
				toContribute.add(menuContribution);
			} else {
				if (filtered || (!popupTarget && !parentID.equals(id)) || !menuContribution.isToBeRendered()) {
					continue;
				}
				includedPopups.add(menuContribution);
			}
		}
		toContribute.addAll(includedPopups);
	}

	/**
	 * Gathers menu contributions that should be applied to a menu with the specified ID,
	 * with visibility evaluation using the provided expression context.
	 * <p>
	 * This is the preferred method for gathering menu contributions as it evaluates
	 * visibility expressions. Contributions are filtered based on:
	 * <ul>
	 * <li>Parent ID matching the specified id or POPUP_PARENT_ID for popup menus</li>
	 * <li>Tag-based filtering (MC_MENU vs MC_POPUP)</li>
	 * <li>Render visibility flag</li>
	 * <li>Visibility expression evaluation (for non-menubar menus)</li>
	 * </ul>
	 * <p>
	 * Note: Menu bar contributions skip visibility evaluation and are always included
	 * if they match the parent ID and are marked to be rendered.
	 *
	 * @param menuModel the menu model to gather contributions for
	 * @param menuContributionList the list of all available menu contributions
	 * @param id the ID of the menu to gather contributions for
	 * @param toContribute the output list where matching and visible contributions will be added
	 * @param eContext the expression context used to evaluate visibility expressions
	 * @param includePopups whether to include popup-specific contributions
	 */
	public static void gatherMenuContributions(final MMenu menuModel,
			final List<MMenuContribution> menuContributionList, final String id,
			final ArrayList<MMenuContribution> toContribute, final ExpressionContext eContext,
			boolean includePopups) {
		if (id == null || id.isEmpty()) {
			return;
		}
		boolean menuBar = (((MUIElement) ((EObject) menuModel).eContainer()) instanceof MWindow);
		for (MMenuContribution menuContribution : menuContributionList) {
			String parentID = menuContribution.getParentId();
			if (parentID == null) {
				// it doesn't make sense for this to be null, temporary workaround for bug 320790
				continue;
			}
			boolean popup = parentID.equals(POPUP_PARENT_ID) && (menuModel instanceof MPopupMenu)
					&& includePopups;
			boolean filtered = isFiltered(menuModel, menuContribution, includePopups);
			if (filtered || (!popup && !parentID.equals(id)) || !menuContribution.isToBeRendered()) {
				continue;
			}
			if (menuBar || isVisible(menuContribution, eContext)) {
				toContribute.add(menuContribution);
			}
		}
	}

	/**
	 * Determines if a menu contribution should be filtered out based on menu type and contribution tags.
	 * <p>
	 * The filtering logic is:
	 * <ul>
	 * <li>If the menu is a popup or includePopups is true: filter out contributions tagged with
	 * MC_MENU but not MC_POPUP</li>
	 * <li>If the menu is tagged with MC_MENU: filter out contributions tagged with MC_POPUP
	 * but not MC_MENU</li>
	 * <li>If includePopups is false: filter out all contributions tagged with MC_POPUP</li>
	 * </ul>
	 *
	 * @param menuModel the menu model being processed
	 * @param menuContribution the contribution to check for filtering
	 * @param includePopups whether popup contributions are being included
	 * @return {@code true} if the contribution should be filtered out, {@code false} otherwise
	 */
	static boolean isFiltered(MMenu menuModel, MMenuContribution menuContribution,
			boolean includePopups) {
		if (includePopups || menuModel.getTags().contains(ContributionsAnalyzer.MC_POPUP)) {
			return !menuContribution.getTags().contains(ContributionsAnalyzer.MC_POPUP)
					&& menuContribution.getTags().contains(ContributionsAnalyzer.MC_MENU);
		}
		if (menuModel.getTags().contains(ContributionsAnalyzer.MC_MENU)) {
			return !menuContribution.getTags().contains(ContributionsAnalyzer.MC_MENU)
					&& menuContribution.getTags().contains(ContributionsAnalyzer.MC_POPUP);
		}
		if (!includePopups) {
			// not including popups, so filter out popup menu contributions if the menu is a regular
			// menu
			return menuContribution.getTags().contains(ContributionsAnalyzer.MC_POPUP);
		}
		return false;
	}

	/**
	 * Collects information about variables accessed by a core expression.
	 * <p>
	 * This method extracts the expression from the MExpression model element and
	 * collects information about which variables the expression accesses. This
	 * information is used to track dependencies and for expression evaluation.
	 *
	 * @param info the ExpressionInfo object to populate with accessed variable information
	 * @param exp the expression to analyze
	 */
	public static void collectInfo(ExpressionInfo info, MExpression exp) {
		if (!(exp instanceof MCoreExpression expr)) {
			return;
		}
		Expression ref = null;
		if (expr.getCoreExpression() instanceof Expression) {
			ref = (Expression) expr.getCoreExpression();
		} else {
			ref = new ReferenceExpression(expr.getCoreExpressionId());
			expr.setCoreExpression(ref);
		}
		ref.collectExpressionInfo(info);
	}

	/**
	 * Evaluates whether a menu contribution is visible based on its visibleWhen expression.
	 *
	 * @param menuContribution the menu contribution to check
	 * @param eContext the expression context for evaluation
	 * @return {@code true} if the contribution should be visible, {@code false} otherwise
	 */
	public static boolean isVisible(MMenuContribution menuContribution, ExpressionContext eContext) {
		if (menuContribution.getVisibleWhen() == null) {
			return true;
		}
		return isVisible(menuContribution.getVisibleWhen(), eContext);
	}

	/**
	 * Evaluates whether a toolbar contribution is visible based on its visibleWhen expression.
	 *
	 * @param contribution the toolbar contribution to check
	 * @param eContext the expression context for evaluation
	 * @return {@code true} if the contribution should be visible, {@code false} otherwise
	 */
	public static boolean isVisible(MToolBarContribution contribution, ExpressionContext eContext) {
		if (contribution.getVisibleWhen() == null) {
			return true;
		}
		return isVisible(contribution.getVisibleWhen(), eContext);
	}

	/**
	 * Evaluates whether a trim contribution is visible based on its visibleWhen expression.
	 *
	 * @param contribution the trim contribution to check
	 * @param eContext the expression context for evaluation
	 * @return {@code true} if the contribution should be visible, {@code false} otherwise
	 */
	public static boolean isVisible(MTrimContribution contribution, ExpressionContext eContext) {
		if (contribution.getVisibleWhen() == null) {
			return true;
		}
		return isVisible(contribution.getVisibleWhen(), eContext);
	}

	/**
	 * Evaluates whether an expression evaluates to visible/true.
	 * <p>
	 * This method handles both core expressions (Eclipse expression framework) and
	 * imperative expressions (custom evaluation code).
	 *
	 * @param exp the expression to evaluate
	 * @param eContext the expression context for evaluation
	 * @return {@code true} if the expression evaluates to visible, {@code false} otherwise
	 */
	public static boolean isVisible(MExpression exp, final ExpressionContext eContext) {
		if (exp instanceof MCoreExpression coreExpression) {
			return isCoreExpressionVisible(coreExpression, eContext);
		} else if (exp instanceof MImperativeExpression) {
			return isImperativeExpressionVisible((MImperativeExpression) exp, eContext);
		}

		return true;
	}

	/**
	 * Evaluates a core expression in the provided context.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Extracts or creates the Eclipse expression from the model element</li>
	 * <li>Creates dependencies on evaluation service variables for reactive evaluation</li>
	 * <li>Evaluates the expression and returns whether it's visible (not FALSE)</li>
	 * </ul>
	 *
	 * @param coreExpression the core expression to evaluate
	 * @param eContext the expression context for evaluation
	 * @return {@code true} if the expression does not evaluate to FALSE, {@code false} otherwise
	 */
	private static boolean isCoreExpressionVisible(MCoreExpression coreExpression, final ExpressionContext eContext) {
		final Expression ref;
		if (coreExpression.getCoreExpression() instanceof Expression) {
			ref = (Expression) coreExpression.getCoreExpression();
		} else {
			ref = new ReferenceExpression(coreExpression.getCoreExpressionId());
			coreExpression.setCoreExpression(ref);
		}
		// Creates dependency on a predefined value that can be "poked" by
		// the evaluation
		// service
		ExpressionInfo info = ref.computeExpressionInfo();
		String[] names = info.getAccessedPropertyNames();
		for (String name : names) {
			eContext.getVariable(name + ".evaluationServiceLink"); //$NON-NLS-1$
		}
		boolean ret = false;
		try {
			ret = ref.evaluate(eContext) != EvaluationResult.FALSE;
		} catch (Exception e) {
			if (DEBUG) {
				trace("isVisible exception", e); //$NON-NLS-1$
			}
		}
		return ret;
	}

	/**
	 * Evaluates an imperative expression (custom Java code) in the provided context.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Creates the imperative expression object from its contribution URI if needed</li>
	 * <li>Invokes the @Evaluate annotated method on the expression object</li>
	 * <li>Handles tracking vs non-tracking evaluation modes</li>
	 * </ul>
	 *
	 * @param exp the imperative expression to evaluate
	 * @param eContext the expression context for evaluation
	 * @return {@code true} if the expression evaluates to true, {@code false} otherwise
	 * @throws IllegalStateException if the expression object has no @Evaluate annotated method
	 */
	private static boolean isImperativeExpressionVisible(MImperativeExpression exp, final ExpressionContext eContext) {
		Object imperativeExpressionObject = exp.getObject();
		if (imperativeExpressionObject == null) {
			IContributionFactory contributionFactory = eContext.eclipseContext.get(IContributionFactory.class);
			Object newImperativeExpression = contributionFactory.create(exp.getContributionURI(),
					eContext.eclipseContext);
			exp.setObject(newImperativeExpression);
			imperativeExpressionObject = newImperativeExpression;
		}

		if (imperativeExpressionObject == null) {
			return false;
		}

		Object result = null;

		IEclipseContext staticContext = EclipseContextFactory.create("Evaluation-Static");//$NON-NLS-1$
		staticContext.set(MImperativeExpression.class, exp);
		try {
			if (exp.isTracking()) {
				result = invoke(imperativeExpressionObject, Evaluate.class, eContext.eclipseContext, staticContext,
						missingEvaluate);
			} else {
				result = ContextInjectionFactory.invoke(imperativeExpressionObject, Evaluate.class,
						eContext.eclipseContext, staticContext, missingEvaluate);
			}
		} finally {
			staticContext.dispose();
		}

		if (result == missingEvaluate) {
			String className="null";//$NON-NLS-1$
			if (imperativeExpressionObject != null) {
				className = imperativeExpressionObject.getClass().getName();
			}
			throw new IllegalStateException(
					"There is no method annotated with @Evaluate in the " + className + " imperative expression class " //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
		return (boolean) result;
	}

	final private static InjectorImpl injector = (InjectorImpl) InjectorFactory.getDefault();

	/**
	 * Invokes a method with the specified qualifier annotation on an object using dependency injection.
	 * <p>
	 * This is a helper method for invoking annotated methods (like @Evaluate) with proper
	 * context injection.
	 *
	 * @param object the object on which to invoke the method
	 * @param qualifier the annotation class qualifying the method to invoke
	 * @param context the primary Eclipse context for injection
	 * @param localContext the local/temporary context for injection
	 * @param defaultValue the value to return if no matching method is found
	 * @return the result of the method invocation
	 * @throws InjectionException if injection or invocation fails
	 */
	static private Object invoke(Object object, Class<? extends Annotation> qualifier, IEclipseContext context,
			IEclipseContext localContext, Object defaultValue) throws InjectionException {
		PrimaryObjectSupplier supplier = ContextObjectSupplier.getObjectSupplier(context, injector);
		PrimaryObjectSupplier tempSupplier = ContextObjectSupplier.getObjectSupplier(localContext, injector);
		return injector.invoke(object, qualifier, defaultValue, supplier, tempSupplier, false, true);
	}

	/**
	 * Adds menu contributions to a menu model, processing them in the order specified
	 * by their position requirements.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Tracks existing menu and separator IDs to avoid duplicates</li>
	 * <li>Processes contributions in multiple passes if positioning dependencies exist</li>
	 * <li>Adds contributed menu elements at the appropriate positions</li>
	 * <li>Records added elements for later removal if needed</li>
	 * </ul>
	 *
	 * @param menuModel the menu to add contributions to
	 * @param toContribute the list of contributions to add
	 * @param menuContributionsToRemove output list to track added elements for later removal
	 */
	public static void addMenuContributions(final MMenu menuModel,
			final ArrayList<MMenuContribution> toContribute,
			final ArrayList<MMenuElement> menuContributionsToRemove) {

		HashSet<String> existingMenuIds = new HashSet<>();
		HashSet<String> existingSeparatorNames = new HashSet<>();
		for (MMenuElement child : menuModel.getChildren()) {
			String elementId = child.getElementId();
			if (child instanceof MMenu && elementId != null) {
				existingMenuIds.add(elementId);
			} else if (child instanceof MMenuSeparator && elementId != null) {
				existingSeparatorNames.add(elementId);
			}
		}

		boolean done = toContribute.isEmpty();
		while (!done) {
			ArrayList<MMenuContribution> curList = new ArrayList<>(toContribute);
			int retryCount = toContribute.size();
			toContribute.clear();

			for (MMenuContribution menuContribution : curList) {
				if (!processAddition(menuModel, menuContributionsToRemove, menuContribution,
						existingMenuIds, existingSeparatorNames)) {
					toContribute.add(menuContribution);
				}
			}
			// We're done if the retryList is now empty (everything done) or
			// if the list hasn't changed at all (no hope)
			done = (toContribute.isEmpty()) || (toContribute.size() == retryCount);
		}
	}

	/**
	 * Processes a single menu contribution, adding its children to the menu model.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Determines the insertion index based on the contribution's position specification</li>
	 * <li>Skips duplicate menus and separators (based on element IDs)</li>
	 * <li>Creates copies of the contribution's children and inserts them</li>
	 * <li>Updates tracking sets for duplicate detection</li>
	 * </ul>
	 *
	 * @param menuModel the menu to add contributions to
	 * @param menuContributionsToRemove list to track added elements for later removal
	 * @param menuContribution the contribution to process
	 * @param existingMenuIds set of existing menu IDs for duplicate detection
	 * @param existingSeparatorNames set of existing separator IDs for duplicate detection
	 * @return {@code true} if the contribution was successfully processed, {@code false} if
	 *         the position reference was not found
	 */
	public static boolean processAddition(final MMenu menuModel,
			final ArrayList<MMenuElement> menuContributionsToRemove,
			MMenuContribution menuContribution, final HashSet<String> existingMenuIds,
			HashSet<String> existingSeparatorNames) {
		int idx = getIndex(menuModel, menuContribution.getPositionInParent());
		if (idx == -1) {
			return false;
		}
		for (MMenuElement item : menuContribution.getChildren()) {
			if (item instanceof MMenu && existingMenuIds.contains(item.getElementId())) {
				// skip this, it's already there
				continue;
			} else if (item instanceof MMenuSeparator
					&& existingSeparatorNames.contains(item.getElementId())) {
				// skip this, it's already there
				continue;
			}
			MMenuElement copy = (MMenuElement) EcoreUtil.copy((EObject) item);
			if (DEBUG) {
				trace("addMenuContribution " + copy, menuModel.getWidget(), menuModel); //$NON-NLS-1$
			}
			menuContributionsToRemove.add(copy);
			menuModel.getChildren().add(idx++, copy);
			if (copy instanceof MMenu && copy.getElementId() != null) {
				existingMenuIds.add(copy.getElementId());
			} else if (copy instanceof MMenuSeparator && copy.getElementId() != null) {
				existingSeparatorNames.add(copy.getElementId());
			}
		}
		return true;
	}

	/**
	 * Processes a single toolbar contribution, adding its children to the toolbar model.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Determines the insertion index based on the contribution's position specification</li>
	 * <li>Skips duplicate separators (based on element IDs)</li>
	 * <li>Creates copies of the contribution's children and inserts them</li>
	 * <li>Updates tracking set for duplicate separator detection</li>
	 * </ul>
	 *
	 * @param toolBarModel the toolbar to add contributions to
	 * @param toolBarContribution the contribution to process
	 * @param contributions list to track added elements for later removal
	 * @param existingSeparatorNames set of existing separator IDs for duplicate detection
	 * @return {@code true} if the contribution was successfully processed, {@code false} if
	 *         the position reference was not found
	 */
	public static boolean processAddition(final MToolBar toolBarModel,
			MToolBarContribution toolBarContribution, List<MToolBarElement> contributions,
			HashSet<String> existingSeparatorNames) {
		int idx = getIndex(toolBarModel, toolBarContribution.getPositionInParent());
		if (idx == -1) {
			return false;
		}
		for (MToolBarElement item : toolBarContribution.getChildren()) {
			if (item instanceof MToolBarSeparator
					&& existingSeparatorNames.contains(item.getElementId())) {
				// skip this, it's already there
				continue;
			}
			MToolBarElement copy = (MToolBarElement) EcoreUtil.copy((EObject) item);
			if (DEBUG) {
				trace("addToolBarContribution " + copy, toolBarModel.getWidget(), toolBarModel); //$NON-NLS-1$
			}
			toolBarModel.getChildren().add(idx++, copy);
			contributions.add(copy);
			if (copy instanceof MToolBarSeparator && copy.getElementId() != null) {
				existingSeparatorNames.add(copy.getElementId());
			}
		}
		return true;
	}

	/**
	 * Processes a single trim contribution, adding its children to the trim bar.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Determines the insertion index based on the contribution's position specification</li>
	 * <li>Skips duplicate toolbars (based on element IDs)</li>
	 * <li>Creates copies of the contribution's children and inserts them</li>
	 * <li>Marks contributed items as non-persistent</li>
	 * <li>Updates tracking set for duplicate toolbar detection</li>
	 * </ul>
	 *
	 * @param trimBar the trim bar to add contributions to
	 * @param contribution the contribution to process
	 * @param contributions list to track added elements for later removal
	 * @param existingToolbarIds set of existing toolbar IDs for duplicate detection
	 * @return {@code true} if the contribution was successfully processed, {@code false} if
	 *         the position reference was not found
	 */
	public static boolean processAddition(final MTrimBar trimBar, MTrimContribution contribution,
			List<MTrimElement> contributions, HashSet<String> existingToolbarIds) {
		int idx = getIndex(trimBar, contribution.getPositionInParent());
		if (idx == -1) {
			return false;
		}
		for (MTrimElement item : contribution.getChildren()) {
			if (item instanceof MToolBar && existingToolbarIds.contains(item.getElementId())) {
				// skip this, it's already there
				continue;
			}
			MTrimElement copy = (MTrimElement) EcoreUtil.copy((EObject) item);
			copy.getPersistedState().put(IWorkbench.PERSIST_STATE, Boolean.FALSE.toString());
			if (DEBUG) {
				trace("addTrimContribution " + copy, trimBar.getWidget(), trimBar); //$NON-NLS-1$
			}
			trimBar.getChildren().add(idx++, copy);
			contributions.add(copy);
			if (copy instanceof MToolBar && copy.getElementId() != null) {
				existingToolbarIds.add(copy.getElementId());
			}
		}
		return true;
	}

	/**
	 * Calculates the insertion index for a contribution based on its position specification.
	 * <p>
	 * Position format: "modifier=elementId" where:
	 * <ul>
	 * <li>modifier is "before" or "after" (default is before)</li>
	 * <li>elementId is the ID of the reference element</li>
	 * <li>Special case: "additions" always returns the end of the list</li>
	 * </ul>
	 *
	 * @param menuModel the container to find the position in
	 * @param positionInParent the position specification string
	 * @return the insertion index, or -1 if the reference element was not found (except for "additions")
	 */
	private static int getIndex(MElementContainer<?> menuModel, String positionInParent) {
		String id = null;
		String modifier = null;
		if (positionInParent != null && positionInParent.length() > 0) {
			String[] array = positionInParent.split("="); //$NON-NLS-1$
			modifier = array[0];
			// may have an invalid position, check for this
			if (array.length > 1) {
				id = array[1];
			}
		}
		if (id == null) {
			return menuModel.getChildren().size();
		}

		int idx = 0;
		int size = menuModel.getChildren().size();
		while (idx < size) {
			if (id.equals(menuModel.getChildren().get(idx).getElementId())) {
				if ("after".equals(modifier)) { //$NON-NLS-1$
					idx++;
				}
				return idx;
			}
			idx++;
		}
		return id.equals("additions") ? menuModel.getChildren().size() : -1; //$NON-NLS-1$
	}

	/**
	 * Retrieves a command by its ID from the application model.
	 *
	 * @param app the application model
	 * @param cmdId the command ID to look up
	 * @return the command with the specified ID, or {@code null} if not found
	 */
	public static MCommand getCommandById(MApplication app, String cmdId) {
		return app.getCommand(cmdId);
	}

	/**
	 * Base class for contribution keys used in merging contributions.
	 * <p>
	 * A key identifies a unique contribution based on:
	 * <ul>
	 * <li>Parent ID - where the contribution is going</li>
	 * <li>Position - where in the parent it should be placed</li>
	 * <li>Scheme tag - the type of contribution (menu, popup, toolbar)</li>
	 * <li>Visibility expression - when it should be visible</li>
	 * <li>Factory - the contribution factory that created it</li>
	 * </ul>
	 * Contributions with the same key can be merged together.
	 */
	static class Key {
		private int tag = -1;
		private int hc = -1;
		private final String parentId;
		private final String position;
		private final MCoreExpression vexp;
		private final Object factory;

		public Key(String parentId, String position, List<String> tags, MCoreExpression vexp,
				Object factory) {
			this.parentId = parentId;
			this.position = position;
			this.vexp = vexp;
			this.factory = factory;
			if (tags.contains("scheme:menu")) { //$NON-NLS-1$
				tag = 1;
			} else if (tags.contains("scheme:popup")) { //$NON-NLS-1$
				tag = 2;
			} else if (tags.contains("scheme:toolbar")) { //$NON-NLS-1$
				tag = 3;
			} else {
				tag = 0;
			}
		}

		int getSchemeTag() {
			return tag;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Key other)) {
				return false;
			}
			Object exp1 = vexp == null ? null : vexp.getCoreExpression();
			Object exp2 = other.vexp == null ? null : other.vexp.getCoreExpression();
			return Objects.equals(parentId, other.parentId) && Objects.equals(position, other.position)
					&& getSchemeTag() == other.getSchemeTag() && Objects.equals(exp1, exp2)
					&& Objects.equals(factory, other.factory);
		}

		@Override
		public int hashCode() {
			if (hc == -1) {
				Object exp1 = vexp == null ? null : vexp.getCoreExpression();
				hc = Objects.hashCode(parentId);
				hc = hc * 87 + Objects.hashCode(position);
				hc = hc * 87 + getSchemeTag();
				hc = hc * 87 + Objects.hashCode(exp1);
				hc = hc * 87 + Objects.hashCode(factory);
			}
			return hc;
		}

		@Override
		public String toString() {
			return getClass().getName() + " " + parentId + "--" + position //$NON-NLS-1$ //$NON-NLS-2$
					+ "--" + getSchemeTag() + "--" + vexp; //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	/**
	 * Key implementation for menu contributions.
	 * <p>
	 * Associates a MenuKey with its corresponding MMenuContribution and stores
	 * the key in the contribution's widget field for efficient retrieval.
	 */
	static class MenuKey extends Key {
		static final String FACTORY = "ContributionFactory"; //$NON-NLS-1$
		private final MMenuContribution contribution;

		public MenuKey(MMenuContribution mc) {
			super(mc.getParentId(), mc.getPositionInParent(), mc.getTags(), (MCoreExpression) mc
					.getVisibleWhen(), mc.getTransientData().get(FACTORY));
			this.contribution = mc;
			mc.setWidget(this);
		}

		public MMenuContribution getContribution() {
			return contribution;
		}
	}

	/**
	 * Key implementation for toolbar contributions.
	 * <p>
	 * Associates a ToolBarKey with its corresponding MToolBarContribution and stores
	 * the key in the contribution's widget field for efficient retrieval.
	 */
	static class ToolBarKey extends Key {
		static final String FACTORY = "ToolBarContributionFactory"; //$NON-NLS-1$
		private final MToolBarContribution contribution;

		public ToolBarKey(MToolBarContribution mc) {
			super(mc.getParentId(), mc.getPositionInParent(), mc.getTags(), (MCoreExpression) mc
					.getVisibleWhen(), mc.getTransientData().get(FACTORY));
			this.contribution = mc;
			mc.setWidget(this);
		}

		public MToolBarContribution getContribution() {
			return contribution;
		}
	}

	/**
	 * Key implementation for trim contributions.
	 * <p>
	 * Associates a TrimKey with its corresponding MTrimContribution and stores
	 * the key in the contribution's widget field for efficient retrieval.
	 */
	static class TrimKey extends Key {
		private final MTrimContribution contribution;

		public TrimKey(MTrimContribution mc) {
			super(mc.getParentId(), mc.getPositionInParent(), mc.getTags(), (MCoreExpression) mc
					.getVisibleWhen(), null);
			this.contribution = mc;
			mc.setWidget(this);
		}

		public MTrimContribution getContribution() {
			return contribution;
		}
	}

	private static MenuKey getKey(MMenuContribution contribution) {
		if (contribution.getWidget() instanceof MenuKey) {
			return (MenuKey) contribution.getWidget();
		}
		return new MenuKey(contribution);
	}

	private static ToolBarKey getKey(MToolBarContribution contribution) {
		if (contribution.getWidget() instanceof ToolBarKey) {
			return (ToolBarKey) contribution.getWidget();
		}
		return new ToolBarKey(contribution);
	}

	private static TrimKey getKey(MTrimContribution contribution) {
		if (contribution.getWidget() instanceof TrimKey) {
			return (TrimKey) contribution.getWidget();
		}
		return new TrimKey(contribution);
	}

	/**
	 * Debugging utility to print menu contributions and their children.
	 *
	 * @param contributions the list of contributions to print
	 */
	public static void printContributions(ArrayList<MMenuContribution> contributions) {
		if (!DEBUG) {
			return;
		}
		for (MMenuContribution c : contributions) {
			trace("\n" + c, null); //$NON-NLS-1$
			for (MMenuElement element : c.getChildren()) {
				printElement(1, element);
			}
		}
	}

	private static void printElement(int level, MMenuElement element) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < level; i++) {
			buf.append('\t');
		}
		buf.append(element.toString());
		trace(buf.toString(), null);
		if (element instanceof MMenu) {
			for (MMenuElement item : ((MMenu) element).getChildren()) {
				printElement(level + 1, item);
			}
		}
	}

	/**
	 * Merges toolbar contributions that have the same key (parent, position, visibility, etc.).
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Groups contributions by their ToolBarKey</li>
	 * <li>Combines children from contributions with matching keys</li>
	 * <li>Removes duplicates (same element ID and type for separators/toolbars)</li>
	 * <li>Produces a consolidated list of unique contributions</li>
	 * </ul>
	 *
	 * @param contributions the list of toolbar contributions to merge
	 * @param result the output list for merged contributions
	 */
	public static void mergeToolBarContributions(ArrayList<MToolBarContribution> contributions,
			ArrayList<MToolBarContribution> result) {
		HashMap<ToolBarKey, ArrayList<MToolBarContribution>> buckets = new HashMap<>();
		if (DEBUG) {
			trace("mergeContributions size: " + contributions.size(), null); //$NON-NLS-1$
		}
		// first pass, sort by parentId?position,scheme,visibleWhen
		for (MToolBarContribution contribution : contributions) {
			ToolBarKey key = getKey(contribution);
			ArrayList<MToolBarContribution> slot = buckets.get(key);
			if (slot == null) {
				slot = new ArrayList<>();
				buckets.put(key, slot);
			}
			slot.add(contribution);
		}
		Iterator<MToolBarContribution> i = contributions.iterator();
		while (i.hasNext() && !buckets.isEmpty()) {
			MToolBarContribution contribution = i.next();
			ToolBarKey key = getKey(contribution);
			ArrayList<MToolBarContribution> slot = buckets.remove(key);
			if (slot == null) {
				continue;
			}
			MToolBarContribution toContribute = null;
			for (MToolBarContribution item : slot) {
				if (toContribute == null) {
					toContribute = item;
					continue;
				}
				Object[] array = item.getChildren().toArray();
				for (Object element : array) {
					MToolBarElement me = (MToolBarElement) element;
					if (!containsMatching(toContribute.getChildren(), me)) {
						toContribute.getChildren().add(me);
					}
				}
			}
			if (toContribute != null) {
				toContribute.setWidget(null);
				result.add(toContribute);
			}
		}
		if (DEBUG) {
			trace("mergeContributions: final size: " + result.size(), null); //$NON-NLS-1$
		}
	}

	/**
	 * Merges menu contributions that have the same key (parent, position, visibility, etc.).
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Groups contributions by their MenuKey</li>
	 * <li>Combines children from contributions with matching keys</li>
	 * <li>Respects positioning requirements when merging</li>
	 * <li>Removes duplicates (same element ID and type for separators/menus)</li>
	 * <li>Produces a consolidated list of unique contributions</li>
	 * </ul>
	 *
	 * @param contributions the list of menu contributions to merge
	 * @param result the output list for merged contributions
	 */
	public static void mergeContributions(ArrayList<MMenuContribution> contributions,
			ArrayList<MMenuContribution> result) {
		HashMap<MenuKey, ArrayList<MMenuContribution>> buckets = new HashMap<>();
		if (DEBUG) {
			trace("mergeContributions size: " + contributions.size(), null); //$NON-NLS-1$
			printContributions(contributions);
		}
		// first pass, sort by parentId?position,scheme,visibleWhen
		for (MMenuContribution contribution : contributions) {
			MenuKey key = getKey(contribution);
			ArrayList<MMenuContribution> slot = buckets.get(key);
			if (slot == null) {
				slot = new ArrayList<>();
				buckets.put(key, slot);
			}
			slot.add(contribution);
		}
		Iterator<MMenuContribution> i = contributions.iterator();
		while (i.hasNext() && !buckets.isEmpty()) {
			MMenuContribution contribution = i.next();
			MenuKey key = getKey(contribution);
			ArrayList<MMenuContribution> slot = buckets.remove(key);
			if (slot == null) {
				continue;
			}
			MMenuContribution toContribute = null;
			for (MMenuContribution item : slot) {
				if (toContribute == null) {
					toContribute = item;
					continue;
				}
				Object[] array = item.getChildren().toArray();
				int idx = getIndex(toContribute, item.getPositionInParent());
				if (idx == -1) {
					idx = 0;
				}
				for (Object element : array) {
					MMenuElement me = (MMenuElement) element;
					if (!containsMatching(toContribute.getChildren(), me)) {
						toContribute.getChildren().add(idx, me);
						idx++;
					}
				}
			}
			if (toContribute != null) {
				toContribute.setWidget(null);
				result.add(toContribute);
			}
		}
		trace("mergeContributions: final size: " + result.size(), null); //$NON-NLS-1$
	}

	/**
	 * Checks if a list of menu elements contains a matching element.
	 * <p>
	 * Two elements match if they have the same element ID, compatible types,
	 * and are either separators or menus (items are not checked for matches).
	 *
	 * @param children the list to search
	 * @param me the element to find a match for
	 * @return {@code true} if a matching element is found, {@code false} otherwise
	 */
	private static boolean containsMatching(List<MMenuElement> children, MMenuElement me) {
		for (MMenuElement element : children) {
			if (Objects.equals(me.getElementId(), element.getElementId())
					&& element.getClass().isInstance(me)
					&& (element instanceof MMenuSeparator || element instanceof MMenu)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if a list of toolbar elements contains a matching element.
	 * <p>
	 * Two elements match if they have the same element ID, compatible types,
	 * and are either separators or toolbars.
	 *
	 * @param children the list to search
	 * @param me the element to find a match for
	 * @return {@code true} if a matching element is found, {@code false} otherwise
	 */
	private static boolean containsMatching(List<MToolBarElement> children, MToolBarElement me) {
		for (MToolBarElement element : children) {
			if (Objects.equals(me.getElementId(), element.getElementId())
					&& element.getClass().isInstance(me)
					&& (element instanceof MToolBarSeparator || element instanceof MToolBar)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if a list of trim elements contains a matching element.
	 * <p>
	 * Two elements match if they have the same element ID, compatible types,
	 * and are either separators or toolbars.
	 *
	 * @param children the list to search
	 * @param me the element to find a match for
	 * @return {@code true} if a matching element is found, {@code false} otherwise
	 */
	private static boolean containsMatching(List<MTrimElement> children, MTrimElement me) {
		for (MTrimElement element : children) {
			if (Objects.equals(me.getElementId(), element.getElementId())
					&& element.getClass().isInstance(me)
					&& (element instanceof MToolBarSeparator || element instanceof MToolBar)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds the index of a menu element with the specified ID in a parent menu.
	 *
	 * @param parentMenu the parent menu container to search
	 * @param id the element ID to find
	 * @return the index of the element, or -1 if not found or if id is null/empty
	 */
	public static int indexForId(MElementContainer<MMenuElement> parentMenu, String id) {
		if (id == null || id.isEmpty()) {
			return -1;
		}
		int i = 0;
		for (MMenuElement item : parentMenu.getChildren()) {
			if (id.equals(item.getElementId())) {
				return i;
			}
			i++;
		}
		return -1;
	}

	/** Tag constant for popup menu contributions */
	public static final String MC_POPUP = "menuContribution:popup"; //$NON-NLS-1$
	/** Tag constant for menu bar menu contributions */
	public static final String MC_MENU = "menuContribution:menu"; //$NON-NLS-1$
	/** Tag constant for toolbar contributions */
	public static final String MC_TOOLBAR = "menuContribution:toolbar"; //$NON-NLS-1$
	/** Parent ID for contributions that apply to any popup menu */
	public static final String POPUP_PARENT_ID = "popup"; //$NON-NLS-1$

	/**
	 * Merges trim contributions that have the same key (parent, position, visibility, etc.).
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Groups contributions by their TrimKey</li>
	 * <li>Combines children from contributions with matching keys</li>
	 * <li>Removes duplicates (same element ID and type for toolbars)</li>
	 * <li>Produces a consolidated list of unique contributions</li>
	 * </ul>
	 *
	 * @param contributions the list of trim contributions to merge
	 * @param result the output list for merged contributions
	 */
	public static void mergeTrimContributions(ArrayList<MTrimContribution> contributions,
			ArrayList<MTrimContribution> result) {
		HashMap<TrimKey, ArrayList<MTrimContribution>> buckets = new HashMap<>();
		if (DEBUG) {
			trace("mergeContributions size: " + contributions.size(), null); //$NON-NLS-1$
		}
		// first pass, sort by parentId?position,scheme,visibleWhen
		for (MTrimContribution contribution : contributions) {
			TrimKey key = getKey(contribution);
			ArrayList<MTrimContribution> slot = buckets.get(key);
			if (slot == null) {
				slot = new ArrayList<>();
				buckets.put(key, slot);
			}
			slot.add(contribution);
		}
		Iterator<MTrimContribution> i = contributions.iterator();
		while (i.hasNext() && !buckets.isEmpty()) {
			MTrimContribution contribution = i.next();
			TrimKey key = getKey(contribution);
			ArrayList<MTrimContribution> slot = buckets.remove(key);
			if (slot == null) {
				continue;
			}
			MTrimContribution toContribute = null;
			for (MTrimContribution item : slot) {
				if (toContribute == null) {
					toContribute = item;
					continue;
				}
				Object[] array = item.getChildren().toArray();
				for (Object element : array) {
					MTrimElement me = (MTrimElement) element;
					if (!containsMatching(toContribute.getChildren(), me)) {
						toContribute.getChildren().add(me);
					}
				}
			}
			if (toContribute != null) {
				toContribute.setWidget(null);
				result.add(toContribute);
			}
		}
		if (DEBUG) {
			trace("mergeContributions: final size: " + result.size(), null); //$NON-NLS-1$
		}
	}

	/**
	 * Populates an Eclipse context with all the interfaces implemented by a model object.
	 * <p>
	 * This method recursively adds the model object to the context under the name of each
	 * interface it implements, making it available for dependency injection lookups by
	 * interface type.
	 *
	 * @param modelObject the model object to populate into the context
	 * @param context the Eclipse context to populate
	 * @param interfaces the array of interfaces to register
	 */
	public static void populateModelInterfaces(Object modelObject, IEclipseContext context,
			Class<?>[] interfaces) {
		for (Class<?> intf : interfaces) {
			if (Policy.DEBUG_CONTEXTS) {
				Activator.trace(Policy.DEBUG_CONTEXTS_FLAG, "Adding " + intf.getName() + " for " //$NON-NLS-1$ //$NON-NLS-2$
						+ modelObject.getClass().getName(), null);
			}
			context.set(intf.getName(), modelObject);

			populateModelInterfaces(modelObject, context, intf.getInterfaces());
		}
	}
}
