package depollsoft.tagmaster.barbershop;

import com.bindroid.trackable.TrackableField;

public class RemoteLocation {

  private TrackableField<String> uri = new TrackableField<String>();

  private TrackableField<String> type = new TrackableField<String>();

  public String getType() {
    return this.type.get();
  }

  public String getUri() {
    return this.uri.get();
  }

  public void setType(String value) {
    this.type.set(value);
  }

  public void setUri(String value) {
    this.uri.set(value);
  }
}
