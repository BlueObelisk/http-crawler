/*
 * Copyright 2011 Sam Adams
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.ch.wwmm.httpcrawler.httpcrawler;

import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import uk.ac.cam.ch.wwmm.httpcrawler.httpcrawler.cache.CacheRequest;
import uk.ac.cam.ch.wwmm.httpcrawler.httpcrawler.cache.CacheResponse;
import uk.ac.cam.ch.wwmm.httpcrawler.httpcrawler.cache.HttpCache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * @author Sam Adams
 */
public class HttpCrawler {

    private static final Logger LOG = Logger.getLogger(HttpCrawler.class);

    private HttpCache cache;
    private HttpClient client;

    private long requestStepMillis = 1000l;
    private long lastRequestTime;

    public HttpCrawler(HttpClient client) {
        this.client = client;
        this.cache = null;
    }

    public HttpCrawler(HttpClient client, HttpCache cache) {
        this.client = client;
        this.cache = cache;
    }

    protected synchronized HttpClient getClient() {
        if (getRequestStepMillis() > 0) {
            sleepUntil(lastRequestTime+getRequestStepMillis());
        }
        lastRequestTime = System.currentTimeMillis();
        return client;
    }

    private void sleepUntil(long time) {
        long now = System.currentTimeMillis();
        while (now < time) {
            try {
                Thread.sleep(time - now);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
            now = System.currentTimeMillis();
        }
    }


    protected HttpCache getCache() {
        return cache;
    }


    public long getRequestStepMillis() {
        return requestStepMillis;
    }

    public void setRequestStepMillis(long requestStepMillis) {
        this.requestStepMillis = requestStepMillis;
    }


    public CrawlerResponse execute(CrawlerRequest request) throws IOException {

        if (request.getId().startsWith("null")) {
            throw new IOException("Null ID: "+request.getId());
        }

        HttpCache cache = getCache();
        if (cache != null) {
            CacheRequest cacheRequest = getCacheRequest(request);
            CacheResponse cacheResponse = cache.get(cacheRequest);
            if (cacheResponse != null) {
                if (isUpToDate(request, cacheResponse)) {
                    LOG.trace("Cache hit: "+request.getId());
                    return createResponse(cacheResponse);
                } else {
                    LOG.trace("Cache expired: "+request.getId());
                }
            } else {
                LOG.trace("Cache miss: "+request.getId());
            }
        }

        HttpUriRequest httpRequest = createHttpRequest(request);
        HttpContext httpContext = new BasicHttpContext();
        HttpResponse httpResponse = null;
        int retry = 3;
        Exception lastEx = null;
        while (httpResponse == null && retry > 0) {
            retry--;
            try {
                LOG.debug("Issuing HTTP "+httpRequest.getMethod()+" "+httpRequest.getURI());
                httpResponse = getClient().execute(httpRequest, httpContext);
            } catch (IOException e) {
                LOG.warn("Error fetching "+httpRequest.getURI()
                        + (retry > 0 ? " [retrying]" : ""), e);
                if (retry == 0) {
                    throw new IOException("Failed to fetch "+httpRequest.getURI(), lastEx);
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    e.printStackTrace();
                }

            }
        }
        try {

            if (isSuccess(httpResponse)) {
                URI url = getResponseUrl(httpRequest, httpContext);
                byte[] bytes = readEntity(httpResponse);
                Header[] headers = httpResponse.getAllHeaders();
                cacheResponse(request.getId(), url, headers, bytes);
                return createResponse(url, headers, bytes);
            } else {
                throw new IOException("Crawler failed ["+request.getUrl()+"] "+httpResponse.getStatusLine());
            }

        } finally {
            closeQuietly(httpResponse);
        }
    }


    private void cacheResponse(String id, URI url, Header[] headers, byte[] bytes) throws IOException {
        if (getCache() != null) {
            LOG.trace("Cached: "+id);
            getCache().store(id, url, headers, bytes);
        }
    }

    private CrawlerResponse createResponse(URI url, Header[] headers, byte[] bytes) {
        List<Header> headerList = Arrays.asList(headers);
        InputStream content = new ByteArrayInputStream(bytes);
        CrawlerResponse response = new CrawlerResponse(url, headerList, content);
        return response;
    }

    private byte[] readEntity(HttpResponse httpResponse) throws IOException {
        HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return new byte[0];
        }
        return IOUtils.toByteArray(entity.getContent());
    }

    private URI getResponseUrl(HttpUriRequest httpRequest, HttpContext httpContext) {
        HttpHost host = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        HttpUriRequest request = (HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST);
        if (host == null || request == null) {
            return httpRequest.getURI();
        }
        return URI.create(host.toURI()).resolve(request.getURI());
    }

    private boolean isSuccess(HttpResponse httpResponse) {
        return HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode();
    }

    private HttpUriRequest createHttpRequest(CrawlerRequest request) {
        if (request instanceof CrawlerGetRequest) {
            return createHttpRequest((CrawlerGetRequest) request);
        }
        if (request instanceof CrawlerPostRequest) {
            return createHttpRequest((CrawlerPostRequest) request);
        }
        throw new UnsupportedOperationException("Unknown request type: "+request.getClass());
    }

    private HttpUriRequest createHttpRequest(CrawlerGetRequest request) {
        HttpGet httpRequest = new HttpGet(request.getUrl());
        return httpRequest;
    }

    private HttpUriRequest createHttpRequest(CrawlerPostRequest request) {
        HttpPost httpRequest = new HttpPost(request.getUrl());
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(request.getParameters(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding error", e);
        }
        httpRequest.setEntity(entity);
        return httpRequest;
    }

    private CacheRequest getCacheRequest(CrawlerRequest request) {
        CacheRequest cacheRequest = new CacheRequest(request.getId());
        return cacheRequest;
    }

    private boolean isUpToDate(CrawlerRequest request, CacheResponse response) {
        if (request.getMaxAge() == null) {
            return true;
        }
        DateTime now = new DateTime();
        Duration age = new Duration(response.getCached(), now);
        return age.isShorterThan(request.getMaxAge());
    }

    private CrawlerResponse createResponse(CacheResponse cacheResponse) {
        CrawlerResponse response = new CrawlerResponse(
                cacheResponse.getUrl(),
                cacheResponse.getHeaders(),
                cacheResponse.getContent()
        );
        return response;
    }

    private static void closeQuietly(HttpResponse response) {
        try {
            if (response.getEntity() != null) {
                response.getEntity().consumeContent();
            }
        } catch (IOException e) {
            // ignore
        }
    }

}
