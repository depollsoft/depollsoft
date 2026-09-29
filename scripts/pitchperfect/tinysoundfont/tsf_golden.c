// Renders TinySoundFont reference clips for the Kotlin port's golden tests.
// Usage: tsf_golden <soundfont.sf2> <clips.txt> <out.raw>
// clips.txt holds clips of timed channel events (see make_golden.sh); each
// clip renders mono 44.1 kHz 16-bit through tsf_render_short, split at every
// event time exactly as the Kotlin test replays it.
#define TSF_IMPLEMENTATION
#include "tsf.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char** argv) {
  if (argc != 4) { fprintf(stderr, "usage: %s font.sf2 clips.txt out.raw\n", argv[0]); return 2; }
  tsf* base = tsf_load_filename(argv[1]);
  if (!base) { fprintf(stderr, "can't load %s\n", argv[1]); return 1; }
  FILE* in = fopen(argv[2], "r");
  FILE* out = fopen(argv[3], "wb");
  char line[256];
  tsf* f = NULL;
  int total = 0, rendered = 0;
  short* buffer = NULL;
#define RENDER_TO(target) do { int n = (target) - rendered; if (n > 0) { tsf_render_short(f, buffer + rendered, n, 0); rendered += n; } } while (0)
  while (fgets(line, sizeof line, in)) {
    char word[32];
    if (line[0] == '#' || sscanf(line, "%31s", word) != 1) continue;
    if (!strcmp(word, "clip")) {
      char name[64];
      sscanf(line, "clip %63s %d", name, &total);
      f = tsf_copy(base);
      tsf_set_output(f, TSF_MONO, 44100, 0);
      buffer = (short*)calloc(total, sizeof(short));
      rendered = 0;
    } else if (!strcmp(word, "end")) {
      RENDER_TO(total);
      fwrite(buffer, sizeof(short), total, out);
      free(buffer);
      tsf_close(f);
      f = NULL;
    } else if (!strcmp(word, "at")) {
      int at, channel; char op[16];
      sscanf(line, "at %d %15s %d", &at, op, &channel);
      RENDER_TO(at);
      if (!strcmp(op, "preset")) { int program; sscanf(line, "at %*d %*s %*d %d", &program); tsf_channel_set_presetnumber(f, channel, program, 0); }
      else if (!strcmp(op, "tuning")) { float semitones; sscanf(line, "at %*d %*s %*d %f", &semitones); tsf_channel_set_tuning(f, channel, semitones); }
      else if (!strcmp(op, "on")) { int key, velocity; sscanf(line, "at %*d %*s %*d %d %d", &key, &velocity); tsf_channel_note_on(f, channel, key, velocity / 127.0f); }
      else if (!strcmp(op, "off")) { int key; sscanf(line, "at %*d %*s %*d %d", &key); tsf_channel_note_off(f, channel, key); }
      else { fprintf(stderr, "unknown event: %s", line); return 1; }
    }
  }
  fclose(out);
  tsf_close(base);
  return 0;
}
