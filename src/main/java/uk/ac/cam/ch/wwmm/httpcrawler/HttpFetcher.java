package uk.ac.cam.ch.wwmm.httpcrawler;

import java.io.IOException;

/**
 * @author Sam Adams
 */
public interface HttpFetcher {

    CrawlerResponse fetchFromCache(String id) throws IOException;

    CrawlerResponse execute(CrawlerRequest request) throws IOException;

}
