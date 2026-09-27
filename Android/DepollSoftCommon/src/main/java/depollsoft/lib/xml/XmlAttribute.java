package depollsoft.lib.xml;


public class XmlAttribute {

  private String name;

  private String value;

  public String getName() {
    return this.name;
  }

  public String getValue() {
    return this.value;
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
