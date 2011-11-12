package depollsoft.pitchperfect.converters;

import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import depollsoft.lib.binding.ValueConverter;
import depollsoft.lib.ui.CustomTypefaceSpan;
import depollsoft.lib.ui.SpannableUtilities;
import depollsoft.pitchperfect.CommonModel;
import depollsoft.pitchperfect.lib.Key;

public class KeySignatureConverter extends ValueConverter
{

   @Override
   public Object convertToTarget(Object sourceValue, Class<?> targetType)
   {
      SpannableStringBuilder res = new SpannableStringBuilder();
      Key k = (Key) sourceValue;
      res.append('&');
      SpannableUtilities.applyToLastChar(res, new CustomTypefaceSpan(
            "MusiQwik", CommonModel.getMusiQwik()));
      if (k.getNumAccidentals() > 0)
      {
         char sharps = (char) ('Á' + k.getNumAccidentals() - 1);
         res.append(sharps);
         SpannableUtilities.applyToLastChar(res, new CustomTypefaceSpan(
               "MusiQwik", CommonModel.getMusiQwik()));
      }
      else if (k.getNumAccidentals() < 0)
      {
         char flats;
         if (k.getNumAccidentals() != -6)
            flats = (char) ('¬' - k.getNumAccidentals() - 1);
         else
            flats = 'Û';
         res.append(flats);
         SpannableUtilities.applyToLastChar(res, new CustomTypefaceSpan(
               "MusiQwik", CommonModel.getMusiQwik()));
      }
      SpannableUtilities.applyToAll(res, new RelativeSizeSpan(1.7f));
      return res;
   }

}
