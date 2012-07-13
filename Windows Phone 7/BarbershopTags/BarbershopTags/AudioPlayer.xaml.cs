using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using SLaB.Utilities;
using Microsoft.Xna.Framework.Media;
using System.Net;
using System.IO.IsolatedStorage;
using System.Text.RegularExpressions;
using System.Linq;
using System.ComponentModel;
using System.IO;

namespace BarbershopTags
{
    public partial class AudioPlayer : UserControl
    {
        private static int _Instances;
        private int _InstanceNum;
        public AudioPlayer()
        {
            InitializeComponent();
            this.MediaElement.MediaOpened += new RoutedEventHandler(MediaElement_MediaOpened);
            this.MediaElement.MediaEnded += new RoutedEventHandler(MediaElement_MediaEnded);
            this.MediaElement.CurrentStateChanged += new RoutedEventHandler(MediaElement_CurrentStateChanged);
            this.MediaElement.MediaFailed += new EventHandler<ExceptionRoutedEventArgs>(MediaElement_MediaFailed);
            this.Loaded += new RoutedEventHandler(AudioPlayer_Loaded);
            LayoutRoot.DataContext = this;
            _InstanceNum = _Instances++;
        }

        private void AudioPlayer_Loaded(object sender, RoutedEventArgs e)
        {
            RefreshButtons();
        }

        private void MediaElement_MediaEnded(object sender, RoutedEventArgs e)
        {
            MediaElement.Stop();
            MediaElement.Position = TimeSpan.Zero;
            RefreshButtons();
        }

        private void MediaElement_MediaFailed(object sender, ExceptionRoutedEventArgs e)
        {
            MediaFailed.Raise(this, new ErrorEventArgs(e.ErrorException));
        }

        public event EventHandler<ErrorEventArgs> MediaFailed;

        private void MediaElement_CurrentStateChanged(object sender, RoutedEventArgs e)
        {
            RefreshButtons();
        }

        private void RefreshButtons()
        {
            //if (MediaElement.CurrentState == MediaElementState.Opening)
            //    IsLoading = true;
            playPauseButton.IsChecked = MediaElement.CurrentState == MediaElementState.Playing;
            Enabler.IsEnabled = Source != null;
            MediaElement.Volume = new SettingsModel().GlobalVolume;
        }

        private void MediaElement_MediaOpened(object sender, RoutedEventArgs e)
        {
            IsLoading = false;
            positionSlider.Maximum = MediaElement.NaturalDuration.TimeSpan.TotalMilliseconds;
            if (MediaElement.NaturalDuration.HasTimeSpan)
                Duration = MediaElement.NaturalDuration.TimeSpan;
            else
                Duration = null;
        }


        public bool IsLoading
        {
            get { return (bool)GetValue(IsLoadingProperty); }
            set { SetValue(IsLoadingProperty, value); }
        }

        public static readonly DependencyProperty IsLoadingProperty =
            DependencyProperty.Register("IsLoading", typeof(bool), typeof(AudioPlayer), new PropertyMetadata(default(bool), OnIsLoadingChanged));

        private static void OnIsLoadingChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((AudioPlayer)obj).OnIsLoadingChanged((bool)args.OldValue, (bool)args.NewValue);
        }

        private void OnIsLoadingChanged(bool oldValue, bool newValue)
        {
        }

        public double Balance
        {
            get { return (double)GetValue(BalanceProperty); }
            set { SetValue(BalanceProperty, value); }
        }

        public static readonly DependencyProperty BalanceProperty =
            DependencyProperty.Register("Balance", typeof(double), typeof(AudioPlayer), new PropertyMetadata(default(double), OnBalanceChanged));

