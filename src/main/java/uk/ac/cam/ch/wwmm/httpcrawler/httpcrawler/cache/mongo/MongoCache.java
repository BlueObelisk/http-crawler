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

package uk.ac.cam.ch.wwmm.httpcrawler.httpcrawler.cache.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.joda.time.DateTime;
import uk.ac.cam.ch.wwmm.httpcrawler.httpcrawler.cache.AbstractHttpCache;
import uk.ac.cam.ch.wwmm.httpcrawler.httpcrawler.cache.CacheRequest;
import uk.ac.cam.ch.wwmm.httpcrawler.httpcrawler.cache.CacheResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Sam Adams
 */
public class MongoCache extends AbstractHttpCache {

    private DB db;
    private DBCollection col;

    public MongoCache(DB db, String collection) {
        this.db = db;
        this.col = db.getCollection(collection);
        // Create index
         col.createIndex(new BasicDBObject("id", 1));
    }

    public CacheResponse get(CacheRequest request) throws IOException {
        String id = request.getId();

        BasicDBObject query = new BasicDBObject("id", id);
        DBObject doc = col.findOne(query);
        if (doc != null) {
            URI url = URI.create((String) doc.get("url"));
            List<Header> headers = getHeaders((String[])doc.get("headers"));
            DateTime cached = DTF.parseDateTime((String)doc.get("timestamp"));
            InputStream in = uncompress((byte[])doc.get("content"));
            CacheResponse response = new CacheResponse(id, url, headers, in, cached);
            return response;
        }
        return null;
    }

    private InputStream uncompress(byte[] bytes) throws IOException {
        ByteArrayInputStream content = new ByteArrayInputStream(bytes);
        return new GZIPInputStream(content);
    }

    private List<Header> getHeaders(String[] s) {
        List<Header> list = new ArrayList<Header>();
        for (String line : s) {
            int i = line.indexOf(':');
            Header h = new BasicHeader(line.substring(i), line.substring(i+2));
            list.add(h);
        }
        return list;
    }

    public void store(String id, URI url, Header[] headers, byte[] bytes) throws IOException {
        DateTime now = new DateTime();
        store(id, url, headers, now, bytes);
    }

    public void store(String id, URI url, Header[] headers, DateTime timestamp, byte[] bytes) throws IOException {
        BasicDBObject doc = new BasicDBObject();
        doc.put("id", id);
        doc.put("url", url.toString());
        doc.put("headers", getHeaderStrings(headers));
        doc.put("timestamp", DTF.print(timestamp));
        byte[] content = compress(bytes);
        doc.put("content", content);
        col.save(doc);
    }

    private byte[] compress(byte[] bytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        GZIPOutputStream out = new GZIPOutputStream(buffer);
        out.write(bytes);
        out.close();
        return buffer.toByteArray();
    }

    private String getContentString(byte[] bytes) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            return readUtf8String(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private List<String> getHeaderStrings(Header[] headers) {
        List<String> list = new ArrayList<String>();
        for (Header h : headers) {
            list.add(h.getName() + ": " + h.getValue());
        }
        return list;
    }

}
