package uk.ac.cam.ch.wwmm.httpcrawler.audit;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

/**
 * @author Sam Adams
 */
public interface RequestAuditor {

    void auditResponse(long timestamp, HttpHost host, HttpRequest request, HttpResponse response, HttpContext context);
    
    void auditError(long timestamp, HttpHost host, HttpRequest request, Throwable error, HttpContext context);

}