        private static void OnBalanceChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((AudioPlayer)obj).OnBalanceChanged((double)args.OldValue, (double)args.NewValue);
        }

        private void OnBalanceChanged(double oldValue, double newValue)
        {
            MediaElement.Balance = newValue;
        }

        public TimeSpan? Duration
        {
            get { return (TimeSpan?)GetValue(DurationProperty); }
            set { SetValue(DurationProperty, value); }
        }

        // Using a DependencyProperty as the backing store for Duration.  This enables animation, styling, binding, etc...
        public static readonly DependencyProperty DurationProperty =
            DependencyProperty.Register("Duration", typeof(TimeSpan?), typeof(AudioPlayer), new PropertyMetadata(null));

        private int _LastTemp = 1;
        private string _LastTempFile = "";
        private IsolatedStorageFileStream _LastStream;
        private static readonly Regex _DispositionPattern = new Regex("attachment; filename=(\"(?<filename>.+)\")|(?<filename>.+)");
        private WebRequest _LatestRequest = null;
        private void RefreshSource()
        {
            var iso = IsolatedStorageFile.GetUserStoreForApplication();
            WebRequest wr = _LatestRequest = WebRequest.Create(Source);
            IsLoading = true;
            MediaElement.ClearValue(MediaElement.SourceProperty);
            if (_LastStream != null)
                _LastStream.Close();
            try
            {
                if (iso.FileExists(_LastTempFile))
                    iso.DeleteFile(_LastTempFile);
            }
            catch { }
            if (Source == null && (MediaPlayer.GameHasControl || MessageBox.Show("Selecting this track will stop the audio playing on your device.  Is this ok?", "Background audio will stop", MessageBoxButton.OKCancel) == MessageBoxResult.OK))
                MediaElement.Source = null;
            wr.BeginGetResponse(res =>
                {
                    UiUtilities.ExecuteOnUiThread(() =>
                        {
                            try
                            {
                                if (wr != _LatestRequest)
                                    return;
                                var response = wr.EndGetResponse(res);
                                var finalUri = response.ResponseUri;
                                string filename = Path.GetFileName(finalUri.AbsolutePath);
                                var file = iso.CreateFile(_LastTempFile = "AudioPlayerTemp" + _InstanceNum + "_" + ++_LastTemp + filename);
                                if (filename.ToLowerInvariant().EndsWith(".mid"))
                                    throw new NotSupportedException();
                                byte[] bytes = new byte[response.ContentLength];
                                response.GetResponseStream().Read(bytes, 0, (int)response.ContentLength);
                                file.Write(bytes, 0, (int)response.ContentLength);
                                file.Close();
                                _LastStream = file = iso.OpenFile(_LastTempFile, System.IO.FileMode.Open);
                                _LastTemp++;
                                Duration = null;
                                if (Source == null || MediaPlayer.GameHasControl || MessageBox.Show("Selecting this track will stop the audio playing on your device.  Is this ok?", "Background audio will stop", MessageBoxButton.OKCancel) == MessageBoxResult.OK)
                                    MediaElement.SetSource(file);
                                else
                                    Source = null;
                                RefreshButtons();
                            }
                            catch (Exception e)
                            {
                                MediaFailed.Raise(this, new ErrorEventArgs(e));
                            }
                            finally
                            {
                                IsLoading = false;
                            }
                        });
                }, null);
        }

        ~AudioPlayer()
        {
            if (!DesignerProperties.IsInDesignTool)
            {
                var iso = IsolatedStorageFile.GetUserStoreForApplication();
                foreach (var file in iso.GetFileNames().Where(name => name.StartsWith("AudioPlayerTemp" + _InstanceNum)))
                    iso.DeleteFile(file);
            }
        }

        public Uri Source
        {
            get { return (Uri)GetValue(SourceProperty); }
            set { SetValue(SourceProperty, value); }
        }

        // Using a DependencyProperty as the backing store for Source.  This enables animation, styling, binding, etc...
        public static readonly DependencyProperty SourceProperty =
            DependencyProperty.Register("Source", typeof(Uri), typeof(AudioPlayer), new PropertyMetadata(null, OnSourceChanged));

        public static void OnSourceChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((AudioPlayer)obj).OnSourceChanged();
        }

        private void OnSourceChanged()
        {
            RefreshSource();
        }

        private void ToggleButton_Click(object sender, RoutedEventArgs e)
        {
            if (MediaElement.CurrentState == MediaElementState.Playing)
                MediaElement.Pause();
            else if (MediaPlayer.GameHasControl || MessageBox.Show("Listening to this track will stop the audio playing on your device.  Is this ok?", "Background audio will stop", MessageBoxButton.OKCancel) == MessageBoxResult.OK)
                MediaElement.Play();
            RefreshButtons();
        }

        private void StopButton_Click(object sender, RoutedEventArgs e)
        {
            MediaElement.Stop();
        }


    }
    public class ErrorEventArgs : EventArgs
    {
        public ErrorEventArgs(Exception error)
        {
            Error = error;
        }
        public Exception Error { get; private set; }
    }
}
