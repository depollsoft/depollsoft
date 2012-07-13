using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.IO;
using System.IO.IsolatedStorage;
using System.Linq;
using System.Windows;
using System.Windows.Threading;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.Windows.Markup;
using System.Net;
using SLaB.Utilities;

namespace SLaB.Offline
{
    [ContentProperty("OfflinedFiles")]
    public class OfflineManager : ISupportInitialize
    {
        private readonly DispatcherTimer _DispatcherTimer;
        private readonly Dictionary<Uri, OfflinedFile> _FileCache;
        private readonly Dictionary<string, byte[]> _FileQueue;
        static OfflineManager()
        {
            Current = new OfflineManager();
        }

        public OfflineManager()
        {
            _DispatcherTimer = new DispatcherTimer();
            _DispatcherTimer.Tick += OnDispatcherTimerTick;
            _FileQueue = new Dictionary<string, byte[]>();
            _FileCache = new Dictionary<Uri, OfflinedFile>();
            OfflinedFiles = new ObservableCollection<OfflinedFile>();
            NotificationFrequency = new TimeSpan(0, 0, 0, 0, 500);
            Application.Current.CheckAndDownloadUpdateCompleted += OnApplicationCheckedForUpdates;
            Application.Current.InstallStateChanged += OnInstallStateChanged;
            OfflinedFiles.CollectionChanged += OnOfflinedFilesChanged;
            WaitingToFlushToIsolatedStorage = false;
        }

        public bool WaitingToFlushToIsolatedStorage { get; private set; }

        private void OnOfflinedFilesChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            OfflinedFile added = null, removed = null;
            switch (e.Action)
            {
                case NotifyCollectionChangedAction.Add:
                    added = e.NewItems.Cast<OfflinedFile>().First();
                    break;
                case NotifyCollectionChangedAction.Remove:
                    removed = e.OldItems.Cast<OfflinedFile>().First();
                    break;
                case NotifyCollectionChangedAction.Replace:
                    added = e.NewItems.Cast<OfflinedFile>().First();
                    removed = e.OldItems.Cast<OfflinedFile>().First();
                    break;
            }
            if (removed != null)
            {
                WebRequest.RegisterPrefix(removed.Uri.OriginalString, removed.WebRequestCreator);
            }
            if (added != null)
            {
                WebRequest.RegisterPrefix(added.Uri.OriginalString, new OfflineWebRequestCreator(added.WebRequestCreator));
            }
        }

        public static OfflineManager Current { get; private set; }

        public ObservableCollection<OfflinedFile> OfflinedFiles { get; private set; }

        public TimeSpan NotificationFrequency
        {
            get { return _DispatcherTimer.Interval; }
            set { _DispatcherTimer.Interval = value; }
        }

        #region ISupportInitialize Members

        void ISupportInitialize.BeginInit()
        {
        }

        void ISupportInitialize.EndInit()
        {
        }

        #endregion

        public event EventHandler<IsolatedStorageQuotaIncreaseRequestEventArgs> IsolatedStorageQuotaIncreaseRequested;

        public void FlushToIsolatedStorage()
        {
            lock (_FileQueue)
            {
                WaitingToFlushToIsolatedStorage = false;
                var temp = new Dictionary<string, byte[]>(_FileQueue);
                foreach (var kvp in temp)
                    EnqueueFileWrite(kvp.Key, kvp.Value);
            }
        }

        public static OfflineManager GetOfflineManager(Application app)
        {
            if (app != Application.Current)
                return null;
            return Current;
        }

        public static void SetOfflineManager(Application app, OfflineManager manager)
        {
            if (app != Application.Current)
                throw new ArgumentException("OfflineManager can only be set on the main Application object", "app");
            Current = manager;
        }

