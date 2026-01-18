package depollsoft.pitchperfect.converters;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;

import com.bindroid.ValueConverter;

import depollsoft.lib.ui.CustomTypefaceSpan;
import depollsoft.pitchperfect.CommonModel;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

public class PitchPipeNoteTextConverter extends ValueConverter {

  private static Spannable sharpFlat;

  /**
   * Lazily initializes and returns the sharp/flat spannable.
   * This avoids loading fonts during class initialization which would fail in tests.
   */
  private static Spannable getSharpFlat() {
    if (sharpFlat == null) {
      SpannableStringBuilder builder = new SpannableStringBuilder(
          CommonModel.sharpString + "/" + CommonModel.flatString);
      Typeface noteHedz = CommonModel.getNoteHedz();
      if (noteHedz != null) {
        builder.setSpan(new CustomTypefaceSpan("NoteHedz", noteHedz), 0, 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new RelativeSizeSpan(1.5f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new CustomTypefaceSpan("NoteHedz", noteHedz),
            builder.length() - 1, builder.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new RelativeSizeSpan(1.5f),
            builder.length() - 1, builder.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      sharpFlat = builder;
    }
    return sharpFlat;
  }

  @Override
  public Object convertToTarget(Object sourceValue, Class<?> targetType) {
    Note note = (Note) sourceValue;
    if (note.getAccidental() != Accidental.Natural)
      return getSharpFlat();
    return note.getFriendlyName();
  }

}
