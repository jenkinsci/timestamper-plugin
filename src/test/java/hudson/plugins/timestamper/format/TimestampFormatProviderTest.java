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
package hudson.plugins.timestamper.format;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Unit test for the {@link TimestampFormatProvider} class.
 *
 * @author Steven G. Brown
 */
class TimestampFormatProviderTest {

    private static final long HALF_HOUR = TimeUnit.MINUTES.toMillis(30);

    private static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1);

    private static final String SYSTEM_TIME_FORMAT = "HH:mm:ss";

    private static final String ELAPSED_TIME_FORMAT = "ss.S";

    /** @return parameterised test data */
    static Stream<Object[]> data() {
        return Stream.of(
                // system
                new Object[] {request("jenkins-timestamper=system"), system()},
                // local (system with browser time zone)
                new Object[] {request("jenkins-timestamper-local=true", "jenkins-timestamper-offset=0"), system("GMT")},
                new Object[] {
                    request("jenkins-timestamper-local=true", "jenkins-timestamper-offset=" + HALF_HOUR),
                    system("GMT-0:30")
                },
                new Object[] {
                    request("jenkins-timestamper-local=true", "jenkins-timestamper-offset=" + ONE_HOUR), system("GMT-1")
                },
                new Object[] {
                    request("jenkins-timestamper-local=true", "jenkins-timestamper-offset=-" + HALF_HOUR),
                    system("GMT+0:30")
                },
                new Object[] {
                    request("jenkins-timestamper-local=true", "jenkins-timestamper-offset=-" + ONE_HOUR),
                    system("GMT+1")
                },
                // elapsed
                new Object[] {request("jenkins-timestamper=elapsed"), elapsed()},
                // none
                new Object[] {request("jenkins-timestamper=none"), empty()},
                // other
                new Object[] {request(), system()},
                new Object[] {request((String[]) null), system()});
    }

    private static StaplerRequest2 request(String... cookies) {
        StaplerRequest2 request = mock(StaplerRequest2.class);
        Cookie[] requestCookies = null;
        if (cookies != null) {
            requestCookies = new Cookie[cookies.length];
            for (int i = 0; i < cookies.length; i++) {
                String[] nameAndValue = cookies[i].split(Pattern.quote("="), 2);
                requestCookies[i] = new Cookie(nameAndValue[0], nameAndValue[1]);
            }
        }
        when(request.getCookies()).thenReturn(requestCookies);
        when(request.toString()).thenReturn(StaplerRequest2.class.getSimpleName() + " " + Arrays.toString(cookies));
        return request;
    }

    private static SystemTimestampFormat system() {
        return new SystemTimestampFormat(SYSTEM_TIME_FORMAT, Optional.empty(), Locale.ENGLISH);
    }

    private static SystemTimestampFormat system(String timeZoneId) {
        return new SystemTimestampFormat(SYSTEM_TIME_FORMAT, Optional.of(timeZoneId), Locale.ENGLISH);
    }

    private static ElapsedTimestampFormat elapsed() {
        return new ElapsedTimestampFormat(ELAPSED_TIME_FORMAT);
    }

    private static EmptyTimestampFormat empty() {
        return EmptyTimestampFormat.INSTANCE;
    }

    @ParameterizedTest
    @MethodSource("data")
    void testGet(StaplerRequest2 request, TimestampFormat expectedTimestampFormat) {
        assertThat(
                TimestampFormatProvider.get(SYSTEM_TIME_FORMAT, ELAPSED_TIME_FORMAT, request, Locale.ENGLISH),
                is(expectedTimestampFormat));
    }
}
