using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using Microsoft.Phone.Shell;
using SLaB.Utilities;

namespace DePhoneTunes
{
    public class NotesModel : INotifyPropertyChanged
    {
        protected int constructingCount;
        private IEnumerable<Note> _MasterList;
        public NotesModel()
        {
            constructingCount++;
            _MasterList = Note.CommonNotes;
            Notes = new List<Note>(Note.PrunedNotes);

            SelectedNote = Note.C4;
            if (!DesignerProperties.IsInDesignTool)
                PhoneApplicationService.Current.State.SetIfContainsKey(GetType().ToString() + ".SelectedNoteIndex", (int i) => SelectedNoteIndex = i);
            constructingCount--;
        }
        private IEnumerable<Note> _Notes;
        public IEnumerable<Note> Notes
        {
            get
            {
                return _Notes;
            }
            protected set
            {
                if (!EqualityComparer<IEnumerable<Note>>.Default.Equals(_Notes, value))
                {
                    _Notes = value;
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("Notes"));
                }
            }
        }

        protected Note FindNote(string name, Accidental accidental = Accidental.Natural, int octave = 4)
        {
            var foundNote = from n in _MasterList
                            where n.FriendlyName.Equals(name) && n.Accidental == accidental && n.Octave == octave
                            select n;
            return foundNote.FirstOrDefault();
        }


        private Note _SelectedNote;
        public Note SelectedNote
        {
            get
            {
                return _SelectedNote;
            }
            set
            {
                if (!EqualityComparer<Note>.Default.Equals(_SelectedNote, value))
                {
                    if (SelectedNote != null)
                        SelectedNote.Stop();
                    _SelectedNote = value;
                    if (SelectedNote != null && ShouldPlay)
                        SelectedNote.Play();
                    SelectedNoteIndex = GetNoteIndex(SelectedNote);
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("SelectedNote"));
                }
            }
        }

        private int GetNoteIndex(Note n)
        {
            if (Notes == null || n == null)
                return -1;
            int x = 0;
            foreach (var v in Notes)
            {
                if (n.Equals(v))
                    return x;
                x++;
            }
            return -1;
        }


        private int _SelectedNoteIndex;
        public int SelectedNoteIndex
        {
            get
            {
                return _SelectedNoteIndex;
            }
            set
            {
                if (!EqualityComparer<int>.Default.Equals(_SelectedNoteIndex, value))
                {
                    _SelectedNoteIndex = value;
                    if (Notes != null && SelectedNoteIndex >= 0)
                        SelectedNote = Notes.ElementAt(SelectedNoteIndex);
                    if (constructingCount == 0)
                        PhoneApplicationService.Current.State.SetIfNotInDesignMode(GetType().ToString() + ".SelectedNoteIndex", SelectedNoteIndex);
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("SelectedNoteIndex"));
                }
            }
        }


        private bool _ShouldPlay;
        public bool ShouldPlay
        {
            get
            {
                return _ShouldPlay;
            }
            set
            {
                if (!EqualityComparer<bool>.Default.Equals(_ShouldPlay, value))
                {
                    _ShouldPlay = value;
                    if (!ShouldPlay)
                        SelectedNote.Stop();
                    else
                        SelectedNote.Play();
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("ShouldPlay"));
                }
            }
        }


        private bool _IsCurrentModel;
        public bool IsCurrentModel
        {
            get
            {
                return _IsCurrentModel;
            }
            set
            {
                if (!EqualityComparer<bool>.Default.Equals(_IsCurrentModel, value))
                {
                    _IsCurrentModel = value;
                    if (!IsCurrentModel)
                        ShouldPlay = false;
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("IsCurrentModel"));
                    IsCurrentModelChanged();
                }
            }
        }

        protected virtual void IsCurrentModelChanged() { }

        protected PropertyChangedEventHandler _PropertyChanged;
        public event PropertyChangedEventHandler PropertyChanged
        {
            add { _PropertyChanged += value; }
            remove { _PropertyChanged -= value; }
        }
    }
}
