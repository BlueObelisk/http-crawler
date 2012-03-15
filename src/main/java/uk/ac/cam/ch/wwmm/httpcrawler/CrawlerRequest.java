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

import org.joda.time.Duration;

import java.net.URI;

/**
 * @author Sam Adams
 */
public abstract class CrawlerRequest {

    private final String id;
    private final URI url;
    private final Duration maxAge;
    private final URI referrer;

    public CrawlerRequest(final URI url, final String id, final Duration maxAge) {
        this(url, id, maxAge, null);
    }
    
    public CrawlerRequest(final URI url, final String id, final Duration maxAge, final URI referrer) {
        this.url = url;
        this.id = id;
        this.maxAge = maxAge;
        this.referrer = referrer;
    }

    public String getId() {
        return id;
    }

    public URI getUrl() {
        return url;
    }

    public Duration getMaxAge() {
        return maxAge;
    }

    public URI getReferrer() {
        return referrer;
    }

}
