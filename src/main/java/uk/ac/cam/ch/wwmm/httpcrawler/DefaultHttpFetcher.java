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
package uk.ac.cam.ch.wwmm.httpcrawler;

import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.CacheRequest;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.CacheResponse;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.HttpCache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Sam Adams
 */
public class DefaultHttpFetcher implements HttpFetcher {

    private static final Logger LOG = Logger.getLogger(DefaultHttpFetcher.class);

    private final HttpCache cache;
    private final HttpClient client;

    private final long maxBackoffSeconds = TimeUnit.HOURS.toSeconds(4);
    private final int maxRetriesOnIOError = 3;

    private long requestStepMillis = 1000l;
    private long lastRequestTime;

    private long backOffSeconds;
    private long backOffStepSeconds = 1;

    public DefaultHttpFetcher(final HttpClient client) {
        this.client = client;
        this.cache = null;
    }

    public DefaultHttpFetcher(final HttpClient client, final HttpCache cache) {
        this.client = client;
        this.cache = cache;
    }

    protected synchronized HttpClient getClient() {
        final long backOffMillis = SECONDS.toMillis(backOffSeconds);
        final long delay = (requestStepMillis > 0) ? requestStepMillis + backOffMillis : backOffMillis;
        sleepUntil(lastRequestTime + delay);
        lastRequestTime = System.currentTimeMillis();
        return client;
    }

    private void sleepUntil(final long targetTime) {
        long now = System.currentTimeMillis();
        while (now < targetTime) {
            LockSupport.parkNanos((targetTime - now) * 1000000);
            now = System.currentTimeMillis();
        }
    }

    protected HttpCache getCache() {
        return cache;
    }

    public long getRequestStepMillis() {
        return requestStepMillis;
    }

    public void setRequestStepMillis(final long requestStepMillis) {
        this.requestStepMillis = requestStepMillis;
    }


    public CrawlerResponse fetchFromCache(final String id) throws IOException {
        final HttpCache cache = getCache();
        if (cache != null) {
            final CacheRequest cacheRequest = new CacheRequest(id);
            final CacheResponse cacheResponse = cache.get(cacheRequest);
            if (cacheResponse != null) {
                return createResponse(cacheResponse);
            }
        }
        return null;
    }


    public CrawlerResponse execute(final CrawlerRequest request) throws IOException {
        return execute(request, null);
    }

    public CrawlerResponse execute(final CrawlerRequest request, final HttpContext context) throws IOException {

        if (request.getId() == null || request.getId().startsWith("null")) {
            throw new IOException("Null ID: "+request.getId());
        }

        final HttpCache cache = getCache();
        CacheResponse cacheResponse = null;
        if (cache != null) {
            final CacheRequest cacheRequest = getCacheRequest(request);
            cacheResponse = cache.get(cacheRequest);
            if (cacheResponse != null) {
                if (cacheResponse.isUpToDate(request.getMaxAge())) {
                    LOG.trace("Cache hit: "+request.getId());
                    return createResponse(cacheResponse);
                } else {
                    LOG.trace("Cache expired: "+request.getId());
                }
            } else {
                LOG.trace("Cache miss: "+request.getId());
            }
        }

        final HttpUriRequest httpRequest = createHttpRequest(request);
        final HttpContext httpContext = context == null ? createContext() : context;
        FetcherParams.setKey(httpContext, request.getId());

        HttpResponse httpResponse = null;
        int remainingAttempts = maxRetriesOnIOError;
        final Exception lastEx = null;
        while (httpResponse == null && remainingAttempts > 0) {
            remainingAttempts--;
            try {
                LOG.debug("Issuing HTTP "+httpRequest.getMethod()+" "+httpRequest.getURI());
                httpResponse = getClient().execute(httpRequest, httpContext);
            } catch (IOException e) {
                backOff();
                LOG.warn("Error fetching "+httpRequest.getURI()
                        + " Back-off for " + backOffSeconds + " seconds"
                        + (remainingAttempts > 0 ? " [retrying]" : ""), e);
                if (remainingAttempts == 0) {
                    if (cacheResponse != null) {
                        // Return stale response
                        LOG.error("Failed to fetch " + httpRequest.getURI() + " ... using stale version", lastEx);
                        return createResponse(cacheResponse, true);
                    }
                    throw new IOException("Failed to fetch "+httpRequest.getURI(), lastEx);
                }
            }

        }
        try {
            if (httpResponse.getStatusLine().getStatusCode() >= 400) {
                backOff();
                LOG.warn(format("HTTP Status %d (%s).  Back-off for %d seconds", httpResponse.getStatusLine().getStatusCode(),
                    httpResponse.getStatusLine().getReasonPhrase(), backOffSeconds));
            } else {
                resetBackOff();
            }

            if (isSuccess(httpResponse)) {
                final URI url = getResponseUrl(httpRequest, httpContext);
                final byte[] bytes = readEntity(httpResponse);
                final Header[] headers = httpResponse.getAllHeaders();
                cacheResponse(request.getId(), url, headers, bytes);
                return createResponse(url, headers, bytes, false, false);
            } else {
                throw new IOException("Crawler failed ["+request.getUrl()+"] "+httpResponse.getStatusLine());
            }

        } finally {
            closeQuietly(httpResponse);
        }
    }

