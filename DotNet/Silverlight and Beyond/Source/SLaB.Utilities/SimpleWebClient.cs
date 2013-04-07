using System;
using System.ComponentModel;
using System.IO;
using System.Net;
using System.Windows;

namespace SLaB.Utilities
{
    /// <summary>
    /// Mimics the behavior of the WebClient class but works with any WebRequest (allowing progress information,
    /// etc. to be transmitted).
    /// </summary>
    public sealed class SimpleWebClient
    {

        private bool _Cancelled;
        private byte[] _Data;
        private int _ReadSoFar;
        private long _TotalBytes;



        /// <summary>
        /// Initializes a new instance of the <see cref="SimpleWebClient"/> class.
        /// </summary>
        /// <param name="request">The request.</param>
        public SimpleWebClient(WebRequest request)
        {
            Request = request;
        }



        /// <summary>
        /// Gets the request.
        /// </summary>
        /// <value>The request.</value>
        public WebRequest Request { get; private set; }




        /// <summary>
        /// Occurs when download progress has changed.
        /// </summary>
        public event EventHandler<DownloadProgressChangedEventArgs> DownloadProgressChanged;

        /// <summary>
        /// Occurs when download has completed.
        /// </summary>
        public event EventHandler<OpenReadCompletedEventArgs> OpenReadCompleted;




        /// <summary>
        /// Cancels the operation.
        /// </summary>
        public void CancelAsync()
        {
            _Cancelled = true;
            Request.Abort();
            OpenReadCompleted.RaiseOnUiThread(this, new OpenReadCompletedEventArgs(null, new Exception("Download did not complete"), false));
        }

        /// <summary>
        /// Makes the Uri absolute.
        /// </summary>
        /// <param name="uri">The URI.</param>
        /// <returns></returns>
        public static Uri MakeAbsolute(Uri uri)
        {
            if (!uri.IsAbsoluteUri)
                uri = new Uri(Application.Current.Host.Source, uri);
            return uri;
        }

        /// <summary>
        /// Opens the WebRequest for reading asynchronously.
        /// </summary>
        public void OpenReadAsync()
        {
            Request.BeginGetResponse((result) =>
            {
                var response = Request.EndGetResponse(result);
                _TotalBytes = response.ContentLength;
                _ReadSoFar = 0;
                _Data = new byte[_TotalBytes];
                Stream responseStream = response.GetResponseStream();
                DownloadProgressChanged.RaiseOnUiThread(this, new DownloadProgressChangedEventArgs(0, 0, _TotalBytes));
                responseStream.BeginRead(_Data, 0, (int)_TotalBytes, ReadBytes, responseStream);
            }, null);
        }

        private void ReadBytes(IAsyncResult result)
        {
            try
            {
                Stream responseStream = (Stream)result.AsyncState;
                if (_Cancelled)
                {
                    responseStream.Close();
                    return;
                }
                int amountRead = responseStream.EndRead(result);
                if (amountRead == 0)
                {
                    responseStream.Close();
                    if (_ReadSoFar != _TotalBytes)
                    {
                        OpenReadCompleted.RaiseOnUiThread(this, new OpenReadCompletedEventArgs(new MemoryStream(_Data, 0, _ReadSoFar), new Exception("Download did not complete"), false));
                        return;
                    }
                }
                _ReadSoFar += amountRead;
                DownloadProgressChanged.RaiseOnUiThread(this, new DownloadProgressChangedEventArgs((int)(1.0 * _ReadSoFar / Math.Min(Math.Max(_ReadSoFar, 1), _TotalBytes)), _ReadSoFar, _TotalBytes));
                if (_TotalBytes == _ReadSoFar)
                    OpenReadCompleted.RaiseOnUiThread(this, new OpenReadCompletedEventArgs(new MemoryStream(_Data), null, false));
                else
                    responseStream.BeginRead(_Data, _ReadSoFar, (int)(_TotalBytes - _ReadSoFar), ReadBytes, responseStream);
            }
            catch (Exception e)
            {
                OpenReadCompleted.RaiseOnUiThread(this, new OpenReadCompletedEventArgs(new MemoryStream(_Data, 0, _ReadSoFar), e, false));
            }
        }
    }

    /// <summary>
    /// Represents an OpenRead completion.
    /// </summary>
    public class OpenReadCompletedEventArgs : AsyncCompletedEventArgs
    {

        internal OpenReadCompletedEventArgs(Stream result, Exception error, bool cancelled)
            : base(error, cancelled, null)
        {
            Result = result;
        }



        /// <summary>
        /// Gets the result.
        /// </summary>
        /// <value>The result.</value>
        public Stream Result { get; private set; }
    }

    /// <summary>
    /// Represents download progress changing.
    /// </summary>
    public class DownloadProgressChangedEventArgs : ProgressChangedEventArgs
    {

        internal DownloadProgressChangedEventArgs(int progressPercentage, long bytesReceived, long totalBytesToReceive)
            : base(progressPercentage, null)
        {
            BytesReceived = bytesReceived;
            TotalBytesToReceive = totalBytesToReceive;
        }



        /// <summary>
        /// Gets or the number of bytes received.
        /// </summary>
        /// <value>The bytes received.</value>
        public long BytesReceived { get; private set; }

        /// <summary>
        /// Gets or sets the total bytes that will eventually be received.
        /// </summary>
        /// <value>The total bytes to receive.</value>
        public long TotalBytesToReceive { get; private set; }
    }
}
