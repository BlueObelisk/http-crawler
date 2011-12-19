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

package uk.ac.cam.ch.wwmm.httpcrawler.cache;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sam Adams
 */
public abstract class AbstractHttpCache implements HttpCache {

    public static final DateTimeFormatter DTF =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z")
                    .withZone(DateTimeZone.UTC);


    protected static List<Header> readHeaders(final InputStream r) throws IOException {
        final List<Header> list = new ArrayList<Header>();
        for (String line = readLine(r); line.length() > 0; line = readLine(r)) {
            final int i = line.indexOf(": ");
            final Header h = new BasicHeader(line.substring(0, i), line.substring(i+2));
            list.add(h);
        }
        return list;
    }

    protected static String readLine(final InputStream in) throws IOException {
        final StringBuilder s = new StringBuilder();
        for (int c = readUtf8(in); c != -1 && c != '\n'; c = readUtf8(in)) {
            s.append((char)c);
        }
        return s.toString();
    }

    protected static String readUtf8String(final InputStream in) throws IOException {
        return IOUtils.toString(in, "UTF-8");
    }

    protected static char readUtf8(final InputStream in) throws IOException {
        final int c = in.read();
        if (c == -1) {
            return (char) -1;
        }
        final int n = getUtf8Bytes(c);
        if (n == 1) {
            return (char) c;
        }
        return readUtf8(in, n);
    }

    protected static char readUtf8(final InputStream in, final int n) throws IOException {
        int c = 0;
        for (int i = 1; i < n; i++) {
            final int x = in.read();
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
    protected static int getUtf8Bytes(final int i0) throws IOException {
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
