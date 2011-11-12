package depollsoft.pitchperfect.converters;

import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import depollsoft.lib.binding.ValueConverter;
import depollsoft.lib.ui.CustomTypefaceSpan;
import depollsoft.lib.ui.SpannableUtilities;
import depollsoft.pitchperfect.CommonModel;
import depollsoft.pitchperfect.lib.Key;

public class KeyNameConverter extends ValueConverter
{

   @Override
   public Object convertToTarget(Object sourceValue, Class<?> targetType)
   {
      Key k = (Key) sourceValue;
      SpannableStringBuilder res = new SpannableStringBuilder();
      res.append(k.getFriendlyName());
      switch (k.getAccidental())
      {
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
      return res;
   }

}
