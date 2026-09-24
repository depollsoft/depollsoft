package depollsoft.tagmaster.barbershop;

import java.util.Objects;

import depollsoft.lib.state.StateField;

public class Track {

  private StateField<String> title = new StateField<>(null);

  private StateField<RemoteLocation> source = new StateField<>(null);

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
