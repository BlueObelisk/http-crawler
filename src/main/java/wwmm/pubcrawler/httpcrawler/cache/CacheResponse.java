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
package wwmm.pubcrawler.httpcrawler.cache;

import org.apache.http.Header;
import org.joda.time.DateTime;

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

    public CacheResponse(String id, URI url, List<Header> headers, InputStream content, DateTime cached) {
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
    
}
