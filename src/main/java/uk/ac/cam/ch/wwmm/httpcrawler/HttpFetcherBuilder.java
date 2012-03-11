package uk.ac.cam.ch.wwmm.httpcrawler;

import org.apache.http.HttpHost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import uk.ac.cam.ch.wwmm.httpcrawler.audit.RequestAuditor;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.HttpCache;

import java.util.concurrent.TimeUnit;

import static org.apache.http.params.HttpConnectionParams.setConnectionTimeout;
import static org.apache.http.params.HttpConnectionParams.setSoTimeout;
import static org.apache.http.params.HttpProtocolParams.setUserAgent;

/**
 * @author Sam Adams
 */
public class HttpFetcherBuilder {
    
    private int connectionTimeout = 10000;
    private int socketTimeout = 10000;
    private HttpHost proxy;
    private String userAgent;
    
    private RequestAuditor requestAuditor;
    private HttpCache cache;

    public HttpFetcherBuilder withProxy(final String host, final int port) {
        this.proxy = new HttpHost(host, port);
        return this;
    }

    public HttpFetcherBuilder withConnectionTimeout(final int connectionTimeout, final TimeUnit timeUnit) {
        this.connectionTimeout = (int) timeUnit.toMillis(connectionTimeout);
        return this;
    }

    public HttpFetcherBuilder withSocketTimeout(final int socketTimeout, final TimeUnit timeUnit) {
        this.socketTimeout = (int) timeUnit.toMillis(socketTimeout);
        return this;
    }

    public HttpFetcherBuilder withUserAgent(final String userAgent) {
        this.userAgent = userAgent;
        return this;
    }
    
    public HttpFetcherBuilder withRequestAuditor(final RequestAuditor requestAuditor) {
        this.requestAuditor = requestAuditor;
        return this;
    }
    
    public HttpFetcherBuilder withCache(final HttpCache cache) {
        this.cache = cache;
        return this;
    }

    public HttpFetcher build() {

        final DefaultHttpClient client = new AuditingHttpClient(requestAuditor);
        client.getParams().setParameter(ClientPNames.MAX_REDIRECTS, 5);

        setConnectionTimeout(client.getParams(), connectionTimeout);
        setSoTimeout(client.getParams(), socketTimeout);
        setUserAgent(client.getParams(), userAgent);

        if (proxy != null) {
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }

        return new DefaultHttpFetcher(client, cache);
    }

}
