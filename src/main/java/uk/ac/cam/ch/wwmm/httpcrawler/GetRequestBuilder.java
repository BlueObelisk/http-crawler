package uk.ac.cam.ch.wwmm.httpcrawler;

import org.apache.http.cookie.Cookie;
import org.joda.time.Duration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Sam Adams
 */
public class GetRequestBuilder {
    
    private String key;
    private URI url;
    private URI referrer;
    private Duration maxAge;
    private Collection<Cookie> cookies;

    public String getKey() {
        return key;
    }

    public GetRequestBuilder withKey(final String key) {
        this.key = key;
        return this;
    }

    public URI getUrl() {
        return url;
    }

    public GetRequestBuilder withUrl(final URI url) {
        this.url = url;
        return this;
    }

    public URI getReferrer() {
        return referrer;
    }

    public GetRequestBuilder withReferrer(final URI referrer) {
        this.referrer = referrer;
        return this;
    }

    public Duration getMaxAge() {
        return maxAge;
    }

    public GetRequestBuilder withMaxAge(final Duration maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public GetRequestBuilder withCookies(final Collection<Cookie> cookies) {
        this.cookies = cookies;
        return this;
    }

    public CrawlerGetRequest build() {
        return new CrawlerGetRequest(url, key, maxAge, referrer, cookies);
    }

}