        internal void EnqueueFileWrite(string fileName, byte[] data)
        {
            lock (_FileQueue)
            {
                if (_FileQueue.ContainsKey(fileName))
                    _FileQueue.Remove(fileName);
                int sizeChange = GetSizeChange(fileName, data);
                if (sizeChange <= IsolatedStorageFile.GetUserStoreForSite().AvailableFreeSpace)
                {
                    var ms = new MemoryStream(data);
                    string[] pathParts = fileName.Split(Path.DirectorySeparatorChar);
                    string pathSoFar = "";
                    foreach (var part in pathParts.Take(pathParts.Length - 1))
                    {
                        pathSoFar += part;
                        pathSoFar += Path.DirectorySeparatorChar;
                        IsolatedStorageFile.GetUserStoreForSite().CreateDirectory(pathSoFar);
                    }
                    IsolatedStorageFileStream file = IsolatedStorageFile.GetUserStoreForSite().OpenFile(fileName,
                                                                                                        FileMode.Create,
                                                                                                        FileAccess.Write,
                                                                                                        FileShare.None);
                    ms.CopyTo(file);
                    file.Close();
                }
                else
                {
                    _FileQueue[fileName] = data;
                    UiUtilities.ExecuteOnUiThread(() =>
                        {
                            _DispatcherTimer.Stop();
                            _DispatcherTimer.Start();
                        });
                }
            }
        }

        internal Stream GetFileData(string fileName)
        {
            byte[] data;
            if (_FileQueue.TryGetValue(fileName, out data))
                return new MemoryStream(data, false);
            if (IsolatedStorageFile.GetUserStoreForSite().FileExists(fileName))
            {
                var ms = new MemoryStream();
                IsolatedStorageFileStream file = IsolatedStorageFile.GetUserStoreForSite().OpenFile(fileName, FileMode.Open);
                file.CopyTo(ms);
                file.Close();
                ms.Seek(0, SeekOrigin.Begin);
                return ms;
            }
            return null;
        }

        internal OfflinedFile GetOfflinedFile(Uri uri)
        {
            OfflinedFile of;
            if (_FileCache.TryGetValue(uri, out of))
                return of;
            return _FileCache[uri] = new OfflinedFile { Uri = uri };
        }

        private int GetSizeChange(string fileName, byte[] data)
        {
            if (!IsolatedStorageFile.GetUserStoreForSite().FileExists(fileName))
                return data.Length;
            IsolatedStorageFileStream file = IsolatedStorageFile.GetUserStoreForSite().OpenFile(fileName, FileMode.Open);
            var fileLength = (int)file.Length;
            file.Close();
            return data.Length - fileLength;
        }

        private void OnApplicationCheckedForUpdates(object sender, CheckAndDownloadUpdateCompletedEventArgs e)
        {
            if (e.Error != null)
                return;
        }

        private void OnDispatcherTimerTick(object sender, EventArgs e)
        {
            lock (_FileQueue)
            {
                if (!WaitingToFlushToIsolatedStorage)
                {
                    if (_FileQueue.Count == 0)
                    {
                        _DispatcherTimer.Stop();
                        return;
                    }
                    int totalSizeChange = _FileQueue.Select(kvp => GetSizeChange(kvp.Key, kvp.Value)).Sum();
                    var neededSpace = (int)(totalSizeChange - IsolatedStorageFile.GetUserStoreForSite().AvailableFreeSpace);
                    EventHandler<IsolatedStorageQuotaIncreaseRequestEventArgs> notify = IsolatedStorageQuotaIncreaseRequested;
                    if (notify != null)
                        notify(this, new IsolatedStorageQuotaIncreaseRequestEventArgs { QuotaIncreaseSize = totalSizeChange });
                    WaitingToFlushToIsolatedStorage = true;
                }
            }
        }

        private void OnInstallStateChanged(object sender, EventArgs e)
        {
        }
    }

    public class IsolatedStorageQuotaIncreaseRequestEventArgs : EventArgs
    {
        public int QuotaIncreaseSize { get; internal set; }
    }
}