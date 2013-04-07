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
using System.Collections.Generic;
using SLaB.Utilities;
using System.ComponentModel;
using System.Collections.ObjectModel;
using System.IO.IsolatedStorage;

namespace DePhoneTunes
{
    public class PitchPipeModel : NotesModel
    {
        private const string ModeKey = "PitchPipe_Mode";
        private ObservableCollection<Note> _CToC;
        private ObservableCollection<Note> _FToF;

        public PitchPipeModel()
        {
            UiUtilities.Dispatcher.BeginInvoke(() =>
                {
                    _CToC = new ObservableCollection<Note>();
                    _CToC.Add(FindNote("C"));
                    _CToC.Add(FindNote("C", Accidental.Sharp));
                    _CToC.Add(FindNote("D"));
                    _CToC.Add(FindNote("D", Accidental.Sharp));
                    _CToC.Add(FindNote("E"));
                    _CToC.Add(FindNote("F"));
                    _CToC.Add(FindNote("F", Accidental.Sharp));
                    _CToC.Add(FindNote("G"));
                    _CToC.Add(FindNote("G", Accidental.Sharp));
                    _CToC.Add(FindNote("A"));
                    _CToC.Add(FindNote("A", Accidental.Sharp));
                    _CToC.Add(FindNote("B"));

                    _FToF = new ObservableCollection<Note>();
                    _FToF.Add(FindNote("F"));
                    _FToF.Add(FindNote("F", Accidental.Sharp));
                    _FToF.Add(FindNote("G"));
                    _FToF.Add(FindNote("G", Accidental.Sharp));
                    _FToF.Add(FindNote("A"));
                    _FToF.Add(FindNote("A", Accidental.Sharp));
                    _FToF.Add(FindNote("B"));
                    _FToF.Add(FindNote("C", octave: 5));
                    _FToF.Add(FindNote("C", Accidental.Sharp, octave: 5));
                    _FToF.Add(FindNote("D", octave: 5));
                    _FToF.Add(FindNote("D", Accidental.Sharp, octave: 5));
                    _FToF.Add(FindNote("E", octave: 5));
                    bool settingsMode = false;
                    if (!DesignerProperties.IsInDesignTool)
                        IsolatedStorageSettings.ApplicationSettings.SetIfContainsKey(ModeKey, (bool val) => settingsMode = val);
                    IsFromFToF = true;
                    IsFromFToF = settingsMode;
                });
        }

        protected override void IsCurrentModelChanged()
        {
            if (!IsCurrentModel)
            {
                foreach (var v in _CToC)
                    v.Stop();
                foreach (var v in _FToF)
                    v.Stop();
            }
        }

        private bool _IsFromFToF;
        public bool IsFromFToF
        {
            get
            {
                return _IsFromFToF;
            }
            set
            {
                if (!EqualityComparer<bool>.Default.Equals(_IsFromFToF, value))
                {
                    foreach (var v in _CToC)
                        v.Stop();
                    foreach (var v in _FToF)
                        v.Stop();
                    _IsFromFToF = value;
                    if (IsFromFToF)
                        Notes = _FToF;
                    else
                        Notes = _CToC;
                    if (!DesignerProperties.IsInDesignTool)
                        IsolatedStorageSettings.ApplicationSettings.SetIfNotInDesignMode(ModeKey, IsFromFToF);
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("IsFromFToF"));
                }
            }
        }
    }
}
