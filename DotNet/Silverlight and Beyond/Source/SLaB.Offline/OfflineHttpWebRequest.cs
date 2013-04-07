#region Using Directives

using System;
using System.IO;
using System.IO.IsolatedStorage;
using System.Net;
using System.Threading;

#endregion

namespace SLaB.Offline
{
    internal class OfflineHttpWebRequest : HttpWebRequest
    {
        private readonly HttpWebRequest _BaseRequest;
        private readonly OfflinedFile _OfflinedFile;

        internal OfflineHttpWebRequest(HttpWebRequest baseRequest)
        {
            this._BaseRequest = baseRequest;
            this._OfflinedFile = OfflineManager.Current.GetOfflinedFile(this._BaseRequest.RequestUri);
        }

        public override bool AllowReadStreamBuffering
        {
            get { return this._BaseRequest.AllowReadStreamBuffering; }
            set { this._BaseRequest.AllowReadStreamBuffering = value; }
        }

        public override bool AllowWriteStreamBuffering
        {
            get { return this._BaseRequest.AllowWriteStreamBuffering; }
            set { this._BaseRequest.AllowWriteStreamBuffering = value; }
        }

        public override long ContentLength
        {
            get { return this._BaseRequest.ContentLength; }
            set { this._BaseRequest.ContentLength = value; }
        }

        public override string ContentType
        {
            get { return this._BaseRequest.ContentType; }
            set { this._BaseRequest.ContentType = value; }
        }

        public override CookieContainer CookieContainer
        {
            get { return this._BaseRequest.CookieContainer; }
            set { this._BaseRequest.CookieContainer = value; }
        }

        public override IWebRequestCreate CreatorInstance
        {
            get { return this._BaseRequest.CreatorInstance; }
        }

        public override ICredentials Credentials
        {
            get { return this._BaseRequest.Credentials; }
            set { this._BaseRequest.Credentials = value; }
        }

        public override bool HaveResponse
        {
            get { return this._BaseRequest.HaveResponse; }
        }

        public override WebHeaderCollection Headers
        {
            get { return this._BaseRequest.Headers; }
            set { this._BaseRequest.Headers = value; }
        }

        public override string Method
        {
            get { return this._BaseRequest.Method; }
            set { this._BaseRequest.Method = value; }
        }

        public override Uri RequestUri
        {
            get { return this._BaseRequest.RequestUri; }
        }

        public override bool SupportsCookieContainer
        {
            get { return this._BaseRequest.SupportsCookieContainer; }
        }

        public override bool UseDefaultCredentials
        {
            get { return this._BaseRequest.UseDefaultCredentials; }
            set { this._BaseRequest.UseDefaultCredentials = value; }
        }

        private bool ShouldLoadFromIsolatedStorage
        {
            get { return IsolatedStorageFile.IsEnabled && IsolatedStorageFile.GetUserStoreForSite().FileExists(this._OfflinedFile.FileName); }
        }

        public override void Abort()
        {
            this._BaseRequest.Abort();
        }

        public override IAsyncResult BeginGetRequestStream(AsyncCallback callback, object state)
        {
            return this._BaseRequest.BeginGetRequestStream(callback, state);
        }

        public override IAsyncResult BeginGetResponse(AsyncCallback callback, object state)
        {
            if (ShouldLoadFromIsolatedStorage)
            {
                var data = OfflineManager.Current.GetFileData(this._OfflinedFile.FileName);
                var response = SerializableHttpWebResponse.Deserialize(data);
                var result = new AsyncResult(callback, state);
                if (!((SerializableHttpWebResponse)response).AwaitingUpdate)
                {
                    result.Response = response;
                    result.Finish(true);
                    return result;
                }
            }
            var finalResult = new AsyncResult(callback, state);
            _BaseRequest.BeginGetResponse(res =>
                {
                    var response = (HttpWebResponse)_BaseRequest.EndGetResponse(res);
                    SerializableHttpWebResponse realResponse = null;
                    realResponse = new SerializableHttpWebResponse(response, (r) =>
                    {
                        finalResult.Response = r;
                        if (_OfflinedFile.ShouldSaveToIsolatedStorage)
                        {
                            MemoryStream ms = new MemoryStream();
                            r.Serialize(ms);
                            var arr = ms.ToArray();
                            OfflineManager.Current.EnqueueFileWrite(this._OfflinedFile.FileName, arr);
                        }
                        finalResult.Finish(false);
                    });
                }, state);
            return finalResult;
        }

        public override Stream EndGetRequestStream(IAsyncResult asyncResult)
        {
            return this._BaseRequest.EndGetRequestStream(asyncResult);
        }

        public override WebResponse EndGetResponse(IAsyncResult asyncResult)
        {
            return ((AsyncResult)asyncResult).Response;
        }

        public override bool Equals(object obj)
        {
            return this._BaseRequest.Equals(obj);
        }

        public override int GetHashCode()
        {
            return this._BaseRequest.GetHashCode();
        }

        public override string ToString()
        {
            return this._BaseRequest.ToString();
        }

        private class AsyncResult : IAsyncResult
        {
            private readonly AsyncCallback _Callback;

            public AsyncResult(AsyncCallback callback, object asyncState)
            {
                this.AsyncWaitHandle = new ManualResetEvent(false);
                this._Callback = callback;
                this.AsyncState = asyncState;
            }

            #region IAsyncResult Members

            public object AsyncState { get; private set; }

            public WaitHandle AsyncWaitHandle { get; private set; }

            public bool CompletedSynchronously { get; private set; }

            public bool IsCompleted { get; private set; }

            #endregion

            internal WebResponse Response { get; set; }

            internal void Finish(bool completedSync)
            {
                this.IsCompleted = true;
                this.CompletedSynchronously = completedSync;
                this._Callback(this);
            }
        }
    }
}