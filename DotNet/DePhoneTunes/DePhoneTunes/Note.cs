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
using System.ComponentModel;
using System.Collections.Generic;
using SLaB.Utilities;
using Microsoft.Xna.Framework.Audio;

namespace DePhoneTunes
{
    public class Note : INotifyPropertyChanged
    {
        private static List<Note> _CachedNotes;
        public static IEnumerable<Note> CommonNotes
        {
            get
            {
                if (_CachedNotes != null)
                    return _CachedNotes;
                _CachedNotes = new List<Note>();
                var notes = _CachedNotes;
                notes.Add(new Note { FriendlyName = "C", Octave = 0, Accidental = Accidental.Natural, KeyNumber = -8 });
                notes.Add(new Note { FriendlyName = "C", Octave = 0, Accidental = Accidental.Sharp, KeyNumber = -7 });
                notes.Add(new Note { FriendlyName = "D", Octave = 0, Accidental = Accidental.Flat, KeyNumber = -7 });
                notes.Add(new Note { FriendlyName = "D", Octave = 0, Accidental = Accidental.Natural, KeyNumber = -6 });
                notes.Add(new Note { FriendlyName = "D", Octave = 0, Accidental = Accidental.Sharp, KeyNumber = -5 });
                notes.Add(new Note { FriendlyName = "E", Octave = 0, Accidental = Accidental.Flat, KeyNumber = -5 });
                notes.Add(new Note { FriendlyName = "E", Octave = 0, Accidental = Accidental.Natural, KeyNumber = -4 });
                notes.Add(new Note { FriendlyName = "F", Octave = 0, Accidental = Accidental.Natural, KeyNumber = -3 });
                notes.Add(new Note { FriendlyName = "F", Octave = 0, Accidental = Accidental.Sharp, KeyNumber = -2 });
                notes.Add(new Note { FriendlyName = "G", Octave = 0, Accidental = Accidental.Flat, KeyNumber = -2 });
                notes.Add(new Note { FriendlyName = "G", Octave = 0, Accidental = Accidental.Natural, KeyNumber = -1 });
                notes.Add(new Note { FriendlyName = "G", Octave = 0, Accidental = Accidental.Sharp, KeyNumber = 0 });
                notes.Add(new Note { FriendlyName = "A", Octave = 0, Accidental = Accidental.Flat, KeyNumber = 0 });
                notes.Add(new Note { FriendlyName = "A", Octave = 0, Accidental = Accidental.Natural, KeyNumber = 1 });
                notes.Add(new Note { FriendlyName = "A", Octave = 0, Accidental = Accidental.Sharp, KeyNumber = 2 });
                notes.Add(new Note { FriendlyName = "B", Octave = 0, Accidental = Accidental.Flat, KeyNumber = 2 });
                notes.Add(new Note { FriendlyName = "B", Octave = 0, Accidental = Accidental.Natural, KeyNumber = 3 });
                int originalCount = notes.Count;
                for (int octave = 1; octave < 8; octave++)
                    for (int i = 0; i < originalCount; i++)
                    {
                        notes.Add(new Note { FriendlyName = notes[i].FriendlyName, Octave = octave, Accidental = notes[i].Accidental, Frequency = notes[i].Frequency * Math.Pow(2, octave) });
                        if (notes[notes.Count - 1].FriendlyName.Equals("C") && octave == 4 && notes[notes.Count - 1].Accidental == Accidental.Natural)
                            _C4 = notes[notes.Count - 1];
                    }
                return _CachedNotes;
            }
        }

        public Note Clone()
        {
            return new Note
            {
                FriendlyName = FriendlyName,
                Octave = Octave,
                Accidental = Accidental,
                Frequency = Frequency,
                KeyNumber = KeyNumber,
                Alternate = Alternate != null ? Alternate.Clone() : null
            };
        }

        private static double GetNoteFrequency(int number)
        {
            return 440 * Math.Pow(2, (number - 49) / 12.0);
        }

        private static List<Note> _CachedPrunedNotes;
        public static IEnumerable<Note> PrunedNotes
        {
            get
            {
                if (_CachedPrunedNotes != null)
                    return _CachedPrunedNotes;
                var notes = new List<Note>(CommonNotes);
                for (int x = 1; x < notes.Count; x++)
                {
                    if (notes[x].Frequency == notes[x - 1].Frequency)
                    {
                        notes[x - 1].Alternate = notes[x];
                        notes.RemoveAt(x);
                        x--;
                    }
                }
                _CachedPrunedNotes = notes;
                return _CachedPrunedNotes;
            }
        }

        private static Note _C4;
        public static Note C4
        {
            get
            {
                return _C4;
            }
        }
        private SoundEffect _Effect;
        private SoundEffectInstance _EffectInstance;

