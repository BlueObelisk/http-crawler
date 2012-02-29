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

package uk.ac.cam.ch.wwmm.httpcrawler.mongodb;

import com.mongodb.*;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.joda.time.DateTime;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.AbstractHttpCache;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.CacheRequest;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.CacheResponse;

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

    private final DB db;
    private final GridFS fs;

    public MongoCache(final DB db, final String collection) {
        this.db = db;

        this.fs = new GridFS(db, collection);
        this.db.getCollection(collection + ".files").ensureIndex(
                BasicDBObjectBuilder.start().add("filename", 1).add("unique", true).get());
    }

    public CacheResponse get(final CacheRequest request) throws IOException {
        final String filename = request.getId();

        final GridFSDBFile file = fs.findOne(filename);
        if (file != null) {
            final URI url = URI.create((String) file.get("url"));
            final BasicDBList list = (BasicDBList) file.get("headers");
            final List<Header> headers = getHeaders(list);
            final DateTime cached = DTF.parseDateTime((String) file.get("timestamp"));
            final InputStream in = new GZIPInputStream(file.getInputStream());
            final CacheResponse response = new CacheResponse(filename, url, headers, in, cached);
            return response;
        }
        return null;
    }

    private List<Header> getHeaders(final List<?> s) {
        final List<Header> list = new ArrayList<Header>();
        for (final Object o : s) {
            final String line = (String) o;
            final int i = line.indexOf(':');
            final Header h = new BasicHeader(line.substring(i), line.substring(i+2));
            list.add(h);
        }
        return list;
    }

    public void store(final String filename, final URI url, final Header[] headers, final byte[] bytes) throws IOException {
        final DateTime now = new DateTime();
        store(filename, url, headers, now, bytes);
    }

    public void store(final String filename, final URI url, final Header[] headers, final DateTime timestamp, final byte[] bytes) throws IOException {
        final byte[] content = compress(bytes);

        final GridFSInputFile file = fs.createFile(content);
        file.setFilename(filename);
        file.put("url", url.toString());
        file.put("headers", getHeaderStrings(headers));
        file.put("timestamp", DTF.print(timestamp));
        fs.remove(filename);
        file.save();
    }

    private byte[] compress(final byte[] bytes) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final GZIPOutputStream out = new GZIPOutputStream(buffer);
        out.write(bytes);
        out.close();
        return buffer.toByteArray();
    }

    private List<String> getHeaderStrings(final Header[] headers) {
        final List<String> list = new ArrayList<String>();
        for (final Header h : headers) {
            list.add(h.getName() + ": " + h.getValue());
        }
        return list;
    }

}
