/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.Transaction;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.transaction.TransactionCoordination;

import java.beans.ExceptionListener;

import org.junit.Test;

public class OptionalAsyncInterceptingMessageProcessorTestCase extends
    AsyncInterceptingMessageProcessorTestCase implements ExceptionListener
{

    @Override
    @Test
    public void testProcessRequestResponse() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE,
            getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE));

        assertSync(messageProcessor, event);
    }

    @Override
    @Test
    public void testProcessOneWay() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestInboundEndpoint(MessageExchangePattern.ONE_WAY));

        assertAsync(messageProcessor, event);
    }

    @Override
    @Test
    public void testProcessOneWayWithTx() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE,
            getTestTransactedInboundEndpoint(MessageExchangePattern.ONE_WAY));
        Transaction transaction = new TestTransaction(muleContext);
        TransactionCoordination.getInstance().bindTransaction(transaction);

        try
        {
            assertSync(messageProcessor, event);
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction(transaction);
        }
    }

    @Override
    @Test
    public void testProcessRequestResponseWithTx() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE,
            getTestTransactedInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE));
        Transaction transaction = new TestTransaction(muleContext);
        TransactionCoordination.getInstance().bindTransaction(transaction);

        try
        {
            assertSync(messageProcessor, event);
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction(transaction);
        }
    }

    @Override
    protected AsyncInterceptingMessageProcessor createAsyncInterceptingMessageProcessor(MessageProcessor listener)
        throws Exception
    {
        AsyncInterceptingMessageProcessor mp = new OptionalAsyncInterceptingMessageProcessor(
            new TestWorkManagerSource());
        mp.setListener(listener);
        return mp;
    }
}
