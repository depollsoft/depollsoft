using System;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Ink;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using Microsoft.Xna.Framework.Audio;
using System.Threading;
using System.Linq;

namespace DePhoneTunes {
  public static class PitchSoundEffectGenerator {
    private const double TwoPi = Math.PI * 2;
    private const double PiOverTwo = Math.PI / 2;
    public static SoundEffectInstance GetPitchSoundEffect(double frequency, int sampleRate, AudioChannels channels, TimeSpan bufferTime, int initialBufferFills = 0) {
      DynamicSoundEffectInstance dsei = new DynamicSoundEffectInstance(sampleRate, channels);
      int sampleSize = dsei.GetSampleSizeInBytes(bufferTime);
      int numChannels = channels == AudioChannels.Mono ? 1 : 2;
      double sampleTimeDelta = 1.0 / sampleRate;
      double curTime = 0;
      Action generateBuffer = () => {
        byte[] samples = new byte[sampleSize];
        for (int x = 0; x < sampleSize; x += numChannels * 2) {
          short result = GetAmplitude(frequency, curTime, 1);
          samples[x] = (byte)(result & 0xFF);
          samples[x + 1] = (byte)(result >> 8);
          if (numChannels == 2) {
            samples[x + 2] = (byte)(result & 0xFF);
            samples[x + 3] = (byte)(result >> 8);
          }
          curTime += sampleTimeDelta;
        }
        dsei.SubmitBuffer(samples);
      };
      for (int x = 0; x < initialBufferFills; x++)
        generateBuffer();
      dsei.BufferNeeded += (o, a) => {
        generateBuffer();
      };
      return dsei;
    }

    private static short GetAmplitude(double frequency, double time, double scaleFactor) {
      int value = (int)(Math.Sin(time * frequency * TwoPi) * scaleFactor * short.MaxValue * 3);
      if (value > short.MaxValue)
        return short.MaxValue;
      if (value < short.MinValue)
        return short.MinValue;
      return (short)value;
    }
  }
}
