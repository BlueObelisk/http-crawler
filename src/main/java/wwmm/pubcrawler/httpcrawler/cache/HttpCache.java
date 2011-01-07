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
package wwmm.pubcrawler.httpcrawler.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sam Adams
 */
public class HttpCache {

    private static final DateTimeFormatter DTF =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z")
                    .withZone(DateTimeZone.UTC);

    private final File root;


    public HttpCache(File dir) throws IOException {
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


    private List<Header> readHeaders(InputStream r) throws IOException {
        List<Header> list = new ArrayList<Header>();
        for (String line = readLine(r); line.length() > 0; line = readLine(r)) {
            int i = line.indexOf(": ");
            Header h = new BasicHeader(line.substring(i), line.substring(i+2));
            list.add(h);
        }
        return list;
    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder s = new StringBuilder();
        for (int c = readUtf8(in); c != -1 && c != '\n'; c = readUtf8(in)) {
            s.append((char)c);
        }
        return s.toString();
    }

    private char readUtf8(InputStream in) throws IOException {
        int c = in.read();
        if (c == -1) {
            return (char) -1;
        }
        int n = getUtf8Bytes(c);
        if (n == 1) {
            return (char) c;
        }
        return readUtf8(in, n);
    }

    private char readUtf8(InputStream in, int n) throws IOException {
        int c = 0;
        for (int i = 1; i < n; i++) {
            int x = in.read();
            if (x == -1) {
                throw new EOFException("EOF mid UTF-8 character");
            }
            if ((x & 0xc0) != 0x80) {
                throw new IOException("Bad byte in UTF-8 character: "+Integer.toBinaryString(x));
            }
            c = (c << 6) | x;
        }
        return (char) c;
    }


    /**
     * @see {http://en.wikipedia.org/wiki/UTF-8#Design}
     * @param i0
     * @return
     */
    private int getUtf8Bytes(int i0) throws IOException {
        // 0xxxxxxx
        if ((i0 & 0x80) == 0) {
            return 1;
        }
        // 110xxxxx
        if ((i0 & 0xe0) == 0xc0) {
            return 2;
        }
        // 1110xxxx
        if ((i0 & 0xf0) == 0xe0) {
            return 3;
        }
        // 11110xxx
        if ((i0 & 0xf8) == 0xf0) {
            return 4;
        }
        // 111110xx
        if ((i0 & 0xfc) == 0xf8) {
            return 5;
        }
        // 1111110x
        if ((i0 & 0xfe) == 0xfc) {
            return 6;
        }
        throw new IOException("Bad UTF-8 first character: "+Integer.toBinaryString(i0));
    }

}

