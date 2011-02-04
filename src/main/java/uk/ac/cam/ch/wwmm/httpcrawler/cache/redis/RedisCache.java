/*
 * Copyright 2011 Sam Adams
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.ch.wwmm.httpcrawler.cache.redis;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.joda.time.DateTime;
import redis.clients.jedis.Jedis;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.AbstractHttpCache;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.CacheRequest;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.CacheResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sam Adams
 */
public class RedisCache extends AbstractHttpCache {

    private Jedis jedis;
    private String prefix;

    public RedisCache(Jedis jedis, String prefix) {
        this.jedis = jedis;
        this.prefix = prefix;
    }

    public CacheResponse get(CacheRequest request) throws IOException {
        String id = request.getId();
        String _id = getId(id);

        URI url = URI.create(jedis.hget(_id, "url"));
        List<Header> headers = getHeaders(jedis.hget(_id, "headers"));
        DateTime cached = DTF.parseDateTime(jedis.hget(_id, "timestamp"));
        ByteArrayInputStream content = new ByteArrayInputStream(jedis.hget(_id, "content").getBytes("UTF-8"));
        CacheResponse response = new CacheResponse(id, url, headers, content, cached);
        return response;
    }

    private List<Header> getHeaders(String s) {
        List<Header> list = new ArrayList<Header>();
        for (String line : s.split("\n")) {
            int i = line.indexOf(':');
            Header h = new BasicHeader(line.substring(i), line.substring(i+2));
            list.add(h);
        }
        return list;
    }

    private String getId(String id) {
        if (prefix == null) {
            return id;
        }
        return prefix + id;
    }

    public void store(String id, URI url, Header[] headers, byte[] bytes) throws IOException {
        String _id = getId(id);
        jedis.hset(_id, "url", url.toString());
        jedis.hset(_id, "headers", getHeaderString(headers));
        DateTime now = new DateTime();
        jedis.hset(_id, "timestamp", DTF.print(now));
        jedis.hset(_id, "content", getContentString(bytes));
    }

    private String getContentString(byte[] bytes) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            return readUtf8String(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private String getHeaderString(Header[] headers) {
        StringBuilder s = new StringBuilder();
        for (Header h : headers) {
            s.append(h.getName()).append(": ").append(h.getValue()).append('\n');
        }
        return s.toString();
    }

}
