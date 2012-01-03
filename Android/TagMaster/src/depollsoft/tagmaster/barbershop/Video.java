package depollsoft.tagmaster.barbershop;

import java.util.Date;

import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.xml.XmlElement;

public class Video {

  private TrackableField<Integer> id = new TrackableField<Integer>();

  private TrackableField<String> description = new TrackableField<String>();

  private TrackableField<String> sungKey = new TrackableField<String>();

  private TrackableField<Boolean> isMultitrack = new TrackableField<Boolean>();

  private TrackableField<String> youTubeCode = new TrackableField<String>();

  private TrackableField<String> sungBy = new TrackableField<String>();

  private TrackableField<String> sungWebsite = new TrackableField<String>();

  private TrackableField<Date> posted = new TrackableField<Date>();

  public String getDescription() {
    return this.description.getValue();
  }

  public int getId() {
    return this.id.getValue();
  }

  public boolean getIsMultitrack() {
    return this.isMultitrack.getValue();
  }

  public Date getPosted() {
    return this.posted.getValue();
  }

  public String getSungBy() {
    return this.sungBy.getValue();
  }

  public String getSungKey() {
    return this.sungKey.getValue();
  }

  public String getSungWebsite() {
    return this.sungWebsite.getValue();
  }

  public String getYouTubeCode() {
    return this.youTubeCode.getValue();
  }

  public void parseFromXml(XmlElement elem) {
    for (XmlElement property : elem.getElements()) {
      String propValue = property.getValue();
      if (propValue == null || propValue.trim().length() == 0)
        propValue = null;
      if (propValue != null)
        propValue = propValue.trim();
      if (property.getName().equals("id"))
        this.setId(Integer.parseInt(propValue));
      else if (property.getName().equals("Desc"))
        this.setDescription(propValue);
      else if (property.getName().equals("SungKey"))
        this.setSungKey(propValue);
      else if (property.getName().equals("Multitrack"))
        this.setIsMultitrack("Yes".equals(propValue));
      else if (property.getName().equals("Code"))
        this.setYouTubeCode(propValue);
      else if (property.getName().equals("SungBy"))
        this.setSungBy(propValue);
      else if (property.getName().equals("SungWebsite"))
        this.setSungWebsite(propValue);
      else if (property.getName().equals("Posted"))
        this.setPosted(new Date(propValue));
    }
  }

  public void setDescription(String value) {
    this.description.setValue(value);
  }

  public void setId(int value) {
    this.id.setValue(value);
  }

  public void setIsMultitrack(boolean value) {
    this.isMultitrack.setValue(value);
  }

  public void setPosted(Date value) {
    this.posted.setValue(value);
  }

  public void setSungBy(String value) {
    this.sungBy.setValue(value);
  }

  public void setSungKey(String value) {
    this.sungKey.setValue(value);
  }

  public void setSungWebsite(String value) {
    this.sungWebsite.setValue(value);
  }

  public void setYouTubeCode(String value) {
    this.youTubeCode.setValue(value);
  }
}
