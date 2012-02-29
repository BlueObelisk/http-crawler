package uk.ac.cam.ch.wwmm.httpcrawler;

import org.apache.http.protocol.HttpContext;

/**
 * @author Sam Adams
 */
public class FetcherParams {

    private static final String HTTPFETCHER_KEY = "httpfetcher.key";


    public static String getKey(final HttpContext context) {
        return (String) context.getAttribute(HTTPFETCHER_KEY);
    }

    public static void setKey(final HttpContext context, final String key) {
        context.setAttribute(HTTPFETCHER_KEY, key);
    }

}
