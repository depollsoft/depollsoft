#region Using Directives

using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Net;
using System.Windows.Markup;
using SLaB.Utilities;
using SLaB.Utilities.Xaml.Serializer;
using System.Threading;

#endregion

namespace SLaB.Offline
{
    public class SerializableHttpWebResponse : HttpWebResponse
    {
        private readonly SerializableWebResponseData _Data;
        private int _LoadedCount;
        private readonly AutoResetEvent _DataLoadWaitHandle;
        private Action<SerializableHttpWebResponse> _CompletedAction;

        public SerializableHttpWebResponse(HttpWebResponse source, Action<SerializableHttpWebResponse> completedAction = null)
        {
            _CompletedAction = completedAction;
            _DataLoadWaitHandle = new AutoResetEvent(false);
            _LoadedCount = 0;
            _Data = new SerializableWebResponseData();
            _Data.ContentLength = source.ContentLength;
            _Data.ContentType = source.ContentType;
            try
            {
                _Data.Cookies.AddRange(source.Cookies.Cast<Cookie>());
            }
            catch (Exception)
            {
            }
            if (source.SupportsHeaders)
            {
                foreach (string header in source.Headers.AllKeys)
                    _Data.Headers[header] = source.Headers[header];
            }
            _Data.Method = source.Method;
            _Data.ResponseUri = source.ResponseUri;
            _Data.StatusCode = source.StatusCode;
            _Data.StatusDescription = source.StatusDescription;
            _Data.SupportsHeaders = source.SupportsHeaders;
            _Data.Data = new byte[_Data.ContentLength];
            var responseStream = source.GetResponseStream();
            responseStream.BeginRead(_Data.Data, 0, (int)ContentLength, ReadBytes, responseStream);
        }

        private void ReadBytes(IAsyncResult result)
        {
            Stream responseStream = result.AsyncState as Stream;
            int readCount = responseStream.EndRead(result);
            if (readCount == 0)
            {
                responseStream.Close();
                _CompletedAction.Raise(this);
                return;
            }
            _LoadedCount += readCount;
            _DataLoadWaitHandle.Set();
            responseStream.BeginRead(_Data.Data, _LoadedCount, (int)ContentLength - _LoadedCount, ReadBytes, responseStream);
        }

        private SerializableHttpWebResponse(SerializableWebResponseData data)
        {
            this._Data = data;
            this._DataLoadWaitHandle = new AutoResetEvent(false);
            this._LoadedCount = (int)this._Data.ContentLength;
        }

        public override long ContentLength
        {
            get { return _Data.ContentLength; }
        }

        public override string ContentType
        {
            get { return _Data.ContentType; }
        }

        public override CookieCollection Cookies
        {
            get
            {
                var cc = new CookieCollection();
                foreach (Cookie cookie in _Data.Cookies)
                    cc.Add(cookie);
                return cc;
            }
        }

        public override WebHeaderCollection Headers
        {
            get
            {
                var whc = new WebHeaderCollection();
                foreach (string key in _Data.Headers.Keys)
                    whc[key] = _Data.Headers[key];
                return whc;
            }
        }

        public override string Method
        {
            get { return _Data.Method; }
        }

        public override Uri ResponseUri
        {
            get { return _Data.ResponseUri; }
        }

        public override HttpStatusCode StatusCode
        {
            get { return _Data.StatusCode; }
        }

        public override string StatusDescription
        {
            get { return _Data.StatusDescription; }
        }

        public override bool SupportsHeaders
        {
            get { return _Data.SupportsHeaders; }
        }

        public bool AwaitingUpdate
        {
            get { return _Data.AwaitingUpdate; }
        }

        public override void Close()
        {
        }

        public static HttpWebResponse Deserialize(Stream inputStream)
        {
            var br = new BinaryReader(inputStream);
            int length = br.ReadInt32();
            byte[] data = br.ReadBytes(length);
            SerializableWebResponseData swrd = UiUtilities.ExecuteOnUiThread(() => (SerializableWebResponseData)XamlReader.Load(br.ReadString()));
            swrd.Data = data;
            return new SerializableHttpWebResponse(swrd);
        }

        public override Stream GetResponseStream()
        {
            return new DataStream(this);
        }

        public void Serialize(Stream outputStream)
        {
            var bw = new BinaryWriter(outputStream);
            bw.Write(_Data.Data.Length);
            bw.Write(_Data.Data);
            var xs = new XamlSerializer();
            string serialized = xs.Serialize(_Data);
            bw.Write(serialized);
        }

        private class DataStream : Stream
        {
            private SerializableHttpWebResponse _Source;
            internal DataStream(SerializableHttpWebResponse source)
            {
                _Source = source;
                Position = 0;
            }
            public override bool CanRead
            {
                get { return true; }
            }

            public override bool CanSeek
            {
                get { return true; }
            }

            public override bool CanWrite
            {
                get { return false; }
            }

            public override void Flush()
            {
                throw new NotSupportedException();
            }

            public override long Length
            {
                get { return _Source.ContentLength; }
            }

            public override long Position { get; set; }

            public override int Read(byte[] buffer, int offset, int count)
            {
                if (Position >= _Source._LoadedCount)
                    _Source._DataLoadWaitHandle.WaitOne();
                count = (int)Math.Min(Math.Min(count, Length - Position), _Source._LoadedCount - Position);
                Array.Copy(_Source._Data.Data, (int)Position, buffer, offset, count);
                return count;
            }

            public override long Seek(long offset, SeekOrigin origin)
            {
                switch (origin)
                {
                    case SeekOrigin.Begin:
                        Position = offset;
                        break;
                    case SeekOrigin.Current:
                        Position += offset;
                        break;
                    case SeekOrigin.End:
                        Position = Length - offset;
                        break;
                }
                return Position;
            }

            public override void SetLength(long value)
            {
                throw new NotSupportedException();
            }

            public override void Write(byte[] buffer, int offset, int count)
            {
                throw new NotSupportedException();
            }
        }
    }

    [EditorBrowsable(EditorBrowsableState.Never)]
    public class SerializableWebResponseData
    {
        public SerializableWebResponseData()
        {
            Cookies = new SerializableCookieCollection();
            Headers = new SerializableWebHeaderCollection();
        }

        [TypeConverter(typeof(LongTypeConverter))]
        public long ContentLength { get; set; }

        public string ContentType { get; set; }
        public SerializableCookieCollection Cookies { get; set; }
        public byte[] Data { get; set; }

        public SerializableWebHeaderCollection Headers { get; set; }
        public string Method { get; set; }
        public Uri ResponseUri { get; set; }
        public HttpStatusCode StatusCode { get; set; }
        public string StatusDescription { get; set; }
        public bool SupportsHeaders { get; set; }
        public bool AwaitingUpdate { get; set; }

        [EditorBrowsable(EditorBrowsableState.Never)]
        public bool ShouldSerializeData()
        {
            return false;
        }
    }

    public class LongTypeConverter : TypeConverter
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
            return long.Parse((string)value);
        }

        public override object ConvertTo(ITypeDescriptorContext context,
                                         CultureInfo culture,
                                         object value,
                                         Type destinationType)
        {
            return value.ToString();
        }
    }

    public class SerializableWebHeaderCollection : Dictionary<string, string>
    {
    }

    public class SerializableCookieCollection : List<Cookie>
    {
    }
}