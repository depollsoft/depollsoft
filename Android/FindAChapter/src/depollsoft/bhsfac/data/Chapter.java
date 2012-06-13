package depollsoft.bhsfac.data;

import depollsoft.lib.binding.TrackableField;

public class Chapter {
  private TrackableField<Integer> id = new TrackableField<Integer>(0);

  private TrackableField<String> chapterName = new TrackableField<String>();

  private TrackableField<String> chorusName = new TrackableField<String>();

  private TrackableField<String> website = new TrackableField<String>();

  private TrackableField<String> address = new TrackableField<String>();

  private TrackableField<String> phone = new TrackableField<String>();

  private TrackableField<String> contactName = new TrackableField<String>();

  private TrackableField<String> contactEmail = new TrackableField<String>();

  private TrackableField<Integer> distance = new TrackableField<Integer>(Integer.MAX_VALUE);

  private TrackableField<String> nextMeetingDate = new TrackableField<String>();

  public String getAddress() {
    return this.address.getValue();
  }

  public String getChapterName() {
    return this.chapterName.getValue();
  }

  public String getChorusName() {
    return this.chorusName.getValue();
  }

  public String getContactEmail() {
    return this.contactEmail.getValue();
  }

  public String getContactName() {
    return this.contactName.getValue();
  }

  public int getDistance() {
    return this.distance.getValue();
  }

  public int getId() {
    return this.id.getValue();
  }

  public String getNextMeetingDate() {
    return this.nextMeetingDate.getValue();
  }

  public String getPhone() {
    return this.phone.getValue();
  }

  public String getWebsite() {
    return this.website.getValue();
  }

  public void setAddress(String address) {
    this.address.setValue(address);
  }

  public void setChapterName(String chapterName) {
    this.chapterName.setValue(chapterName);
  }

  public void setChorusName(String chorusName) {
    this.chorusName.setValue(chorusName);
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail.setValue(contactEmail);
  }

  public void setContactName(String contactName) {
    this.contactName.setValue(contactName);
  }

  public void setDistance(int distance) {
    this.distance.setValue(distance);
  }

  public void setId(int id) {
    this.id.setValue(id);
  }

  public void setNextMeetingDate(String nextMeetingDate) {
    this.nextMeetingDate.setValue(nextMeetingDate);
  }

  public void setPhone(String phone) {
    this.phone.setValue(phone);
  }

  public void setWebsite(String website) {
    this.website.setValue(website);
  }
}
