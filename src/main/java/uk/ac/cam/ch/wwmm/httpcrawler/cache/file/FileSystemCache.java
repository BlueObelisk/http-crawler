/*
 * Copyright 2011 Sam Adams
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.ch.wwmm.httpcrawler.cache.file;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.joda.time.DateTime;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.AbstractHttpCache;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.CacheRequest;
import uk.ac.cam.ch.wwmm.httpcrawler.cache.CacheResponse;

import java.io.*;
import java.net.URI;
import java.util.List;

/**
 * @author Sam Adams
 */
public class FileSystemCache extends AbstractHttpCache {

    private final File root;

    public FileSystemCache(final File dir) throws IOException {
        if (dir == null) {
            throw new IllegalArgumentException("Null cache directory");
        }
        if (!dir.isDirectory()) {
            FileUtils.forceMkdir(dir);
        }
        this.root = dir;
    }


    private File getFile(final String id) {
        return new File(root, id);
    }


    public CacheResponse get(final CacheRequest request) throws IOException {
        final File file = getFile(request.getId());
        if (!file.isFile()) {
            return null;
        }
        final BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            final URI url = URI.create(readLine(in));
            final DateTime cached = DTF.parseDateTime(readLine(in));
            final List<Header> headers = readHeaders(in);
            final byte[] bytes = IOUtils.toByteArray(in);
            final InputStream content = new ByteArrayInputStream(bytes);
            final CacheResponse response = new CacheResponse(request.getId(), url, headers, content, cached);
            return response;
        } finally {
            in.close();
        }

    }

    public void store(final String id, final URI url, final Header[] headers, final byte[] bytes) throws IOException {
        final File file = getFile(id);
        FileUtils.forceMkdir(file.getParentFile());
        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        try {
            final Writer w = new OutputStreamWriter(out, "UTF-8");
            w.write(url.toString());
            w.write('\n');
            final DateTime now = new DateTime();
            w.write(DTF.print(now));
            w.write('\n');
            for (final Header h : headers) {
                w.write(h.getName());
                w.write(": ");
                w.write(h.getValue());
                w.write('\n');
            }
            w.write('\n');
            w.flush();
            out.write(bytes);
        } finally {
            out.close();
        }
    }




}

