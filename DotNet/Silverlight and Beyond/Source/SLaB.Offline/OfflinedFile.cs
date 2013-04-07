#region Using Directives

using System;
using System.IO;
using System.IO.IsolatedStorage;
using System.Net;
using System.Net.Browser;
using System.Windows;
using System.Windows.Markup;
using System.ComponentModel;
using System.Globalization;
using System.Linq;
using SLaB.Utilities;

#endregion

namespace SLaB.Offline
{
    [ContentProperty("Uri")]
    public class OfflinedFile : ISupportInitialize
    {
        static OfflinedFile()
        {
            if (IsolatedStorageFile.IsEnabled && !IsolatedStorageFile.GetUserStoreForSite().DirectoryExists(FolderName))
                IsolatedStorageFile.GetUserStoreForSite().CreateDirectory(FolderName);
        }
        public OfflinedFile()
        {
            _InstallMode = Offline.InstallMode.OutOfBrowser | Offline.InstallMode.InBrowser;
            _WebRequestCreator = System.Net.Browser.WebRequestCreator.BrowserHttp;
        }
        private string _FileName;
        private Uri _Uri;
        private bool _Locked;
        private InstallMode _InstallMode;
        private UpdatePolicy _UpdatePolicy;
        private IWebRequestCreate _WebRequestCreator;
        [TypeConverter(typeof(WebRequestCreatorConverter))]
        public IWebRequestCreate WebRequestCreator
        {
            get
            {
                return _WebRequestCreator;
            }
            set
            {
                if (_Locked)
                    throw new NotSupportedException();
                _WebRequestCreator = value;
            }
        }
        public InstallMode InstallMode
        {
            get
            {
                return _InstallMode;
            }
            set
            {
                if (_Locked)
                    throw new NotSupportedException();
                _InstallMode = value;
            }
        }
        public UpdatePolicy UpdatePolicy
        {
            get
            {
                return _UpdatePolicy;
            }
            set
            {
                if (_Locked)
                    throw new NotSupportedException();
                _UpdatePolicy = value;
            }
        }
        public Uri Uri
        {
            get
            {
                return _Uri;
            }
            set
            {
                if (_Locked)
                    throw new NotSupportedException();
                if (value != null && !value.IsAbsoluteUri)
                    _Uri = new Uri(Application.Current.Host.Source, value);
                else
                    _Uri = value;
            }
        }
        private static readonly string FolderName = "SLaB.Offline";
        private static readonly string DoublePath = "" + Path.DirectorySeparatorChar + Path.DirectorySeparatorChar;
        private static readonly string SinglePath = "" + Path.DirectorySeparatorChar;


        public string FileName
        {
            get
            {
                if (this._FileName != null)
                    return this._FileName;
                string fn = this.Uri.ToString().ToLowerInvariant();
                foreach (char ch in Path.GetInvalidPathChars().Concat(new char[] { Path.VolumeSeparatorChar, Path.PathSeparator, Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar }))
                    fn = fn.Replace(ch, Path.DirectorySeparatorChar);
                while (fn.Contains(DoublePath))
                    fn = fn.Replace(DoublePath, SinglePath);
                fn = FolderName + Path.DirectorySeparatorChar + fn;
                this._FileName = fn;
                return fn;
            }
        }

        internal bool ShouldSaveToIsolatedStorage
        {
            get
            {
                if (InstallMode == InstallMode.Never)
                    return false;
                if (InstallMode.HasFlag(InstallMode.InBrowser) && !UiUtilities.ExecuteOnUiThread(() => Application.Current.IsRunningOutOfBrowser))
                    return true;
                if (InstallMode.HasFlag(InstallMode.OutOfBrowser) && UiUtilities.ExecuteOnUiThread(() => Application.Current.InstallState == InstallState.Installed))
                    return true;
                return false;
            }
        }

        public override bool Equals(object obj)
        {
            return this.Uri.Equals(((OfflinedFile)obj).Uri);
        }

        public override int GetHashCode()
        {
            return this.Uri.GetHashCode();
        }

        public override string ToString()
        {
            return this.Uri.ToString();
        }

        void ISupportInitialize.BeginInit()
        {
        }

        void ISupportInitialize.EndInit()
        {
            _Locked = true;
        }
    }
    public class WebRequestCreatorConverter : TypeConverter
    {
        public override bool CanConvertFrom(ITypeDescriptorContext context, Type sourceType)
        {
            return sourceType.Equals(typeof(string));
        }
        public override bool CanConvertTo(ITypeDescriptorContext context, Type destinationType)
        {
            return destinationType.Equals(typeof(string));
        }
        public override object ConvertFrom(ITypeDescriptorContext context, CultureInfo culture, object value)
        {
            switch (value as string)
            {
                case "Client":
                    return System.Net.Browser.WebRequestCreator.ClientHttp;
                case "Browser":
                    return System.Net.Browser.WebRequestCreator.BrowserHttp;
            }
            throw new ArgumentException();
        }
        public override object ConvertTo(ITypeDescriptorContext context, CultureInfo culture, object value, Type destinationType)
        {
            if (value == System.Net.Browser.WebRequestCreator.BrowserHttp)
                return "Browser";
            if (value == System.Net.Browser.WebRequestCreator.ClientHttp)
                return "Client";
            throw new ArgumentException();
        }
    }
}