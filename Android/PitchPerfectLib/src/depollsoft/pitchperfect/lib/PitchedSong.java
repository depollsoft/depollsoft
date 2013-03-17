package depollsoft.pitchperfect.lib;

import java.util.UUID;

import com.bindroid.trackable.TrackableField;

public class PitchedSong implements Comparable<PitchedSong> {
  private TrackableField<String> name = new TrackableField<String>();

  private TrackableField<Key> key = new TrackableField<Key>();

  private TrackableField<String> uuid = new TrackableField<String>();

  private TrackableField<Boolean> isPlaying = new TrackableField<Boolean>(false);

  public PitchedSong() {
    this.setId(UUID.randomUUID().toString());
  }

  public int compareTo(PitchedSong another) {
    return this.getName().compareToIgnoreCase(another.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PitchedSong))
      return false;
    return ((PitchedSong) o).getId().equals(this.getId());
  }

  public String getId() {
    return this.uuid.get();
  }

  public boolean getIsPlaying() {
    return this.isPlaying.get() && this.getKey().getNote().getIsPlaying();
  }

  public Key getKey() {
    return this.key.get();
  }

  public String getName() {
    return this.name.get();
  }

  @Override
  public int hashCode() {
    return this.getId().hashCode();
  }

  public void play() {
    this.isPlaying.set(true);
    this.getKey().getNote().play();
  }

  public void setId(String value) {
    this.uuid.set(value);
  }

  public void setKey(Key value) {
    this.key.set(value);
  }

  public void setName(String value) {
    this.name.set(value);
  }

  public void stop() {
    this.isPlaying.set(false);
    this.getKey().getNote().stop();
  }
}
