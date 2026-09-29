// Renders one held note through TinySoundFont as Pitch Perfect's Android app plays it: a channel
// set to the program, velocity 100, mono 44.1 kHz. Writes float samples to stdout.
// Usage: tsf_note <soundfont.sf2> <program> <key> <seconds>
#define TSF_IMPLEMENTATION
#include "tsf.h"
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv) {
  if (argc != 5) { fprintf(stderr, "usage: %s font.sf2 program key seconds\n", argv[0]); return 2; }
  tsf* f = tsf_load_filename(argv[1]);
  if (!f) { fprintf(stderr, "can't load %s\n", argv[1]); return 1; }
  tsf_set_output(f, TSF_MONO, 44100, 0);
  tsf_channel_set_presetnumber(f, 0, atoi(argv[2]), 0);
  tsf_channel_note_on(f, 0, atoi(argv[3]), 100 / 127.0f);
  int samples = (int)(atof(argv[4]) * 44100);
  float* buffer = (float*)malloc(sizeof(float) * samples);
  tsf_render_float(f, buffer, samples, 0);
  fwrite(buffer, sizeof(float), samples, stdout);
  free(buffer);
  tsf_close(f);
  return 0;
}
