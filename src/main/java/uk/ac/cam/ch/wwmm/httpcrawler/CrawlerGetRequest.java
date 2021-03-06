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

import org.apache.http.cookie.Cookie;
import org.joda.time.Duration;

import java.net.URI;
import java.util.Collection;

/**
 * @author Sam Adams
 */
public class CrawlerGetRequest extends CrawlerRequest {

    public CrawlerGetRequest(final URI url, final String id, final Duration maxAge) {
        super(url, id, maxAge);
    }

    public CrawlerGetRequest(final URI url, final String id, final Duration maxAge, final URI referrer, final Collection<Cookie> cookies) {
        super(url, id, maxAge, referrer, cookies);
    }
    
}
