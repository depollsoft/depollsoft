package depollsoft.pitchperfect.converters;

import android.content.Context;
import android.text.Spannable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import depollsoft.lib.activity.RichApplication;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PitchPipeNoteTextConverterTest {
    @Test
    public void converts_sharp_flat_and_natural() throws Exception {
        Context app = RuntimeEnvironment.getApplication();
        // Seed RichApplication context and pre-seed CommonModel font to avoid asset load
        java.lang.reflect.Field f = RichApplication.class.getDeclaredField("context");
        f.setAccessible(true);
        f.set(null, app);
        java.lang.reflect.Field fontField = depollsoft.pitchperfect.CommonModel.class.getDeclaredField("noteHedz");
        fontField.setAccessible(true);
        fontField.set(null, android.graphics.Typeface.DEFAULT);

        PitchPipeNoteTextConverter conv = new PitchPipeNoteTextConverter();

        Note sharp = Note.findNote("A", Accidental.Sharp, 4);
        Object outSharp = conv.convertToTarget(sharp, CharSequence.class);
        assertTrue(outSharp instanceof Spannable);

        Note nat = Note.findNote("C", Accidental.Natural, 4);
        Object outNat = conv.convertToTarget(nat, CharSequence.class);
        assertEquals("C", String.valueOf(outNat));
    }
}
