package depollsoft.lib.ui;

import static org.junit.Assert.*;

import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for the SpannableUtilities class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
public class SpannableUtilitiesTest {

    // =====================================================================
    // applyToAll tests
    // =====================================================================

    @Test
    public void applyToAll_appliesSpanToEntireString() {
        SpannableString spannable = new SpannableString("Hello World");
        UnderlineSpan span = new UnderlineSpan();
        
        SpannableUtilities.applyToAll(spannable, span);
        
        UnderlineSpan[] spans = spannable.getSpans(0, spannable.length(), UnderlineSpan.class);
        assertEquals(1, spans.length);
        assertEquals(0, spannable.getSpanStart(spans[0]));
        assertEquals(spannable.length(), spannable.getSpanEnd(spans[0]));
    }

    @Test
    public void applyToAll_withEmptyString_doesNotCrash() {
        SpannableString spannable = new SpannableString("");
        UnderlineSpan span = new UnderlineSpan();
        
        SpannableUtilities.applyToAll(spannable, span);
        
        UnderlineSpan[] spans = spannable.getSpans(0, spannable.length(), UnderlineSpan.class);
        assertEquals(1, spans.length);
    }

    @Test
    public void applyToAll_withSingleCharacter_appliesSpan() {
        SpannableString spannable = new SpannableString("X");
        StyleSpan span = new StyleSpan(android.graphics.Typeface.BOLD);
        
        SpannableUtilities.applyToAll(spannable, span);
        
        StyleSpan[] spans = spannable.getSpans(0, spannable.length(), StyleSpan.class);
        assertEquals(1, spans.length);
        assertEquals(0, spannable.getSpanStart(spans[0]));
        assertEquals(1, spannable.getSpanEnd(spans[0]));
    }

