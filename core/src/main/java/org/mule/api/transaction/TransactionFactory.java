/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.transaction;

import org.mule.api.MuleContext;


/**
 * <code>TransactionFactory</code> creates a transaction.
 *
 */
public interface TransactionFactory
{
    /**
     * Create and begins a new transaction
     * 
     * @return a new Transaction
     * @throws TransactionException if the transaction cannot be created or begun
     * @param muleContext
     */
    Transaction beginTransaction(MuleContext muleContext) throws TransactionException;

    /** Join an existing external transaction, if one exists; if not, return null.
     * For most factory types, this always return null.
     */
    Transaction joinExternalTransaction(MuleContext muleContext) throws TransactionException;

    /**
     * Determines whether this transaction factory creates transactions that are
     * really transacted or if they are being used to simulate batch actions, such as
     * using Jms Client Acknowledge.
     * 
     * @return
     */
    boolean isTransacted();
}
