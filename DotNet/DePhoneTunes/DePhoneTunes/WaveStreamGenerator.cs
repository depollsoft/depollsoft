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
using System.IO;
using System.Text;
using System.Linq;

namespace DePhoneTunes {
  public class WaveStreamGenerator {
    private const short PcmFormatCode = 0x0001;
    private const int SampleRate = 16000;
    private const int Channels = 1;
    public static Stream GetStream(double frequency, TimeSpan duration, double scaleFactor = 0.9, bool clipToFullCycle = true) {
      //return Application.GetResourceStream(new Uri("c4.wav", UriKind.Relative)).Stream;
      int sampleCount = (int)(duration.TotalSeconds * SampleRate);
      if (clipToFullCycle) {
        double fullCycle = 1.0 / frequency * SampleRate;
        sampleCount -= (int)(Math.Round(sampleCount % fullCycle));
      }
      // See: http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html
      MemoryStream ms = new MemoryStream(sampleCount * Channels * 16 / 8);
      BinaryWriter bw = new BinaryWriter(ms);
      // Write ckID
      bw.Write(Encoding.UTF8.GetBytes("RIFF"));
      // Write an empty 4-byte space for ckSize
      bw.Write(0);
      // Write WAVEID
      bw.Write(Encoding.UTF8.GetBytes("WAVE"));

      // Write WAVE chunks



      // Write ckID
      bw.Write(Encoding.UTF8.GetBytes("fmt "));
      // Write cksize
      bw.Write(16);
      // Write wFormatTag
      bw.Write(PcmFormatCode);
      // Write nChcannels
      bw.Write((short)Channels);
      // Write nSamplesPerSec
      bw.Write(SampleRate);
      // Write nAvgBytesPerSec
      bw.Write(SampleRate * 16 / 8 * Channels);
      // Write nBlockAlign
      bw.Write((short)(16 / 8 * Channels));
      // Write nBitsPerSample (16-bit PCM)
      bw.Write((short)16);
      // Write ckID
      bw.Write(Encoding.UTF8.GetBytes("data"));
      // Write cksize
      bw.Write(sampleCount * Channels * 16 / 8);
      for (int sample = 0; sample < sampleCount; sample++) {
        var amp = GetAmplitude(frequency, ((double)sample) / SampleRate, scaleFactor);
        for (int x = 0; x < Channels; x++)
          bw.Write(amp);
      }
      bw.Flush();
      // Return to write the original ckSize
      bw.Seek(4, SeekOrigin.Begin);
      bw.Write((int)(ms.Length - 8));
      bw.Flush();
      //bw.Seek(0, SeekOrigin.Begin);
      //BinaryReader br = new BinaryReader(ms);
      //StringBuilder sb = new StringBuilder();
      //int count = 0;
      //while (ms.Position < ms.Length)
      //{
      //    sb.Append(string.Format("{0:x2} ", br.ReadByte()));
      //    if (++count % 16 == 0)
      //        sb.AppendLine();
      //}
      //var str = sb.ToString();
      ms.Seek(0, SeekOrigin.Begin);
      return ms;
    }

    private static byte[] GetLittleEndian(short value) {
      MemoryStream ms = new MemoryStream();
      BinaryWriter bw = new BinaryWriter(ms);
      bw.Write(value);
      bw.Flush();
      return ms.ToArray().Reverse().ToArray();
    }

    private static byte[] GetLittleEndian(int value) {
      MemoryStream ms = new MemoryStream();
      BinaryWriter bw = new BinaryWriter(ms);
      bw.Write(value);
      bw.Flush();
      return ms.ToArray().Reverse().ToArray();
    }

    private const double TwoPi = Math.PI * 2;
    private const double PiOverTwo = Math.PI / 2;

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