    private HttpContext createContext() {
        final HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
        return httpContext;
    }

    private void backOff() {
        long tmp = backOffSeconds;
        backOffSeconds = Math.min(maxBackoffSeconds, backOffSeconds + backOffStepSeconds);
        backOffStepSeconds = tmp;
        // 1 1 2 3 5
    }

    private void resetBackOff() {
        backOffSeconds = 0;
        backOffStepSeconds = 1;
    }


    private void cacheResponse(final String id, final URI url, final Header[] headers, final byte[] bytes) throws IOException {
        if (getCache() != null) {
            LOG.trace("Cached: "+id);
            getCache().store(id, url, asList(headers), bytes);
        }
    }

    private CrawlerResponse createResponse(final URI url, final Header[] headers, final byte[] bytes, final boolean fromCache, final boolean stale) {
        final List<Header> headerList = Arrays.asList(headers);
        final InputStream content = new ByteArrayInputStream(bytes);
        final CrawlerResponse response = new CrawlerResponse(url, headerList, content, fromCache, stale);
        return response;
    }

    private byte[] readEntity(final HttpResponse httpResponse) throws IOException {
        final HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return new byte[0];
        }
        return IOUtils.toByteArray(entity.getContent());
    }

    private URI getResponseUrl(final HttpUriRequest httpRequest, final HttpContext httpContext) {
        final HttpHost host = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        final HttpUriRequest request = (HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST);
        if (host == null || request == null) {
            return httpRequest.getURI();
        }
        return URI.create(host.toURI()).resolve(request.getURI());
    }

    private boolean isSuccess(final HttpResponse httpResponse) {
        return HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode();
    }

    private HttpUriRequest createHttpRequest(final CrawlerRequest request) {
        final HttpUriRequest httpRequest;
        if (request instanceof CrawlerGetRequest) {
            httpRequest = createHttpRequest((CrawlerGetRequest) request);
        }
        else if (request instanceof CrawlerPostRequest) {
            httpRequest = createHttpRequest((CrawlerPostRequest) request);
        }
        else {
            throw new UnsupportedOperationException("Unknown request type: "+request.getClass());
        }
        if (request.getReferrer() != null) {
            String referer = request.getReferrer().toString();
            if (referer.indexOf('#') != -1) {
                // Remove fragment
                referer = referer.substring(0, referer.indexOf('#'));
            }
            httpRequest.addHeader("Referer", referer);
        }
        return httpRequest;
    }

    private HttpUriRequest createHttpRequest(final CrawlerGetRequest request) {
        final HttpGet httpRequest = new HttpGet(request.getUrl());
        return httpRequest;
    }

    private HttpUriRequest createHttpRequest(final CrawlerPostRequest request) {
        final HttpPost httpRequest = new HttpPost(request.getUrl());
        httpRequest.setEntity(createEntity(request));
        return httpRequest;
    }

    private HttpEntity createEntity(final CrawlerPostRequest request) {
        try {
            return new UrlEncodedFormEntity(request.getParameters(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding error", e);
        }
    }

    private CacheRequest getCacheRequest(final CrawlerRequest request) {
        final CacheRequest cacheRequest = new CacheRequest(request.getId());
        return cacheRequest;
    }

    private CrawlerResponse createResponse(final CacheResponse cacheResponse) {
        return createResponse(cacheResponse, false);
    }

    private CrawlerResponse createResponse(final CacheResponse cacheResponse, final boolean stale) {
        final CrawlerResponse response = new CrawlerResponse(
                cacheResponse.getUrl(),
                cacheResponse.getHeaders(),
                cacheResponse.getContent(),
                true,
                stale
        );
        return response;
    }

    private static void closeQuietly(final HttpResponse response) {
        try {
            if (response.getEntity() != null) {
                response.getEntity().consumeContent();
            }
        } catch (IOException e) {
            // ignore
        }
    }

}
