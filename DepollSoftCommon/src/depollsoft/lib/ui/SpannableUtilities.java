package depollsoft.lib.ui;

import android.text.Spannable;
import android.text.Spanned;

public class SpannableUtilities
{
   public static void applyToAll(Spannable s, Object what)
   {
      s.setSpan(what, 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
   }

   public static void applyToLastChar(Spannable s, Object what)
   {
      s.setSpan(what, s.length() - 1, s.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
   }
}
