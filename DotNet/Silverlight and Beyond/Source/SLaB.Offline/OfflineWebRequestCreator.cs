#region Using Directives

using System;
using System.Net;
using System.Net.Browser;

#endregion

namespace SLaB.Offline
{
    public class OfflineWebRequestCreator : IWebRequestCreate
    {
        private readonly IWebRequestCreate _Creator;

        static OfflineWebRequestCreator()
        {
            ClientHttp = new OfflineWebRequestCreator(WebRequestCreator.ClientHttp);
            BrowserHttp = new OfflineWebRequestCreator(WebRequestCreator.BrowserHttp);
        }

        public OfflineWebRequestCreator(IWebRequestCreate baseCreator)
        {
            this._Creator = baseCreator;
        }

        #region IWebRequestCreate Members

        public WebRequest Create(Uri uri)
        {
            return new OfflineHttpWebRequest((HttpWebRequest)this._Creator.Create(uri));
        }

        #endregion

        public static IWebRequestCreate BrowserHttp { get; private set; }
        public static IWebRequestCreate ClientHttp { get; private set; }
    }
}