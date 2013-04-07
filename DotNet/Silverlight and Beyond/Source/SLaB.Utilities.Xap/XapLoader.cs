#region Using Directives

using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Net;
using System.Reflection;
using System.Threading;
using System.Windows;
using System.Windows.Markup;
using System.Windows.Resources;


#endregion

namespace SLaB.Utilities.Xap
{
    /// <summary>
    ///   A utility class that can be used to load Xaps and their accompanying cached assemblies.
    /// </summary>
    public class XapLoader
    {


        /// <summary>
        ///   Asynchronously loads a Xap at the given Uri.
        /// </summary>
        /// <param name = "uri">The Uri that contains the location of the Xap.</param>
        /// <param name = "callback">The callback to be invoked when the operation completes.</param>
        /// <param name = "asyncState">Custom state to be carried with the async operation.</param>
        /// <returns>A XapAsyncResult that can be used to retrieve the loaded Xap.</returns>
        public IAsyncResult BeginLoadXap(Uri uri, AsyncCallback callback, object asyncState)
        {
            WebRequest wr = WebRequest.Create(UiUtilities.ExecuteOnUiThread(() => SimpleWebClient.MakeAbsolute(uri)));
            SimpleWebClient wc = new SimpleWebClient(wr);
            if (wr is HttpWebRequest)
                ((HttpWebRequest)wr).AllowReadStreamBuffering = false;
            XapAsyncResult result = new XapAsyncResult(asyncState);
            wc.OpenReadCompleted += (sender, args) =>
                {
                    try
                    {
                        if (args.Error != null)
                            throw args.Error;
                        Stream response = args.Result;
                        XapAsyncResult xapResult = (XapAsyncResult)this.BeginLoadXap(response,
                                        res =>
                                        {
                                            try
                                            {
                                                Xap xap =
                                                    this.EndLoadXap(res);
                                            }
                                            catch (Exception e)
                                            {
                                                result.Error = e;
                                                callback(result);
                                                return;
                                            }
                                            callback(result);
                                        },
                                        result,
                                        uri);
                    }
                    catch (Exception e)
                    {
                        result.Error = e;
                        callback(result);
                        return;
                    }
                };
            result.AddClient(wc);
            wc.OpenReadAsync();
            return result;
        }

        /// <summary>
        ///   Asynchronously loads a Xap from the given stream.
        /// </summary>
        /// <param name = "xapStream">The stream from which to load the Xap.</param>
        /// <param name = "callback">The callback to be invoked when the operation completes.</param>
        /// <param name = "asyncState">Custom state to be carried with the async operation.</param>
        /// <returns>A XapAsyncResult that can be used to retrieve the loaded Xap.</returns>
        public IAsyncResult BeginLoadXap(Stream xapStream, AsyncCallback callback, object asyncState)
        {
            return this.BeginLoadXap(xapStream,
                                     callback,
                                     new XapAsyncResult(asyncState),
                                     Application.Current.Host.Source);
        }

        /// <summary>
        ///   Cancels loading of a Xap.
        /// </summary>
        /// <param name = "result">The XapAsyncResult that was returned by BeginLoadXap.</param>
        public void CancelLoadXap(IAsyncResult result)
        {
            XapAsyncResult res = (XapAsyncResult)result;
            res.IsCancelled = true;
            foreach (var client in res.ActiveClients)
            {
                client.CancelAsync();
            }
        }

        /// <summary>
        ///   Completes an asynchronous load of a Xap.
        /// </summary>
        /// <param name = "result">The XapAsyncResult that was returned by BeginLoadXap (or passed into the callback).</param>
        /// <returns>The loaded Xap.</returns>
        public Xap EndLoadXap(IAsyncResult result)
        {
            XapAsyncResult res = (XapAsyncResult)result;
            if (res.Error != null)
                throw new Exception("A failure in loading a xap occurred", res.Error);
            return res.Result;
        }

