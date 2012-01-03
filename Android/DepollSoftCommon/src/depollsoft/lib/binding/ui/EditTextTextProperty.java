package depollsoft.lib.binding.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import depollsoft.lib.binding.Trackable;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.Function;
import depollsoft.lib.util.ObjectUtilities;
import depollsoft.lib.util.Property;

public class EditTextTextProperty extends Property<String> {
  private EditText target;
  private Trackable notifier = new Trackable();

  public EditTextTextProperty(EditText target) {
    this.target = target;
    this.propertyType = String.class;
    target.addTextChangedListener(new TextWatcher() {

      public void afterTextChanged(Editable s) {
        EditTextTextProperty.this.notifier.updateTrackers();
      }

      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {
      }

      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }
    });
    this.getter = new Function<String>() {
      public String evaluate() {
        EditTextTextProperty.this.notifier.track();
        return EditTextTextProperty.this.target.getText().toString();
      }
    };
    this.setter = new Action<String>() {
      public void invoke(String parameter) {
        if (!ObjectUtilities.equals(parameter, EditTextTextProperty.this.target
            .getText().toString()))
          EditTextTextProperty.this.target.setText(parameter);
      }

    };
  }
}
