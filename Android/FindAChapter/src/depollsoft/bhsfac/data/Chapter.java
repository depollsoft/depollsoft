package depollsoft.bhsfac.data;

import com.bindroid.trackable.TrackableField;

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
    return this.address.get();
  }

  public String getChapterName() {
    return this.chapterName.get();
  }

  public String getChorusName() {
    return this.chorusName.get();
  }

  public String getContactEmail() {
    return this.contactEmail.get();
  }

  public String getContactName() {
    return this.contactName.get();
  }

  public int getDistance() {
    return this.distance.get();
  }

  public int getId() {
    return this.id.get();
  }

  public String getNextMeetingDate() {
    return this.nextMeetingDate.get();
  }

  public String getPhone() {
    return this.phone.get();
  }

  public String getWebsite() {
    return this.website.get();
  }

  public void setAddress(String address) {
    this.address.set(address);
  }

  public void setChapterName(String chapterName) {
    this.chapterName.set(chapterName);
  }

  public void setChorusName(String chorusName) {
    this.chorusName.set(chorusName);
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail.set(contactEmail);
  }

  public void setContactName(String contactName) {
    this.contactName.set(contactName);
  }

  public void setDistance(int distance) {
    this.distance.set(distance);
  }

  public void setId(int id) {
    this.id.set(id);
  }

  public void setNextMeetingDate(String nextMeetingDate) {
    this.nextMeetingDate.set(nextMeetingDate);
  }

  public void setPhone(String phone) {
    this.phone.set(phone);
  }

  public void setWebsite(String website) {
    this.website.set(website);
  }
}
