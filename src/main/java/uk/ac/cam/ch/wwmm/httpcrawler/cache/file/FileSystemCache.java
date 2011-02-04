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

    public FileSystemCache(File dir) throws IOException {
        if (dir == null) {
            throw new IllegalArgumentException("Null cache directory");
        }
        if (!dir.isDirectory()) {
            FileUtils.forceMkdir(dir);
        }
        this.root = dir;
    }


    private File getFile(String id) {
        return new File(root, id);
    }


    public CacheResponse get(CacheRequest request) throws IOException {
        File file = getFile(request.getId());
        if (!file.isFile()) {
            return null;
        }
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            URI url = URI.create(readLine(in));
            DateTime cached = DTF.parseDateTime(readLine(in));
            List<Header> headers = readHeaders(in);
            byte[] bytes = IOUtils.toByteArray(in);
            InputStream content = new ByteArrayInputStream(bytes);
            CacheResponse response = new CacheResponse(request.getId(), url, headers, content, cached);
            return response;
        } finally {
            in.close();
        }

    }

    public void store(String id, URI url, Header[] headers, byte[] bytes) throws IOException {
        File file = getFile(id);
        FileUtils.forceMkdir(file.getParentFile());
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        try {
            Writer w = new OutputStreamWriter(out, "UTF-8");
            w.write(url.toString());
            w.write('\n');
            DateTime now = new DateTime();
            w.write(DTF.print(now));
            w.write('\n');
            for (Header h : headers) {
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

