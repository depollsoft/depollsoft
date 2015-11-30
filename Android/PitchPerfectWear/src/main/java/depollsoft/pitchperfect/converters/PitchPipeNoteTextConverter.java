package depollsoft.pitchperfect.converters;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.UnderlineSpan;

import com.bindroid.ValueConverter;

import depollsoft.lib.ui.CustomTypefaceSpan;
import depollsoft.pitchperfect.CommonModel;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

public class PitchPipeNoteTextConverter extends ValueConverter {

  private static Spannable sharpFlat;

  static {
    PitchPipeNoteTextConverter.sharpFlat = new SpannableStringBuilder(
            CommonModel.sharpString + "/" + CommonModel.flatString);
    PitchPipeNoteTextConverter.sharpFlat.setSpan(new CustomTypefaceSpan(
                    "NoteHedz", CommonModel.getNoteHedz()), 0, 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    PitchPipeNoteTextConverter.sharpFlat.setSpan(new RelativeSizeSpan(1.2f), 0,
            1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    PitchPipeNoteTextConverter.sharpFlat.setSpan(new CustomTypefaceSpan(
                    "NoteHedz", CommonModel.getNoteHedz()),
            PitchPipeNoteTextConverter.sharpFlat.length() - 1,
            PitchPipeNoteTextConverter.sharpFlat.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    PitchPipeNoteTextConverter.sharpFlat.setSpan(new RelativeSizeSpan(1.2f),
            PitchPipeNoteTextConverter.sharpFlat.length() - 1,
            PitchPipeNoteTextConverter.sharpFlat.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  @Override
  public Object convertToTarget(Object sourceValue, Class<?> targetType) {
    Note note = (Note) sourceValue;
    if (note.getAccidental() != Accidental.Natural)
      return PitchPipeNoteTextConverter.sharpFlat;
    return note.getFriendlyName();
  }

}
