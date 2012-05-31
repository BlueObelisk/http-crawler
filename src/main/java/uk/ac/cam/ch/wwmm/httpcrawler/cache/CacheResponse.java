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
package uk.ac.cam.ch.wwmm.httpcrawler.cache;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * @author Sam Adams
 */
public class CacheResponse {

    private final String id;
    private final URI url;
    private final List<Header> headers;
    private final InputStream content;
    private final DateTime cached;

    public CacheResponse(final String id, final URI url, final List<Header> headers, final InputStream content, final DateTime cached) {
        this.id = id;
        this.url = url;
        this.headers = headers;
        this.content = content;
        this.cached = cached;
    }

    public String getId() {
        return id;
    }

    public URI getUrl() {
        return url;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public InputStream getContent() {
        return content;
    }

    public DateTime getCached() {
        return cached;
    }

    public boolean isUpToDate(final Duration maxAge) {
        if (maxAge == null) {
            return true;
        }
        final DateTime now = new DateTime();
        final Duration age = new Duration(getCached(), now);
        return age.isShorterThan(maxAge);
    }
    
    public Header getContentTypeHeader() {
        for (final Header header : headers) {
            if ("Content-Type".equalsIgnoreCase(header.getName())) {
                return header;
            }
        }
        return null;
    }
    
    public String getCharSet() {
        final Header contentType = getContentTypeHeader();
        if (contentType != null) {
            final HeaderElement values[] = contentType.getElements();
            if (values.length > 0) {
                final NameValuePair param = values[0].getParameterByName("charset");
                if (param != null) {
                    return param.getValue();
                }
            }
        }
        return null;
    }
}
