package depollsoft.pitchperfect.converters;

import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.SubscriptSpan;
import depollsoft.lib.binding.ValueConverter;
import depollsoft.lib.ui.CustomTypefaceSpan;
import depollsoft.lib.ui.SpannableUtilities;
import depollsoft.pitchperfect.CommonModel;
import depollsoft.pitchperfect.lib.Note;

public class NoteListNoteTextConverter extends ValueConverter {

  @Override
  public Object convertToTarget(Object sourceValue, Class<?> targetType) {
    Note note = (Note) sourceValue;
    SpannableStringBuilder res = new SpannableStringBuilder();
    res.append(note.getFriendlyName());
    switch (note.getAccidental()) {
    case Natural:
      break;
    case Flat:
      res.append(CommonModel.flatString);
      SpannableUtilities.applyToLastChar(res, new CustomTypefaceSpan(
          "NoteHedz", CommonModel.getNoteHedz()));
      SpannableUtilities.applyToLastChar(res, new RelativeSizeSpan(1.2f));
      break;
    case Sharp:
      res.append(CommonModel.sharpString);
      SpannableUtilities.applyToLastChar(res, new CustomTypefaceSpan(
          "NoteHedz", CommonModel.getNoteHedz()));
      SpannableUtilities.applyToLastChar(res, new RelativeSizeSpan(1.2f));
      break;
    }
    res.append("" + note.getOctave());
    SpannableUtilities.applyToLastChar(res, new RelativeSizeSpan(.5f));
    SpannableUtilities.applyToLastChar(res, new SubscriptSpan());
    if (note.getAlternate() != null) {
      note = note.getAlternate();
      res.append(" / ");
      res.append(note.getFriendlyName());
      switch (note.getAccidental()) {
      case Natural:
        break;
      case Flat:
        res.append(CommonModel.flatString);
        SpannableUtilities.applyToLastChar(res, new CustomTypefaceSpan(
            "NoteHedz", CommonModel.getNoteHedz()));
        SpannableUtilities.applyToLastChar(res, new RelativeSizeSpan(1.2f));
        break;
      case Sharp:
        res.append(CommonModel.sharpString);
        SpannableUtilities.applyToLastChar(res, new CustomTypefaceSpan(
            "NoteHedz", CommonModel.getNoteHedz()));
        SpannableUtilities.applyToLastChar(res, new RelativeSizeSpan(1.2f));
        break;
      }
      res.append("" + note.getOctave());
      SpannableUtilities.applyToLastChar(res, new RelativeSizeSpan(.5f));
      SpannableUtilities.applyToLastChar(res, new SubscriptSpan());
    }
    return res;
  }

}
