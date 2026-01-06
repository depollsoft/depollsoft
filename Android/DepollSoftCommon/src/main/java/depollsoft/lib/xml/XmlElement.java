package depollsoft.lib.xml;

import java.util.ArrayList;
import java.util.List;

import com.bindroid.trackable.TrackableField;

public class XmlElement {
  private TrackableField<String> name = new TrackableField<String>();

  private TrackableField<List<XmlElement>> elements = new TrackableField<List<XmlElement>>();

  private TrackableField<List<XmlAttribute>> attributes = new TrackableField<List<XmlAttribute>>();

  private TrackableField<String> value = new TrackableField<String>();

  public XmlElement() {
    this.setElements(new ArrayList<XmlElement>());
    this.setAttributes(new ArrayList<XmlAttribute>());
  }

  public XmlAttribute attribute(String key) {
    for (XmlAttribute attribute : this.getAttributes())
      if (attribute.getName().equals(key))
        return attribute;
    return null;
  }

  public List<XmlElement> elements(String name) {
    ArrayList<XmlElement> results = new ArrayList<XmlElement>();
    for (XmlElement elem : this.getElements())
      if (elem.getName().equals(name))
        results.add(elem);
    return results;
  }

  public List<XmlAttribute> getAttributes() {
    return this.attributes.get();
  }

  public List<XmlElement> getElements() {
    return this.elements.get();
  }

  public String getName() {
    return this.name.get();
  }

  public String getValue() {
    return this.value.get();
  }

  private void setAttributes(List<XmlAttribute> value) {
    this.attributes.set(value);
  }

  private void setElements(List<XmlElement> value) {
    this.elements.set(value);
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
