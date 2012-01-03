package depollsoft.lib.xml;

import java.util.ArrayList;
import java.util.List;

import depollsoft.lib.binding.TrackableField;

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
    return this.attributes.getValue();
  }

  public List<XmlElement> getElements() {
    return this.elements.getValue();
  }

  public String getName() {
    return this.name.getValue();
  }

  public String getValue() {
    return this.value.getValue();
  }

  private void setAttributes(List<XmlAttribute> value) {
    this.attributes.setValue(value);
  }

  private void setElements(List<XmlElement> value) {
    this.elements.setValue(value);
  }

  public void setName(String value) {
    this.name.setValue(value);
  }

  public void setValue(String value) {
    this.value.setValue(value);
  }

  @Override
  public String toString() {
    return "" + this.getName() + " -> " + this.getValue();
  }
}
