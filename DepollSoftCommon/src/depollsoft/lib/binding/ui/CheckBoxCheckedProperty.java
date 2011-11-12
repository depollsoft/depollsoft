package depollsoft.lib.binding.ui;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import depollsoft.lib.binding.Trackable;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.Function;
import depollsoft.lib.util.Property;

public class CheckBoxCheckedProperty extends Property<Boolean>
{
   private Trackable trackable = new Trackable();

   public CheckBoxCheckedProperty(final CheckBox button)
   {
      button.setOnCheckedChangeListener(new OnCheckedChangeListener()
      {
         public void onCheckedChanged(CompoundButton buttonView,
               boolean isChecked)
         {
            CheckBoxCheckedProperty.this.trackable.updateTrackers();
         }
      });
      this.getter = new Function<Boolean>()
      {
         public Boolean evaluate()
         {
            CheckBoxCheckedProperty.this.trackable.track();
            return button.isChecked();
         }
      };
      this.setter = new Action<Boolean>()
      {
         public void invoke(Boolean parameter)
         {
            button.setChecked(parameter);
         }
      };
   }

   @Override
   public Class<?> getType()
   {
      return Boolean.TYPE;
   }
}
