package depollsoft.tagmaster.barbershop;

import com.bindroid.trackable.TrackableField;
import com.bindroid.utils.ObjectUtilities;

public class Track {

  private TrackableField<String> title = new TrackableField<String>();

  private TrackableField<RemoteLocation> source = new TrackableField<RemoteLocation>();

  public Track() {
  }

  public Track(String title, RemoteLocation source) {
    this.setTitle(title);
    this.setSource(source);
  }

  public boolean Equals(Object obj) {
    if (obj == null)
      return false;
    Track track = (Track) obj;
    return ObjectUtilities.equals(this.getTitle(), track.getTitle())
        && ObjectUtilities.equals(this.getSource(), track.getSource());
  }

  public RemoteLocation getSource() {
    return this.source.get();
  }

  public String getTitle() {
    return this.title.get();
  }

  public void setSource(RemoteLocation value) {
    this.source.set(value);
  }

  public void setTitle(String value) {
    this.title.set(value);
  }

  @Override
  public String toString() {
    return this.getTitle();
  }
}
