package depollsoft.lib.xml;

import java.util.ArrayList;
import java.util.List;


public class XmlElement {
  private String name;

  private List<XmlElement> elements;

  private List<XmlAttribute> attributes;

  private String value;

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
    return this.attributes;
  }

  public List<XmlElement> getElements() {
    return this.elements;
  }

  public String getName() {
    return this.name;
  }

  public String getValue() {
    return this.value;
  }

  private void setAttributes(List<XmlAttribute> value) {
    this.attributes = value;
  }

  private void setElements(List<XmlElement> value) {
    this.elements = value;
  }

  public void setName(String value) {
    this.name = value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "" + this.getName() + " -> " + this.getValue();
  }
}
