package depollsoft.tagmaster.barbershop;

import java.util.Objects;

public class Track {

  private String title;

  private RemoteLocation source;

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
    return Objects.equals(this.getTitle(), track.getTitle())
        && Objects.equals(this.getSource(), track.getSource());
  }

  public RemoteLocation getSource() {
    return this.source;
  }

  public String getTitle() {
    return this.title;
  }

  public void setSource(RemoteLocation value) {
    this.source = value;
  }

  public void setTitle(String value) {
    this.title = value;
  }

  @Override
  public String toString() {
    return this.getTitle();
  }
}
