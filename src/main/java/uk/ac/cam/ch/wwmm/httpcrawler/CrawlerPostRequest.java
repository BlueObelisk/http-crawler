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

import org.apache.http.NameValuePair;
import org.joda.time.Duration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Sam Adams
 */
public class CrawlerPostRequest extends CrawlerRequest {

    private final List<NameValuePair> parameters;

    public CrawlerPostRequest(final URI uri, final List<? extends NameValuePair> parameters, final String id, final Duration maxAge) {
        super(uri, id, maxAge);
        this.parameters = new ArrayList<NameValuePair>(parameters);
    }

    public CrawlerPostRequest(final URI uri, final String id, final Duration maxAge, final NameValuePair... parameters) {
        super(uri, id, maxAge);
        this.parameters = Arrays.asList(parameters);
    }

    public List<NameValuePair> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

}
