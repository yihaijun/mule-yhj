/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultMuleContextTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testDisposal() throws MuleException, InterruptedException
    {
        int threadsBeforeStart = Thread.activeCount();
        MuleContext ctx = new DefaultMuleContextFactory().createMuleContext();
        ctx.start();
        assertTrue(Thread.activeCount() > threadsBeforeStart);
        ctx.stop();
        ctx.dispose();
        // Check that workManager ("MuleServer") thread no longer exists.
        assertTrue(Thread.activeCount() == threadsBeforeStart);
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isStarted());
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }
}
