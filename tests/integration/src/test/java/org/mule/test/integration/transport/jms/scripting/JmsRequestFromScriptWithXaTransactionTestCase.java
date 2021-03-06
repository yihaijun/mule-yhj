/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.jms.scripting;

/**
 * Tests that we are able to request jms messages from inside the script when
 * the endpoints are defined using XA transactions.
 */
public class JmsRequestFromScriptWithXaTransactionTestCase extends AbstractJmsRequestFromScriptTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transport/jms/scripting/jms-request-from-script-with-xa-transaction-config.xml";
    }
}
