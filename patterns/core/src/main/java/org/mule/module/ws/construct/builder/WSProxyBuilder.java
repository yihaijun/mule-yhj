/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.construct.builder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.construct.builder.AbstractFlowConstructWithSingleInboundAndOutboundEndpointBuilder;
import org.mule.module.ws.construct.WSProxy;
import org.mule.util.FileUtils;

public class WSProxyBuilder extends
    AbstractFlowConstructWithSingleInboundAndOutboundEndpointBuilder<WSProxyBuilder, WSProxy>
{
    protected URI wsldLocation;
    protected File wsdlFile;

    @Override
    protected MessageExchangePattern getInboundMessageExchangePattern()
    {
        return MessageExchangePattern.REQUEST_RESPONSE;
    }

    @Override
    protected MessageExchangePattern getOutboundMessageExchangePattern()
    {
        return MessageExchangePattern.REQUEST_RESPONSE;
    }

    public WSProxyBuilder outboundTransformers(Transformer... outboundTransformers)
    {
        this.outboundTransformers = Arrays.asList((MessageProcessor[]) outboundTransformers);
        return this;
    }

    public WSProxyBuilder outboundResponseTransformers(Transformer... outboundResponseTransformers)
    {
        this.outboundResponseTransformers = Arrays.asList((MessageProcessor[]) outboundResponseTransformers);
        return this;
    }

    public WSProxyBuilder wsldLocation(URI wsldLocation)
    {
        this.wsldLocation = wsldLocation;
        return this;
    }

    public WSProxyBuilder wsdlFile(File wsdlFile)
    {
        this.wsdlFile = wsdlFile;
        return this;
    }

    @Override
    protected WSProxy buildFlowConstruct(MuleContext muleContext) throws MuleException
    {
        if (wsdlFile != null)
        {
            return buildStaticWsdlContentsWSProxy(muleContext);
        }

        if (wsldLocation != null)
        {
            return buildStaticWsdlUriWSProxy(muleContext);
        }

        return buildDynamicWsdlUriWSProxy(muleContext);
    }

    private WSProxy buildDynamicWsdlUriWSProxy(MuleContext muleContext) throws MuleException
    {
        return new WSProxy(name, muleContext, getOrBuildInboundEndpoint(muleContext),
            getOrBuildOutboundEndpoint(muleContext));
    }

    private WSProxy buildStaticWsdlContentsWSProxy(MuleContext muleContext) throws MuleException
    {
        try
        {
            return new WSProxy(name, muleContext, getOrBuildInboundEndpoint(muleContext),
                getOrBuildOutboundEndpoint(muleContext), FileUtils.readFileToString(wsdlFile));
        }
        catch (final IOException ioe)
        {
            throw new ConfigurationException(ioe);
        }
    }

    private WSProxy buildStaticWsdlUriWSProxy(MuleContext muleContext) throws MuleException
    {
        return new WSProxy(name, muleContext, getOrBuildInboundEndpoint(muleContext),
            getOrBuildOutboundEndpoint(muleContext), wsldLocation);
    }
}
