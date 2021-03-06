/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.application.Application;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.FileUtils;
import org.mule.util.FilenameUtils;

import java.beans.Introspector;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleDeployer implements MuleDeployer
{

    protected transient final Log logger = LogFactory.getLog(getClass());
    protected DeploymentService deploymentService;

    public DefaultMuleDeployer(DeploymentService deploymentService)
    {
        this.deploymentService = deploymentService;
    }

    public void deploy(Application app)
    {
        final ReentrantLock lock = deploymentService.getLock();
        try
        {
            if (!lock.tryLock(0, TimeUnit.SECONDS))
            {
                return;
            }
            app.install();
            app.init();
            app.start();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return;
        }
        catch (Throwable t)
        {
            app.dispose();

            if (t instanceof DeploymentException)
            {
                // re-throw as is
                throw ((DeploymentException) t);
            }

            final String msg = String.format("Failed to deploy application [%s]", app.getAppName());
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
        finally
        {
            if (lock.isHeldByCurrentThread())
            {
                lock.unlock();
            }
        }
    }

    public void undeploy(Application app)
    {
        final ReentrantLock lock = deploymentService.getLock();
        try
        {
            if (!lock.tryLock(0, TimeUnit.SECONDS))
            {
                return;
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return;
        }

        try
        {
            tryToStopApp(app);
            tryToDisposeApp(app);

            try
            {
                final File appDir = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), app.getAppName());
                FileUtils.deleteDirectory(appDir);

                // remove a marker, harmless, but a tidy app dir is always better :)
                final File marker = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), String.format("%s-anchor.txt", app.getAppName()));
                marker.delete();

                Introspector.flushCaches();
            }
            catch (IOException e)
            {
                throw new DeploymentException(MessageFactory.createStaticMessage("Cannot delete application folder"), e);
            }
        }
        finally
        {
            if (lock.isHeldByCurrentThread())
            {
                lock.unlock();
            }
        }
    }

    private void tryToDisposeApp(Application app)
    {
        try
        {
            app.dispose();
        }
        catch (Throwable t)
        {
            logger.error(String.format("Unable to cleanly dispose application '%s'. Restart Mule if you get errors redeploying this application", app.getAppName()), t);
        }
    }

    private void tryToStopApp(Application app)
    {
        try
        {
            app.stop();
        }
        catch (Throwable t)
        {
            logger.error(String.format("Unable to cleanly stop application '%s'. Restart Mule if you get errors redeploying this application", app.getAppName()), t);
        }
    }

    public Application installFromAppDir(String packedMuleAppFileName) throws IOException
    {
        final ReentrantLock lock = deploymentService.getLock();
        try
        {
            if (!lock.tryLock(0, TimeUnit.SECONDS))
            {
                throw new IOException("Another deployment operation is in progress");
            }

            final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();
            File appFile = new File(appsDir, packedMuleAppFileName);
            // basic security measure: outside apps dir use installFrom(url) and go through any
            // restrictions applied to it
            if (!appFile.getParentFile().equals(appsDir))
            {
                throw new SecurityException("installFromAppDir() can only deploy from $MULE_HOME/apps. Use installFrom(url) instead.");
            }
            return installFrom(appFile.toURL());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException("Install operation has been interrupted");
        }
        finally
        {
            if (lock.isHeldByCurrentThread())
            {
                lock.unlock();
            }
        }
    }

    public Application installFrom(URL url) throws IOException
    {
        // TODO plug in app-bloodhound/validator here?
        if (!url.toString().endsWith(".zip"))
        {
            throw new IllegalArgumentException("Invalid Mule application archive: " + url);
        }

        final String baseName = FilenameUtils.getBaseName(url.toString());
        if (baseName.contains("%20"))
        {
            throw new DeploymentInitException(
                    MessageFactory.createStaticMessage("Mule application name may not contain spaces: " + baseName));
        }

        final ReentrantLock lock = deploymentService.getLock();

        String appName;
        File appDir = null;
        boolean errorEncountered = false;
        try
        {
            if (!lock.tryLock(0, TimeUnit.SECONDS))
            {
                throw new IOException("Another deployment operation is in progress");
            }

            final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();

            final String fullPath = url.toURI().toString();

            if (logger.isInfoEnabled())
            {
                logger.info("Exploding a Mule application archive: " + fullPath);
            }

            appName = FilenameUtils.getBaseName(fullPath);
            appDir = new File(appsDir, appName);
            // normalize the full path + protocol to make unzip happy
            final File source = new File(url.toURI());
            
            FileUtils.unzip(source, appDir);
            if ("file".equals(url.getProtocol()))
            {
                FileUtils.deleteQuietly(source);
            }
        }
        catch (URISyntaxException e)
        {
            errorEncountered = true;
            final IOException ex = new IOException(e.getMessage());
            ex.fillInStackTrace();
            throw ex;
        }
        catch (InterruptedException e)
        {
            errorEncountered = true;
            Thread.currentThread().interrupt();
            throw new IOException("Install operation has been interrupted");
        }
        catch (IOException e)
        {
            errorEncountered = true;
            // re-throw
            throw e;
        }
        catch (Throwable t)
        {
            errorEncountered = true;
            final String msg = "Failed to install app from URL: " + url;
            throw new DeploymentInitException(MessageFactory.createStaticMessage(msg), t);
        }
        finally {
            // delete an app dir, as it's broken
            if (errorEncountered && appDir != null && appDir.exists())
            {
                final boolean couldNotDelete = FileUtils.deleteTree(appDir);
                /*
                if (couldNotDelete)
                {
                    final String msg = String.format("Couldn't delete app directory '%s' after it failed to install", appDir);
                    logger.error(msg);
                }
                */
            }
            if (lock.isHeldByCurrentThread())
            {
                lock.unlock();
            }
        }

        // appname is never null by now
        return deploymentService.getAppFactory().createApp(appName);
    }
}
