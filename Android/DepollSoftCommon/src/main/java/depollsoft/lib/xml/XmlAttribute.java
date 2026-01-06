package depollsoft.lib.xml;

import com.bindroid.trackable.TrackableField;

public class XmlAttribute {

  private TrackableField<String> name = new TrackableField<String>();

  private TrackableField<String> value = new TrackableField<String>();

  public String getName() {
    return this.name.get();
  }

  public String getValue() {
    return this.value.get();
  }

  public void setName(String value) {
    this.name.set(value);
  }

  public void setValue(String value) {
    this.value.set(value);
  }

  @Override
  public String toString() {
    return "" + this.getName() + " -> " + this.getValue();
  }
}
