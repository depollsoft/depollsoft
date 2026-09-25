package depollsoft.pitchperfect.lib;

import java.util.UUID;

import depollsoft.lib.state.StateField;

public class PitchedSong implements Comparable<PitchedSong> {
  private StateField<String> name = new StateField<>(null);

  private StateField<Key> key = new StateField<>(null);

  private String uuid = null;

  private StateField<Boolean> isPlaying = new StateField<>(false);

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
    return this.uuid;
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
    this.uuid = value;
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
