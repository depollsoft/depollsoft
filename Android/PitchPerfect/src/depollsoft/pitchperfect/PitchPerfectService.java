package depollsoft.pitchperfect;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.HashSet;

import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

public class PitchPerfectService extends Service {
  private final HashSet<Note> playingNotes = new HashSet<Note>();

  public PitchPerfectService() {
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    String noteName = intent.getStringExtra("noteName");
    String accidentalName = intent.getStringExtra("accidental");
    int octave = intent.getIntExtra("octave", 4);
    Accidental accidental = Accidental.Natural;
    if (accidentalName != null) {
      if (accidentalName.equals("b")) {
        accidental = Accidental.Flat;
      } else if (accidentalName.equals("#")) {
        accidental = Accidental.Sharp;
      }
    }
    Note note = Note.findNote(noteName, accidental, octave);
    if (intent.hasExtra("play")) {
      if (intent.getBooleanExtra("play", false)) {
        startPlaying(note);
      } else {
        stopPlaying(note);
      }
    } else {
      if (note.getIsPlaying()) {
        stopPlaying(note);
      } else {
        startPlaying(note);
      }
    }
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void startPlaying(Note n) {
    n.play();
    playingNotes.add(n);
  }

  private void stopPlaying(Note n) {
    n.stop();
    playingNotes.remove(n);
    if (playingNotes.isEmpty()) {
      stopSelf();
    }
  }
}
