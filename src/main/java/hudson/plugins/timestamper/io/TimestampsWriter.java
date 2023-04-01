/*
 * The MIT License
 *
 * Copyright (c) 2013 Steven G. Brown
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.timestamper.io;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Run;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Write the time-stamps for a build to disk.
 *
 * @author Steven G. Brown
 */
public class TimestampsWriter implements Closeable {

    private static final int BUFFER_SIZE = 1024;

    private final Path timestampsFile;

    private final Optional<MessageDigest> timestampsDigest;

    @CheckForNull
    private OutputStream timestampsOutput;

    /** Buffer that is used to store Varints prior to writing to a file. */
    private final byte[] buffer = new byte[BUFFER_SIZE];

    private long previousCurrentTimeMillis;

    /** Create a time-stamps writer for the given build. */
    public TimestampsWriter(Run<?, ?> build) throws IOException {
        this(build, Optional.empty());
    }

    /** Create a time-stamps writer for the given build. */
    public TimestampsWriter(Run<?, ?> build, Optional<MessageDigest> digest) throws IOException {
        this(TimestamperPaths.timestampsFile(build), build.getStartTimeInMillis(), digest);
    }

    public TimestampsWriter(Path timestampsFile, long buildStartTime, Optional<MessageDigest> digest)
            throws IOException {
        this.timestampsFile = timestampsFile;
        this.previousCurrentTimeMillis = buildStartTime;
        this.timestampsDigest = Objects.requireNonNull(digest);

        Path parentDir = timestampsFile.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        Files.createFile(timestampsFile);
    }

    /**
     * Write a time-stamp for a line of the console log.
     *
     * @param currentTimeMillis {@link System#currentTimeMillis()}
     * @param times the number of times to write the time-stamp
     */
    public void write(long currentTimeMillis, int times) throws IOException {
        if (times < 1) {
            return;
        }
        long elapsedMillis = currentTimeMillis - previousCurrentTimeMillis;
        previousCurrentTimeMillis = currentTimeMillis;

        // Write to the time-stamps file.
        if (timestampsOutput == null) {
            timestampsOutput = openTimestampsStream();
        }
        writeVarintsTo(timestampsOutput, elapsedMillis);
        if (times > 1) {
            writeZerosTo(timestampsOutput, times - 1);
        }
    }

    /**
     * Open an output stream for writing to the time-stamps file.
     *
     * @return the output stream
     */
    private OutputStream openTimestampsStream() throws IOException {
        OutputStream outputStream = Files.newOutputStream(timestampsFile);
        if (timestampsDigest.isPresent()) {
            outputStream = new DigestOutputStream(outputStream, timestampsDigest.get());
        }
        return outputStream;
    }

    /** Write each value to the given output stream as a Base 128 Varint. */
    private void writeVarintsTo(OutputStream outputStream, long... values) throws IOException {
        int offset = 0;
        for (long value : values) {
            offset = Varint.write(value, buffer, offset);
        }
        outputStream.write(buffer, 0, offset);
        outputStream.flush();
    }

    /** Write n bytes of 0 to the given output stream. */
    private void writeZerosTo(OutputStream outputStream, int n) throws IOException {
        Arrays.fill(buffer, (byte) 0);
        while (n > 0) {
            int bytesToWrite = Math.min(n, buffer.length);
            n -= bytesToWrite;
            outputStream.write(buffer, 0, bytesToWrite);
            outputStream.flush();
        }
    }

    /** Write a time-stamps digest file for the build. */
    public void writeDigest() throws IOException {
        if (timestampsDigest.isPresent()) {
            writeDigest(timestampsDigest.get());
        }
    }

    private void writeDigest(MessageDigest timestampsDigest) throws IOException {
        StringBuilder hash = new StringBuilder();
        for (byte b : timestampsDigest.digest()) {
            hash.append(String.format("%02x", b));
        }
        hash.append("\n");
        Path digestFile =
                timestampsFile.resolveSibling(timestampsFile.getFileName() + "." + timestampsDigest.getAlgorithm());
        Files.write(digestFile, hash.toString().getBytes(StandardCharsets.US_ASCII));
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        if (timestampsOutput != null) {
            timestampsOutput.close();
        }
    }
}
