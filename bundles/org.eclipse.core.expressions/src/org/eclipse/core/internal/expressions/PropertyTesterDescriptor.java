/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.expressions;

import org.osgi.framework.Bundle;

import org.eclipse.core.expressions.IPropertyTester;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

public class PropertyTesterDescriptor implements IPropertyTester {

	private IConfigurationElement fConfigElement;
	private String fNamespace;
	private String fProperties;

	private static final String PROPERTIES= "properties"; //$NON-NLS-1$
	private static final String NAMESPACE= "namespace"; //$NON-NLS-1$
	private static final String CLASS= "class";  //$NON-NLS-1$

	public PropertyTesterDescriptor(IConfigurationElement element) throws CoreException {
		fConfigElement= element;
		fNamespace= fConfigElement.getAttribute(NAMESPACE);
		if (fNamespace == null) {
			throw new CoreException(new Status(IStatus.ERROR, ExpressionPlugin.getPluginId(),
				IStatus.ERROR,
				ExpressionMessages.PropertyTesterDescriptor_no_namespace,
				null));
		}
		StringBuffer buffer= new StringBuffer(","); //$NON-NLS-1$
		String properties= element.getAttribute(PROPERTIES);
		if (properties == null) {
			throw new CoreException(new Status(IStatus.ERROR, ExpressionPlugin.getPluginId(),
				IStatus.ERROR,
				ExpressionMessages.PropertyTesterDescritpri_no_properties,
				null));
		}
		for (int i= 0; i < properties.length(); i++) {
			char ch= properties.charAt(i);
			if (!Character.isWhitespace(ch))
				buffer.append(ch);
		}
		buffer.append(',');
		fProperties= buffer.toString();
	}

	public PropertyTesterDescriptor(IConfigurationElement element, String namespace, String properties) {
		fConfigElement= element;
		fNamespace= namespace;
		fProperties= properties;
	}

	public String getProperties() {
		return fProperties;
	}

	public String getNamespace() {
		return fNamespace;
	}

	public IConfigurationElement getConfigurationElement() {
		return fConfigElement;
	}

	public boolean handles(String namespace, String property) {
		return fNamespace.equals(namespace) && fProperties.indexOf("," + property + ",") != -1;  //$NON-NLS-1$//$NON-NLS-2$
	}

	public boolean isInstantiated() {
		return false;
	}

	public boolean isDeclaringPluginActive() {
		Bundle fBundle= Platform.getBundle(fConfigElement.getContributor().getName());
		return fBundle.getState() == Bundle.ACTIVE;
	}

	public IPropertyTester instantiate() throws CoreException {
		return (IPropertyTester)fConfigElement.createExecutableExtension(CLASS);
	}

	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		Assert.isTrue(false, "Method should never be called"); //$NON-NLS-1$
		return false;
	}
}
