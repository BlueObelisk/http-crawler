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
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.Header;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Sam Adams
 */
public class CrawlerResponse {

    private final URI url;
    private final List<Header> headers;
    private InputStream content;
    private final boolean stale;

    public CrawlerResponse(URI url, List<? extends Header> headers, InputStream content, boolean stale) {
        this.url = url;
        this.headers = new ArrayList<Header>(headers);
        this.content = content;
        this.stale = true;
    }

    public URI getUrl() {
        return url;
    }

    public boolean isStale() {
        return stale;
    }

    public InputStream getContent() {
        if (content == null) {
            throw new IllegalStateException("Stream closed");
        }
        return content;
    }

    public List<Header> getAllHeaders() {
        return Collections.unmodifiableList(headers);
    }

    public Header getFirstHeader(String name) {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                return header;
            }
        }
        return null;
    }

    public void closeQuietly() {
        try {
            close();
        } catch (IOException e) { }
    }

    public synchronized void close() throws IOException {
        if (content != null) {
            try {
                IOUtils.copy(content, NullOutputStream.NULL_OUTPUT_STREAM);
                content.close();
            } finally {
                content = null;
            }
        }
    }

    public Header getContentType() {
        return getFirstHeader("Content-type");
    }
    
}
