package uk.ac.cam.ch.wwmm.httpcrawler.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import uk.ac.cam.ch.wwmm.httpcrawler.FetcherParams;
import uk.ac.cam.ch.wwmm.httpcrawler.audit.RequestAuditor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * @author Sam Adams
 */
public class MongoRequestAuditor implements RequestAuditor {

    private final DBCollection collection;

    public MongoRequestAuditor(final DBCollection collection) {
        this.collection = collection;
    }

    public void auditResponse(final long timestamp, final HttpHost host, final HttpRequest request, final HttpResponse response, final HttpContext context) {

        final DBObject o = new BasicDBObject();
        recordRequest(timestamp, host, request, context, o);
        o.put("status", response.getStatusLine().getStatusCode());
        o.put("message", response.getStatusLine().getReasonPhrase());

        collection.insert(o);
    }

    public void auditError(final long timestamp, final HttpHost host, final HttpRequest request, final Throwable error, final HttpContext context) {
        final DBObject o = new BasicDBObject();
        recordRequest(timestamp, host, request, context, o);
        o.put("error", generateStackTrace(error));

        collection.insert(o);
    }

    private void recordRequest(final long timestamp, final HttpHost host, final HttpRequest request, final HttpContext context, final DBObject o) {
        final String key = FetcherParams.getKey(context);
        if (key != null) {
            o.put("key", key);
        }
        o.put("timestamp", new Date(timestamp));
        o.put("host", host.toURI());
        if (request instanceof HttpUriRequest) {
            final HttpUriRequest httpUriRequest = (HttpUriRequest) request;
            o.put("url", httpUriRequest.getURI().toString());
            o.put("method", httpUriRequest.getMethod());
        }
    }

    private String generateStackTrace(final Throwable error) {
        final StringWriter buffer = new StringWriter();
        error.printStackTrace(new PrintWriter(buffer));
        return buffer.toString();
    }

}
