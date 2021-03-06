/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.lifecycle.RegistryLifecycleManager;
import org.mule.util.UUID;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class AbstractRegistry implements Registry
{
    /** the unique id for this Registry */
    private String id;

    protected transient Log logger = LogFactory.getLog(getClass());

    protected MuleContext muleContext;

    protected RegistryLifecycleManager lifecycleManager;

    protected AbstractRegistry(String id, MuleContext muleContext)
    {
        if (id == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull("RegistryID"));
        }
        this.id = id;
        this.muleContext = muleContext;
        lifecycleManager = createLifecycleManager();
    }

    public final synchronized void dispose()
    {
        if(lifecycleManager.getState().isStarted())
        {
            try
            {
                getLifecycleManager().fireLifecycle(Stoppable.PHASE_NAME);
            }
            catch (LifecycleException e)
            {
                logger.error("Failed to shut down registry cleanly: " + getRegistryId(), e);
            }
        }
        //Fire dispose lifecycle before calling doDispose() that that registries can clear any object caches once all objects
        //are disposed
        try
        {
            getLifecycleManager().fireLifecycle(Disposable.PHASE_NAME);
        }
        catch (LifecycleException e)
        {
            logger.error("Failed to shut down registry cleanly: " + getRegistryId(), e);
        }

        try
        {
            doDispose();
        }
        catch (Exception e)
        {
            logger.error("Failed to cleanly dispose: " + e.getMessage(), e);
        }
    }

    protected RegistryLifecycleManager createLifecycleManager()
    {
        return new RegistryLifecycleManager(getRegistryId(), this, muleContext);
    }

    abstract protected void doInitialise() throws InitialisationException;

    abstract protected void doDispose();

    public final void initialise() throws InitialisationException
    {
        if (id == null)
        {
            logger.warn("No unique id has been set on this registry");
            id = UUID.getUUID();
        }
        try
        {
            doInitialise();
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
        try
        {
            fireLifecycle(Initialisable.PHASE_NAME);
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (LifecycleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public RegistryLifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

    public void fireLifecycle(String phase) throws LifecycleException
    {
        //Implicitly call stop if necessary when disposing
        if(Disposable.PHASE_NAME.equals(phase) && lifecycleManager.getState().isStarted())
        {
            getLifecycleManager().fireLifecycle(Stoppable.PHASE_NAME);
        }
        getLifecycleManager().fireLifecycle(phase);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key)
    {
        return (T) lookupObject(key);
    }

    public <T> T lookupObject(Class<T> type) throws RegistrationException
    {
        // Accumulate objects from all registries.
        Collection<T> objects = lookupObjects(type);
        
        if (objects.size() == 1)
        {
            return objects.iterator().next();
        }
        else if (objects.size() > 1)
        {
            throw new RegistrationException(MessageFactory.createStaticMessage("More than one object of type " + type + " registered but only one expected."));
        }
        else
        {
            return null;
        }
    }
    
    public <T> Collection<T> lookupObjectsForLifecycle(Class<T> type)
    {
        // By default use the normal lookup. If a registry implementation needs a
        // different lookup implementation for lifecycle it should override this
        // method
        return lookupObjects(type);
    }


    // /////////////////////////////////////////////////////////////////////////
    // Registry Metadata
    // /////////////////////////////////////////////////////////////////////////

    public final String getRegistryId()
    {
        return id;
    }
}
