/*
 * <p>标题: 中国电信综合调度系统第4版</p>
 * <p>描述: ...</p>
 * <p>版权: Copyright (c) 2012</p>
 * <p>公司: 天讯瑞达通信技术有限公司</p>
 * <p>网址：http://www.tisson.cn/</p>
 */
package com.regaltec.asip.transformer.xslt;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.TransformerFactoryImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>概述该类作用，请详细描述。</p>
 * <p>创建日期：2012-8-27 下午05:16:30</p>
 *
 * @author yihaijun
 */
public class SaxonCachingTransformerFactory {//extends TransformerFactoryImpl {
    /** Map to hold templates cache. */
    private static Map templatesCache = new HashMap();

    /** Factory logger. */
    protected static Logger logger = LoggerFactory.getLogger(SaxonCachingTransformerFactory.class);

    /** Active readers count. */
    protected static int activeReaders = 0;
    /** Active writers count. */
    protected static int activeWriters = 0;

    /** 
     * Process the source into a Transformer object. If source is a StreamSource 
     * with <code>systemID</code> pointing to a file, transformer is produced 
     * from a cached templates object. Cache is done in soft references; cached 
     * objects are reloaded, when file's date of last modification changes. 
     * @param source An object that holds a URI, input stream, etc. 
     * @return A Transformer object that may be used to perform a transformation 
     * in a single thread, never null. 
     * @throws TransformerConfigurationException - May throw this during the 
     * parse when it is constructing the Templates object and fails. 
     * 
     */
    public Transformer newTransformer(final Source source) throws TransformerConfigurationException {
        if(source==null){
            logger.warn("newTransformer:source==null");
            return null;
        }
        // Check that source in a StreamSource
        if (source instanceof StreamSource)
            try {
                // Create URI of the source
                String srcId = source.getSystemId();
                if(srcId==null){
                    logger.warn("newTransformer:srcId==null");
                }
                if (srcId.indexOf(':') == 1) { // fix up windows path so that uri works
                    srcId = srcId.replaceAll("\\\\", "/");
                    srcId = srcId.substring(2);
                    srcId = srcId.replaceAll(" ", "%20");
                }
                final URI uri = new URI(srcId);
                if(uri==null){
                    logger.warn("newTransformer:uri==null");
                }
                // If URI points to a file, load transformer from the file
                // (or from the cache)
                if(logger.isDebugEnabled()){
                    logger.debug("uri.getScheme()="+uri.getScheme());
                }
                if ("file".equalsIgnoreCase(uri.getScheme())){
                    return newTransformer(new File(uri.getPath()));
                }else{
                }
            } catch (URISyntaxException urise) {
                throw new TransformerConfigurationException(urise);
            }
//        return super.newTransformer(source);
            return null;
    }

    /** 
     * Creates a transformer from a file (and caches templates) or from 
     * cached templates object. 
     * @param file file to load transformer from. 
     * @return Transformer, built from given file. 
     * @throws TransformerConfigurationException if there was a problem loading 
     * transformer from the file. 
     */
    protected Transformer newTransformer(final File file) throws TransformerConfigurationException {
        if(file==null){
            logger.warn("newTransformer:file==null");
            return null;
        }
        // Search the cache for the templates entry
        TemplatesCacheEntry templatesCacheEntry = read(file.getAbsolutePath());

        // If entry found
        if (templatesCacheEntry != null) {
            // Check timestamp of modification
            if (templatesCacheEntry.lastModified < templatesCacheEntry.templatesFile.lastModified())
                templatesCacheEntry = null;
        }
        // If no templatesEntry is found or this entry was obsolete
        if (templatesCacheEntry == null) {
            if(logger.isDebugEnabled()){
                logger.debug("Loading transformation [" + file.getAbsolutePath() + "].");
            }
            // If this file does not exists, throw the exception
            if (!file.exists()) {
                throw new TransformerConfigurationException("Requested transformation [" + file.getAbsolutePath()
                        + "] does not exist.");
            }

            // Create new cache entry
//            templatesCacheEntry = new TemplatesCacheEntry(newTemplates(new StreamSource(file)), file);

            // Save this entry to the cache
            write(file.getAbsolutePath(), templatesCacheEntry);
        } else {
            if(logger.isDebugEnabled()){
                logger.debug("Using cached transformation [" + file.getAbsolutePath() + "].");
            }
        }
        return templatesCacheEntry.templates.newTransformer();
    }

    /** 
     * Returns a templates cache entry for the specified absolute path. 
     * @param absolutePath absolute path of the entry. 
     * @return Templates cache entry for the specified path. 
     */
    protected TemplatesCacheEntry read(String absolutePath) {
        beforeRead();
        final TemplatesCacheEntry templatesCacheEntry = (TemplatesCacheEntry) templatesCache.get(absolutePath);
        afterRead();
        return templatesCacheEntry;
    }

    /** 
     * Saves templates cache entry for the specified absolute path. 
     * @param absolutePath absolute path of the entry. 
     * @param templatesCacheEntry templates cache entry to save. 
     */
    protected void write(String absolutePath, TemplatesCacheEntry templatesCacheEntry) {
        beforeWrite();
        templatesCache.put(absolutePath, templatesCacheEntry);
        afterWrite();
    }

    /** 
     * Invoked just before reading, waits until reading is allowed. 
     */
    protected synchronized void beforeRead() {
        while (activeWriters > 0)
            try {
                wait();
            } catch (InterruptedException iex) {
            }
        ++activeReaders;
    }

    /** 
     * Invoked just after reading. 
     */
    protected synchronized void afterRead() {
        --activeReaders;
        notifyAll();
    }

    /** 
     * Invoked just before writing, waits until writing is allowed. 
     */
    protected synchronized void beforeWrite() {
        while (activeReaders > 0 || activeWriters > 0)
            try {
                wait();
            } catch (InterruptedException iex) {
            }
        ++activeWriters;
    }

    /** 
     * Invoked just after writing. 
     */
    protected synchronized void afterWrite() {
        --activeWriters;
        notifyAll();
    }

    /** 
     * Private class to hold templates cache entry. 
     */
    private class TemplatesCacheEntry {
        /** When was the cached entry last modified. */
        private long lastModified;

        /** Cached templates object. */
        private Templates templates;

        /** Templates file object. */
        private File templatesFile;

        /** 
         * Constructs a new cache entry. 
         * @param templates templates to cache. 
         * @param templatesFile file, from which this transformer was loaded. 
         */
        private TemplatesCacheEntry(final Templates templates, final File templatesFile) {
            this.templates = templates;
            this.templatesFile = templatesFile;
            this.lastModified = templatesFile.lastModified();
        }
    }
}