        private IAsyncResult BeginLoadXap(Stream xapStream, AsyncCallback callback, XapAsyncResult result, Uri baseUri)
        {
            if (result.IsCancelled)
                return result;
            if (!baseUri.IsAbsoluteUri)
                baseUri = new Uri(Application.Current.Host.Source, baseUri);
            byte[] bytes = ReadStream(xapStream);
            using (xapStream = new MemoryStream(bytes))
            {
                StreamResourceInfo sri = new StreamResourceInfo(xapStream, null);
                StreamResourceInfo manifestStream = Application.GetResourceStream(sri,
                                                                                  new Uri("AppManifest.xaml",
                                                                                          UriKind.Relative));
                Deployment.Deployment manifest = GetManifest(manifestStream.Stream);
                List<Assembly> assemblies = new List<Assembly>();
                IEnumerable<Uri> externalAssemblies = null;
                Dictionary<Assembly, byte[]> byteMappings = new Dictionary<Assembly, byte[]>();
                if (result.IsCancelled)
                    return result;
                UiUtilities.ExecuteOnUiThread(() =>
                    {
                        foreach (var asm in manifest.Parts)
                        {
                            StreamResourceInfo assemblyStream = Application.GetResourceStream(sri,
                                                                                              new Uri(asm.Source,
                                                                                                      UriKind.Relative));
                            Assembly asmReflect = asm.Load(assemblyStream.Stream);
                            assemblies.Add(asmReflect);
                            byteMappings[asmReflect] = bytes;
                        }
                        externalAssemblies = (from p in manifest.ExternalParts
                                              where p is ExtensionPart
                                              select ((ExtensionPart)p).Source).ToList();
                    });
                if (result.IsCancelled)
                    return result;
                if (externalAssemblies.Count() > 0)
                {
                    result.CompletedSynchronously = false;
                    lock (result.InternalLock)
                        result.ToDownload = externalAssemblies.Count();
                }
                else
                {
                    Xap xap = new Xap(manifest, assemblies);
                    result.Result = xap;
                    (result.AsyncWaitHandle as ManualResetEvent).Set();
                    callback(result);
                    return result;
                }
                foreach (var uri in externalAssemblies)
                {
                    if (result.IsCancelled)
                        return result;
                    Uri realUri = uri;
                    if (!uri.IsAbsoluteUri)
                        realUri = new Uri(baseUri, uri);
                    WebRequest wr = WebRequest.Create(UiUtilities.ExecuteOnUiThread(() => SimpleWebClient.MakeAbsolute(realUri)));
                    SimpleWebClient wc = new SimpleWebClient(wr);
                    if (wr is HttpWebRequest)
                        ((HttpWebRequest)wr).AllowReadStreamBuffering = false;
                    wc.OpenReadCompleted += (sender, args) =>
                        {
                            try
                            {
                                Stream downloadResult = args.Result;
                                byte[] extBytes = ReadStream(downloadResult);
                                using (downloadResult = new MemoryStream(extBytes))
                                {
                                    foreach (
                                        string assemblyName in
                                            ZipUtilities.GetFilenames(downloadResult).Where(
                                                filename => filename.EndsWith(".dll")))
                                    {
                                        downloadResult.Seek(0, SeekOrigin.Begin);
                                        StreamResourceInfo externalSri = new StreamResourceInfo(downloadResult, null);
                                        StreamResourceInfo externalAssemblyStream =
                                            Application.GetResourceStream(externalSri,
                                                                          new Uri(assemblyName, UriKind.Relative));

                                        Assembly asmReflect =
                                            UiUtilities.ExecuteOnUiThread(
                                                () => new AssemblyPart().Load(externalAssemblyStream.Stream));
                                        assemblies.Add(asmReflect);
                                        byteMappings[asmReflect] = extBytes;
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                result.Error = e;
                                callback(result);
                                return;
                            }
                            lock (result.InternalLock)
                            {
                                result.ToDownload--;
                                if (result.ToDownload == 0)
                                {
                                    Xap xap = UiUtilities.ExecuteOnUiThread(() => new Xap(manifest, assemblies));
                                    result.Result = xap;
                                    (result.AsyncWaitHandle as ManualResetEvent).Set();
                                    callback(result);
                                }
                            }
                        };
                    result.AddClient(wc);
                    if (result.IsCancelled)
                        return result;
                    wc.OpenReadAsync();
                }
                return result;
            }
        }

        private static Deployment.Deployment GetManifest(Stream manifestStream)
        {
            StreamReader manifestReader = new StreamReader(manifestStream);
            string manifestXaml = manifestReader.ReadToEnd();
            manifestXaml = SLaBify(manifestXaml, "Deployment");
            manifestXaml = SLaBify(manifestXaml, "Icon");
            manifestXaml = SLaBify(manifestXaml, "IconCollection");
            manifestXaml = SLaBify(manifestXaml, "OutOfBrowserSettings");
            manifestXaml = SLaBify(manifestXaml, "WindowSettings");
            manifestXaml = SLaBify(manifestXaml, "SecuritySettings");
            manifestXaml = manifestXaml.Replace("xmlns=\"http://schemas.microsoft.com/client/2007/deployment\"",
                                                "xmlns=\"http://schemas.microsoft.com/client/2007/deployment\" xmlns:SLaB=\"clr-namespace:SLaB.Utilities.Xap.Deployment;assembly=SLaB.Utilities.Xap\"");
            return UiUtilities.ExecuteOnUiThread(() => (Deployment.Deployment)XamlReader.Load(manifestXaml));
        }

        private static byte[] ReadStream(Stream stream)
        {
            MemoryStream ms = new MemoryStream();
            stream.CopyTo(ms);
            return ms.ToArray();
        }

        private static string SLaBify(string xaml, string typeName)
        {
            xaml = xaml.Replace("<" + typeName, "<SLaB:" + typeName);
            xaml = xaml.Replace("</" + typeName, "</SLaB:" + typeName);
            return xaml;
        }




        /// <summary>
        ///   An AsyncResult used when loading Xaps.
        /// </summary>
        public class XapAsyncResult : IAsyncResult, INotifyPropertyChanged
        {
        #region Fields (6) 

            private readonly ObservableCollection<SimpleWebClient> _Clients;
            private readonly Dictionary<SimpleWebClient, DownloadProgressChangedEventArgs> _CurrentProgress;
            private double _Progress;
            private readonly ReadOnlyObservableCollection<SimpleWebClient> _ReadOnlyClients;
            private Xap _Result;
            private int _ToDownload;

        #endregion Fields 

        #region Constructors (1) 

            internal XapAsyncResult(object asyncState)
            {
                this.AsyncState = asyncState;
                this.AsyncWaitHandle = new ManualResetEvent(false);
                this.InternalLock = new object();
                this._Progress = 0;
                this.CompletedSynchronously = true;
                this._Clients = new ObservableCollection<SimpleWebClient>();
                this._ReadOnlyClients = new ReadOnlyObservableCollection<SimpleWebClient>(this._Clients);
                this._CurrentProgress = new Dictionary<SimpleWebClient, DownloadProgressChangedEventArgs>();
            }

        #endregion Constructors 

        #region Properties (7) 

            /// <summary>
            ///   Gets the set of active SimpleWebClients that were used to download the Xap and its dependencies.
            /// </summary>
            public ReadOnlyObservableCollection<SimpleWebClient> ActiveClients
            {
                get { return this._ReadOnlyClients; }
            }

            internal Exception Error { get; set; }

            internal object InternalLock { get; private set; }

            internal bool IsCancelled { get; set; }

            /// <summary>
            ///   Gets the download progress (in terms of bytes downloaded/known total size).
            /// </summary>
            public double Progress
            {
                get { return this._Progress; }
                internal set
                {
                    if (value != this._Progress)
                    {
                        this._Progress = value;
                        if (this.PropertyChanged != null)
                            this.PropertyChanged(this, new PropertyChangedEventArgs("Progress"));
                    }
                }
            }

            internal Xap Result
            {
                get { return this._Result; }
                set
                {
                    this._Result = value;
                    this.CheckCompleted();
                }
            }

            internal int ToDownload
            {
                get { return this._ToDownload; }
                set
                {
                    this._ToDownload = value;
                    this.CheckCompleted();
                }
            }

        #endregion Properties 

        #region Methods (4) 

        // Private Methods (3) 

            private void CheckCompleted()
            {
                if (this.IsCompleted)
                {
                    ((ManualResetEvent)this.AsyncWaitHandle).Set();
                }
            }

            private void ClientDownloadProgressChanged(object sender, DownloadProgressChangedEventArgs e)
            {
                this._CurrentProgress[sender as SimpleWebClient] = e;
                if (e.BytesReceived == e.TotalBytesToReceive)
                {
                    (sender as SimpleWebClient).DownloadProgressChanged -= this.ClientDownloadProgressChanged;
                }
                this.UpdateProgress();
            }

            private void UpdateProgress()
            {
                long total = (from client in this._Clients
                              where this._CurrentProgress.ContainsKey(client)
                              select this._CurrentProgress[client].TotalBytesToReceive).Sum();
                if (total == 0)
                    total = 1;
                long soFar = (from client in this._Clients
                              where this._CurrentProgress.ContainsKey(client)
                              select this._CurrentProgress[client].BytesReceived).Sum();
                this.Progress = 1.0 * soFar / total;
            }
        // Internal Methods (1) 

            internal void AddClient(SimpleWebClient client)
            {
                this._Clients.Add(client);
                client.DownloadProgressChanged += this.ClientDownloadProgressChanged;
            }

        #endregion Methods 



            #region IAsyncResult Members

            /// <summary>
            ///   Gets the AsyncState passed into BeginLoadXap.
            /// </summary>
            public object AsyncState { get; private set; }

            /// <summary>
            ///   Gets a WaitHandle that will be set when the operation completes.
            /// </summary>
            public WaitHandle AsyncWaitHandle { get; private set; }

            /// <summary>
            ///   Gets whether the operation completed synchronously.
            /// </summary>
            public bool CompletedSynchronously { get; internal set; }

            /// <summary>
            ///   Gets whether the operation has completed.
            /// </summary>
            public bool IsCompleted
            {
                get { return this.ToDownload == 0 && this.Result != null; }
            }

            #endregion

            #region INotifyPropertyChanged Members

            /// <summary>
            ///   An event that is raised when Progress changes.
            /// </summary>
            public event PropertyChangedEventHandler PropertyChanged;

            #endregion
        }
    }
}