package depollsoft.tagmaster.barbershop;

import java.util.Date;

import com.bindroid.trackable.TrackableField;

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
    return this.description.get();
  }

  public int getId() {
    return this.id.get();
  }

  public boolean getIsMultitrack() {
    return this.isMultitrack.get();
  }

  public Date getPosted() {
    return this.posted.get();
  }

  public String getSungBy() {
    return this.sungBy.get();
  }

  public String getSungKey() {
    return this.sungKey.get();
  }

  public String getSungWebsite() {
    return this.sungWebsite.get();
  }

  public String getYouTubeCode() {
    return this.youTubeCode.get();
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
    this.description.set(value);
  }

  public void setId(int value) {
    this.id.set(value);
  }

  public void setIsMultitrack(boolean value) {
    this.isMultitrack.set(value);
  }

  public void setPosted(Date value) {
    this.posted.set(value);
  }

  public void setSungBy(String value) {
    this.sungBy.set(value);
  }

  public void setSungKey(String value) {
    this.sungKey.set(value);
  }

  public void setSungWebsite(String value) {
    this.sungWebsite.set(value);
  }

  public void setYouTubeCode(String value) {
    this.youTubeCode.set(value);
  }
}