    @Test
    public void applyToAll_setsExclusiveFlags() {
        SpannableString spannable = new SpannableString("Test");
        ForegroundColorSpan span = new ForegroundColorSpan(0xFF0000);
        
        SpannableUtilities.applyToAll(spannable, span);
        
        int flags = spannable.getSpanFlags(span);
        assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, flags);
    }

    @Test
    public void applyToAll_withMultipleSpans_appliesAll() {
        SpannableString spannable = new SpannableString("Multiple Spans");
        UnderlineSpan underline = new UnderlineSpan();
        StyleSpan bold = new StyleSpan(android.graphics.Typeface.BOLD);
        ForegroundColorSpan color = new ForegroundColorSpan(0x00FF00);
        
        SpannableUtilities.applyToAll(spannable, underline);
        SpannableUtilities.applyToAll(spannable, bold);
        SpannableUtilities.applyToAll(spannable, color);
        
        assertEquals(1, spannable.getSpans(0, spannable.length(), UnderlineSpan.class).length);
        assertEquals(1, spannable.getSpans(0, spannable.length(), StyleSpan.class).length);
        assertEquals(1, spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class).length);
    }

    @Test
    public void applyToAll_coversEntireLength() {
        String text = "This is a longer piece of text for testing";
        SpannableString spannable = new SpannableString(text);
        BackgroundColorSpan span = new BackgroundColorSpan(0xFFFFFF);
        
        SpannableUtilities.applyToAll(spannable, span);
        
        assertEquals(0, spannable.getSpanStart(span));
        assertEquals(text.length(), spannable.getSpanEnd(span));
    }

    @Test
    public void applyToAll_withUnicodeText_worksCorrectly() {
        SpannableString spannable = new SpannableString("日本語テスト");
        UnderlineSpan span = new UnderlineSpan();
        
        SpannableUtilities.applyToAll(spannable, span);
        
        assertEquals(0, spannable.getSpanStart(span));
        assertEquals(spannable.length(), spannable.getSpanEnd(span));
    }

    @Test
    public void applyToAll_withWhitespaceOnlyString_worksCorrectly() {
        SpannableString spannable = new SpannableString("   ");
        UnderlineSpan span = new UnderlineSpan();
        
        SpannableUtilities.applyToAll(spannable, span);
        
        UnderlineSpan[] spans = spannable.getSpans(0, spannable.length(), UnderlineSpan.class);
        assertEquals(1, spans.length);
    }

    // =====================================================================
    // applyToLastChar tests
    // =====================================================================

    @Test
    public void applyToLastChar_appliesSpanToLastCharacter() {
        SpannableString spannable = new SpannableString("Hello");
        UnderlineSpan span = new UnderlineSpan();
        
        SpannableUtilities.applyToLastChar(spannable, span);
        
        assertEquals(4, spannable.getSpanStart(span));
        assertEquals(5, spannable.getSpanEnd(span));
    }

    @Test
    public void applyToLastChar_withSingleCharacter_appliesSpan() {
        SpannableString spannable = new SpannableString("A");
        StyleSpan span = new StyleSpan(android.graphics.Typeface.ITALIC);
        
        SpannableUtilities.applyToLastChar(spannable, span);
        
        assertEquals(0, spannable.getSpanStart(span));
        assertEquals(1, spannable.getSpanEnd(span));
    }

    @Test
    public void applyToLastChar_setsExclusiveFlags() {
        SpannableString spannable = new SpannableString("Test");
        UnderlineSpan span = new UnderlineSpan();
        
        SpannableUtilities.applyToLastChar(spannable, span);
        
        int flags = spannable.getSpanFlags(span);
        assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, flags);
    }

    @Test
    public void applyToLastChar_withLongString_onlyAffectsLast() {
        String text = "This is a very long string with many characters";
        SpannableString spannable = new SpannableString(text);
        ForegroundColorSpan span = new ForegroundColorSpan(0xFF0000);
        
        SpannableUtilities.applyToLastChar(spannable, span);
        
        assertEquals(text.length() - 1, spannable.getSpanStart(span));
        assertEquals(text.length(), spannable.getSpanEnd(span));
    }

    @Test
    public void applyToLastChar_spanLengthIsOne() {
        SpannableString spannable = new SpannableString("Testing");
        BackgroundColorSpan span = new BackgroundColorSpan(0x00FFFF);
        
        SpannableUtilities.applyToLastChar(spannable, span);
        
        int length = spannable.getSpanEnd(span) - spannable.getSpanStart(span);
        assertEquals(1, length);
    }

    @Test
    public void applyToLastChar_withUnicode_worksCorrectly() {
        SpannableString spannable = new SpannableString("Hello 日本");
        UnderlineSpan span = new UnderlineSpan();
        
        SpannableUtilities.applyToLastChar(spannable, span);
        
        assertEquals(spannable.length() - 1, spannable.getSpanStart(span));
        assertEquals(spannable.length(), spannable.getSpanEnd(span));
    }

    @Test
    public void applyToLastChar_multipleSpans_eachTargetsLastChar() {
        SpannableString spannable = new SpannableString("ABCDE");
        UnderlineSpan span1 = new UnderlineSpan();
        StyleSpan span2 = new StyleSpan(android.graphics.Typeface.BOLD);
        
        SpannableUtilities.applyToLastChar(spannable, span1);
        SpannableUtilities.applyToLastChar(spannable, span2);
        
        assertEquals(4, spannable.getSpanStart(span1));
        assertEquals(4, spannable.getSpanStart(span2));
    }

    @Test
    public void applyToLastChar_doesNotAffectOtherCharacters() {
        SpannableString spannable = new SpannableString("ABCDE");
        UnderlineSpan span = new UnderlineSpan();
        
        SpannableUtilities.applyToLastChar(spannable, span);
        
        // Check that the span doesn't cover characters 0-3
        UnderlineSpan[] spansAtStart = spannable.getSpans(0, 1, UnderlineSpan.class);
        assertEquals(0, spansAtStart.length);
        
        UnderlineSpan[] spansAtMiddle = spannable.getSpans(2, 3, UnderlineSpan.class);
        assertEquals(0, spansAtMiddle.length);
    }

    @Test
    public void applyToLastChar_withWhitespaceAtEnd_spansWhitespace() {
        SpannableString spannable = new SpannableString("Hello ");
        UnderlineSpan span = new UnderlineSpan();
        
        SpannableUtilities.applyToLastChar(spannable, span);
        
        // Last char is a space
        assertEquals(5, spannable.getSpanStart(span));
        assertEquals(6, spannable.getSpanEnd(span));
    }

    // =====================================================================
    // Edge case tests
    // =====================================================================

    @Test
    public void applyToAll_thenApplyToLastChar_bothSpansApplied() {
        SpannableString spannable = new SpannableString("Hello");
        UnderlineSpan allSpan = new UnderlineSpan();
        StyleSpan lastSpan = new StyleSpan(android.graphics.Typeface.BOLD);
        
        SpannableUtilities.applyToAll(spannable, allSpan);
        SpannableUtilities.applyToLastChar(spannable, lastSpan);
        
        // allSpan should cover entire string
        assertEquals(0, spannable.getSpanStart(allSpan));
        assertEquals(5, spannable.getSpanEnd(allSpan));
        
        // lastSpan should only cover last character
        assertEquals(4, spannable.getSpanStart(lastSpan));
        assertEquals(5, spannable.getSpanEnd(lastSpan));
    }

    @Test
    public void sameSpanTypeAppliedTwice_bothExist() {
        SpannableString spannable = new SpannableString("Test");
        UnderlineSpan span1 = new UnderlineSpan();
        UnderlineSpan span2 = new UnderlineSpan();
        
        SpannableUtilities.applyToAll(spannable, span1);
        SpannableUtilities.applyToLastChar(spannable, span2);
        
        UnderlineSpan[] allSpans = spannable.getSpans(0, spannable.length(), UnderlineSpan.class);
        assertEquals(2, allSpans.length);
    }
}
