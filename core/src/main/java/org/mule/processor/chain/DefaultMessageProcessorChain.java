/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.transformer.Transformer;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.context.notification.MessageProcessorNotification;
import org.mule.routing.MessageFilter;
import org.mule.routing.requestreply.ReplyToMessageProcessor;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DefaultMessageProcessorChain extends AbstractMessageProcessorChain
{

    protected DefaultMessageProcessorChain(List<MessageProcessor> processors)
    {   
        super(null, processors);
    }

    protected DefaultMessageProcessorChain(MessageProcessor... processors)
    {
        super(null, Arrays.asList(processors));
    }

    protected DefaultMessageProcessorChain(String name, List<MessageProcessor> processors)
    {
        super(name, processors);
    }

    protected DefaultMessageProcessorChain(String name, MessageProcessor... processors)
    {
        super(name, Arrays.asList(processors));
    }

    public static MessageProcessorChain from(MessageProcessor messageProcessor)
    {
        return new DefaultMessageProcessorChain(messageProcessor);
    }

    public static MessageProcessorChain from(MessageProcessor... messageProcessors) throws MuleException
    {
        return new DefaultMessageProcessorChainBuilder().chain(messageProcessors).build();
    }

    public static MessageProcessorChain from(List<MessageProcessor> messageProcessors) throws MuleException
    {
        return new DefaultMessageProcessorChainBuilder().chain(messageProcessors).build();
    }
    
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        FlowConstruct flowConstruct = event.getFlowConstruct();
        MuleEvent currentEvent = event;
        MuleEvent resultEvent;
        MuleEvent copy = null;
        Iterator<MessageProcessor> processorIterator = processors.iterator();
        MessageProcessor processor = null;
        if (processorIterator.hasNext())
        {
            processor = processorIterator.next();
        }
        boolean resultWasNull = false;
        while (processor != null)
        {
            MessageProcessor nextProcessor = null;
            if (processorIterator.hasNext())
            {
                nextProcessor = processorIterator.next();
            }
            fireNotification(event.getFlowConstruct(), event, processor,
                MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);

            if (flowConstruct instanceof SimpleFlowConstruct && nextProcessor != null
                && processorMayReturnNull(processor))
            {
                copy = OptimizedRequestContext.criticalSetEvent(currentEvent);
            }

            resultEvent = processor.process(currentEvent);
            if (resultWasNull && processor instanceof ReplyToMessageProcessor)
            {
                // reply-to processing should not resurrect a dead event
                resultEvent = null;
            }

            fireNotification(event.getFlowConstruct(), resultEvent, processor,
                MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);

            if (resultEvent != null)
            {
                resultWasNull = false;
                currentEvent = resultEvent;
            }
            else if (flowConstruct instanceof SimpleFlowConstruct && nextProcessor != null)
            {
                resultWasNull = true;
                // // In a flow when a MessageProcessor returns null the next
                // processor acts as an implicit
                // // branch receiving a copy of the message used for previous
                // MessageProcessor
                if (copy != null)
                {
                    currentEvent = copy;
                }
                else
                {
                    // this should not happen
                    currentEvent = OptimizedRequestContext.criticalSetEvent(currentEvent);
                }
            }
            else
            {
                // But in a service we don't do any implicit branching.
                return null;
            }
            processor = nextProcessor;
        }
        return currentEvent;
    }

    protected boolean processorMayReturnNull(MessageProcessor processor)
    {
        if (processor instanceof OutboundEndpoint)
        {
            return  !((OutboundEndpoint) processor).getExchangePattern().hasResponse();
        }
        else if (processor instanceof Component || processor instanceof Transformer
                 || processor instanceof MessageFilter)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

}
