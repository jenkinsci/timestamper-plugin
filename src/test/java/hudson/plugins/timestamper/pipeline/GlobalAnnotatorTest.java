package hudson.plugins.timestamper.pipeline;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Supplier;

import hudson.MarkupText;
import hudson.model.Run;
import hudson.plugins.timestamper.format.ElapsedTimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormatProvider;

/**
 * 
 * @author awitt
 *
 */
public class GlobalAnnotatorTest {
    
    /**
     * {@link GlobalAnnotator} will determine build start time by {@link Run#getStartTimeInMillis()}
     */
    protected Run<?,?> run;
    
    /**
     * The {@link GlobalAnnotator} under test
     */
    protected GlobalAnnotator annotator;
    
    @Before
    public void setUp() {

        run = mock( Run.class );
        
        annotator = new GlobalAnnotator();
        
        Whitebox.setInternalState(TimestampFormatProvider.class, new Supplier<TimestampFormat>() {

            @Override
            public TimestampFormat get() {
                return new ElapsedTimestampFormat( "'<b>'HH:mm:ss.S'</b> '" ); // default Elapsed timestamp format
            }
            
        });
    }

    @Test
    public void testTimestampsWhenNotWrapped() {
        
        // Timestamps should be correctly identified in a "regular" log line.
        
        when( run.getStartTimeInMillis() )
        .thenReturn( 1572023430 * 1000L ); // 2019-10-25T17:10:30.000
        
        String log = "[2019-10-25T17:10:30.374Z] Raw log line";
        
        MarkupText text = new MarkupText( log );
        
        annotator.annotate( run, text );
        
        assertEquals(
            "Timestamps should be correctly identified in a regular log line.",
            "<span class=\"timestamp\"><b>00:00:00.374</b> </span><span style=\"display: none\">[2019-10-25T17:10:30.374Z]</span> Raw log line",
            text.toString( true ) );
    }
    
    @Test
    public void testTimestampsWhenWrapped() {
        
        // Timestamps should be correctly identified in log line wrapped in HTML by some other ConsoleAnnotator
        
        when( run.getStartTimeInMillis() )
        .thenReturn( 1572023430 * 1000L ); // 2019-10-25T17:10:30.000
        
        String log = "[2019-10-25T17:10:30.374Z] Raw log line";
        
        MarkupText text = new MarkupText( log );
        text.wrapBy("<span data-timestamper style='who-knows'>", "</span>"); // as some other ConsoleAnnotator might
        
        annotator.annotate( run, text );
        
        assertEquals(
            "Timestamps should be correctly identified in a log line that another ConsoleAnnotator has wrapped in HTML tags.",
            "<span class=\"timestamp\"><b>00:00:00.374</b> </span><span data-timestamper style='who-knows'><span style=\"display: none\">[2019-10-25T17:10:30.374Z]</span> Raw log line</span>",
            text.toString( true ) );
        
    }
    
    @Test
    public void testTimestampsWhenWrappedWithoutSpecialCase() {
        
        // Timestamps should be incorrectly identified in log line wrapped in HTML by some other ConsoleAnnotator
        // when that annotator doesn't know about the special-case workaround
        
        when( run.getStartTimeInMillis() )
        .thenReturn( 1572023430 * 1000L ); // 2019-10-25T17:10:30.000
        
        String log = "[2019-10-25T17:10:30.374Z] Raw log line";
        
        MarkupText text = new MarkupText( log );
        text.wrapBy("<span style='who-knows'>", "</span>"); // as some other ConsoleAnnotator might
        
        annotator.annotate( run, text );
        
        assertEquals(
            "Timestamp identification will regrettably fail in a log line that another ConsoleAnnotator has wrapped in HTML tags without using the special workaround.",
            "<span style='who-knows'>[2019-10-25T17:10:30.374Z] Raw log line</span>",
            text.toString( true ) );
        
    }
    
    @Test
    public void testTimestampsWhenDoubleWrapped() {
        
        // Timestamps should be incorrectly identified in log line wrapped in HTML by some other ConsoleAnnotator
        // when that annotator doesn't know about the special-case workaround
        
        when( run.getStartTimeInMillis() )
        .thenReturn( 1572023430 * 1000L ); // 2019-10-25T17:10:30.000
        
        String log = "[2019-10-25T17:10:30.374Z] Raw log line";
        
        MarkupText text = new MarkupText( log );
        text.wrapBy("<span data-timestamper style='who-knows'>", "</span>"); // as some other ConsoleAnnotator might
        text.wrapBy("<span data-timestamper style='something-else'>", "</span>"); // as some third ConsoleAnnotator might
        
        annotator.annotate( run, text );
        
        assertEquals(
            "Timestamp identification will regrettably fail in a log line wrapped by two other ConsoleAnnotators, both of whom tried to use this workaround.",
            "<span data-timestamper style='who-knows'><span data-timestamper style='something-else'>[2019-10-25T17:10:30.374Z] Raw log line</span></span>",
            text.toString( true ) );
        
    }
    
    @Test
    public void testTimestampsWhenHTMLIsPartOfTheLog() {
        
        // Timestamps should be correctly detected when HTML is part of the actual log output.
        
        when( run.getStartTimeInMillis() )
        .thenReturn( 1572023430 * 1000L ); // 2019-10-25T17:10:30.000
        
        String log = "[2019-10-25T17:10:30.374Z] <span style='who-knows'>content</span> remainder";
        
        MarkupText text = new MarkupText( log );
        
        annotator.annotate( run, text );
        
        assertEquals(
            "Timestamps should be correctly identified when the actual log line contains HTML tags.",
            "<span class=\"timestamp\"><b>00:00:00.374</b> </span><span style=\"display: none\">[2019-10-25T17:10:30.374Z]</span> &lt;span style='who-knows'&gt;content&lt;/span&gt; remainder",
            text.toString( true ) );
        
    }

}
