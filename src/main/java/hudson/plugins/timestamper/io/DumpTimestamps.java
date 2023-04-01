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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.input.CountingInputStream;

/**
 * Debugging tool which outputs the contents of a timestamper directory.
 *
 * @author Steven G. Brown
 */
public final class DumpTimestamps {

    /**
     * Read the values from the timestamper directory path given by the command-line arguments and
     * output these values to the console. This is intended only for debugging. It is not invoked by
     * Jenkins.
     *
     * @param args the command-line arguments, expected to contain a timestamper directory path
     */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("no command-line arguments");
        }
        Path timestamperDir = Paths.get(args[0]);
        dump(timestamperDir, "timestamps", 1);
        System.out.println();
        dump(timestamperDir, "timeshifts", 2);
    }

    private static void dump(Path parent, String filename, int columns) throws IOException {
        System.out.println(filename);
        Path file = parent.resolve(filename);
        if (!Files.isRegularFile(file)) {
            System.out.println("(none)");
            return;
        }
        byte[] fileContents = Files.readAllBytes(file);
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(fileContents));
        List<String> values = new ArrayList<>();
        while (inputStream.getCount() < fileContents.length) {
            values.add(Long.toString(Varint.read(inputStream)));
            if (values.size() == columns) {
                System.out.println(String.join("\t", values));
                values.clear();
            }
        }
        if (!values.isEmpty()) {
            System.out.println(String.join("\t", values));
        }
    }

    private DumpTimestamps() {}
}
