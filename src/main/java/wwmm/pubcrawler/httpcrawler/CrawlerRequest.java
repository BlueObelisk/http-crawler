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
package wwmm.pubcrawler.httpcrawler;

import org.joda.time.Duration;

import java.net.URI;

/**
 * @author Sam Adams
 */
public class CrawlerRequest {

    private final String id;
    private final URI url;
    private final Duration maxAge;

    public CrawlerRequest(URI url, String id, Duration maxAge) {
        this.url = url;
        this.id = id;
        this.maxAge = maxAge;
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

}
