package uk.ac.cam.ch.wwmm.httpcrawler;

import org.apache.http.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import uk.ac.cam.ch.wwmm.httpcrawler.audit.RequestAuditor;

import java.io.IOException;

/**
 * @author Sam Adams
 */
public class AuditingHttpClient extends DefaultHttpClient {

    private final RequestAuditor auditor;

    public AuditingHttpClient(final RequestAuditor auditor) {
        this.auditor = auditor;
    }

    @Override
    protected HttpRequestExecutor createRequestExecutor() {
        return new LoggingRequestExecutor();
    }

    private class LoggingRequestExecutor extends HttpRequestExecutor {

        @Override
        public HttpResponse execute(final HttpRequest request, final HttpClientConnection conn, final HttpContext context) throws IOException, HttpException {
            final long timestamp = System.currentTimeMillis();
            try {
                final HttpResponse response = super.execute(request, conn, context);
                auditResponse(request, context, timestamp, response);
                return response;
            } catch (final IOException e) {
                auditError(request, context, timestamp, e);
                throw e;
            } catch (final HttpException e) {
                auditError(request, context, timestamp, e);
                throw e;
            } catch (final RuntimeException e) {
                auditError(request, context, timestamp, e);
                throw e;
            } catch (final Error e) {
                auditError(request, context, timestamp, e);
                throw e;
            }
        }
    }

    private void auditResponse(final HttpRequest request, final HttpContext context, final long timestamp, final HttpResponse response) {
        if (auditor != null) {
            final HttpHost host = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            auditor.auditResponse(timestamp, host, request, response, context);
        }
    }

    private void auditError(final HttpRequest request, final HttpContext context, final long timestamp, final Throwable e) {
        if (auditor != null) {
            final HttpHost host = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            auditor.auditError(timestamp, host, request, e, context);
        }
    }

}
