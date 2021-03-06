/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import java.text.MessageFormat;
import java.util.Properties;

public class JXPathExpressionWithNamespaceTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public static final String MESSAGE = "<foo:endpoint xmlns:foo=\"http://foo.com\">{0}</foo:endpoint>";

    public JXPathExpressionWithNamespaceTestCase()
    {
        super(true);
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.expression", "/foo:endpoint");
        p.setProperty("selector.evaluator", "jxpath");

        return p;
    }

    @Override
    protected Object getMatchMessage() throws Exception
    {
        return documentFor("matchingEndpoint1");
    }

    @Override
    protected Object getErrorMessage() throws Exception
    {
        return documentFor("missingEndpoint");
    }

    protected String documentFor(String name) throws Exception
    {
        return MessageFormat.format(MESSAGE, name);
    }

}
