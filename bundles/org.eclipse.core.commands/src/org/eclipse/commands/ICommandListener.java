/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.commands;

/**
 * An instance of this interface can be used by clients to receive notification
 * of changes to one or more instances of <code>ICommand</code>.
 * <p>
 * This interface may be implemented by clients.
 * </p>
 * 
 * @since 3.1
 * @see Command#addCommandListener(ICommandListener)
 * @see Command#removeCommandListener(ICommandListener)
 */
public interface ICommandListener {

    /**
     * Notifies that one or more properties of an instance of
     * <code>ICommand</code> have changed. Specific details are described in
     * the <code>CommandEvent</code>.
     * 
     * @param commandEvent
     *            the command event. Guaranteed not to be <code>null</code>.
     */
    void commandChanged(CommandEvent commandEvent);
}