        public Note()
        {
            PlayStopCommand = new LambdaCommand<object>(o =>
                {
                    if (this.IsPlaying)
                        this.Stop();
                    else
                        this.Play();
                });
        }
        public override string ToString()
        {
            string result = FriendlyName;
            switch (Accidental)
            {
                case Accidental.Flat:
                    result += "b";
                    break;
                case Accidental.Natural:
                    break;
                case Accidental.Sharp:
                    result += "s";
                    break;
            }
            return result;
        }

        private bool EqualsWithoutAlternate(object obj)
        {
            Note other = obj as Note;
            if (other == null)
                return false;
            return (other.FriendlyName.Equals(FriendlyName) && other.Octave.Equals(Octave) && other.Accidental.Equals(Accidental));
        }

        public override bool Equals(object obj)
        {
            return EqualsWithoutAlternate(obj) || (this.Alternate != null && Alternate.Equals(obj));
        }

        private string _FriendlyName;
        public string FriendlyName
        {
            get
            {
                return _FriendlyName;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_FriendlyName, value))
                {
                    _FriendlyName = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("FriendlyName"));
                }
            }
        }


        private int _Octave;
        public int Octave
        {
            get
            {
                return _Octave;
            }
            set
            {
                if (!EqualityComparer<int>.Default.Equals(_Octave, value))
                {
                    _Octave = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Octave"));
                }
            }
        }

        private Accidental _Accidental;
        public Accidental Accidental
        {
            get
            {
                return _Accidental;
            }
            set
            {
                if (!EqualityComparer<Accidental>.Default.Equals(_Accidental, value))
                {
                    _Accidental = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Accidental"));
                }
            }
        }

        private double _Frequency;
        public double Frequency
        {
            get
            {
                return _Frequency;
            }
            set
            {
                if (!EqualityComparer<double>.Default.Equals(_Frequency, value))
                {
                    _Frequency = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Frequency"));
                }
            }
        }


        private int? _KeyNumber;
        public int? KeyNumber
        {
            get
            {
                return _KeyNumber;
            }
            set
            {
                if (!EqualityComparer<int?>.Default.Equals(_KeyNumber, value))
                {
                    _KeyNumber = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("KeyNumber"));
                    if (value.HasValue)
                        Frequency = GetNoteFrequency(value.Value);
                }
            }
        }

        private bool _IsPlaying;
        public bool IsPlaying
        {
            get
            {
                return _IsPlaying;
            }
            set
            {
                if (!EqualityComparer<bool>.Default.Equals(_IsPlaying, value))
                {
                    _IsPlaying = value;
                    if (value)
                        Play();
                    else
                        Stop();
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("IsPlaying"));
                }
            }
        }


        private ICommand _PlayStopCommand;
        public ICommand PlayStopCommand
        {
            get
            {
                return _PlayStopCommand;
            }
            private set
            {
                if (!EqualityComparer<ICommand>.Default.Equals(_PlayStopCommand, value))
                {
                    _PlayStopCommand = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("PlayStopCommand"));
                }
            }
        }


        private Note _Alternate;
        public Note Alternate
        {
            get
            {
                return _Alternate;
            }
            set
            {
                if (!EqualityComparer<Note>.Default.Equals(_Alternate, value))
                {
                    _Alternate = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Alternate"));
                }
            }
        }

        private bool isAttemptingToPlay;
        public void Play()
        {
            if (isAttemptingToPlay)
                return;
            isAttemptingToPlay = true;
            //if (_EffectInstance != null)
            //    return;
            //_Effect = SoundEffect.FromStream(WaveStreamGenerator.GetStream(Frequency, TimeSpan.FromSeconds(5.0)));
            //_EffectInstance = _Effect.CreateInstance();
            //_EffectInstance.IsLooped = true;
            if (_EffectInstance == null)
                _EffectInstance = PitchSoundEffectGenerator.GetPitchSoundEffect(Frequency, 16000, AudioChannels.Mono, TimeSpan.FromSeconds(1.0 / 3), 3);
            _EffectInstance.Play();
            IsPlaying = true;
            isAttemptingToPlay = false;
        }

        public void Stop()
        {
            try
            {
                if (_EffectInstance != null)
                {
                    _EffectInstance.Stop();
                    //_EffectInstance.Dispose();
                }
            }
            catch { }
            finally
            {
                //_EffectInstance = null;
                //_Effect = null;
                IsPlaying = false;
            }
        }

        public event PropertyChangedEventHandler PropertyChanged;
    }

    public enum Accidental
    {
        Natural = 0,
        Sharp = 1,
        Flat = -1,
    }
}
