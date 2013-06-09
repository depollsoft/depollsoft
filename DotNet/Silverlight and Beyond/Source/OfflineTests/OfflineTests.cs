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
using Microsoft.Silverlight.Testing;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SLaB.Offline;
using System.IO;
using System.Net.Browser;

namespace OfflineTests
{
    [TestClass]
    public class OfflineTests : SilverlightTest
    {
        [TestMethod]
        [Asynchronous]
        public void TestWebResponseSerialization()
        {
            WebRequest.RegisterPrefix("http://", WebRequestCreator.ClientHttp);
            var wr = (HttpWebRequest)WebRequest.Create(new Uri(Application.Current.Host.Source, "../OfflineTestsTestPage.html"));
            wr.CookieContainer = new CookieContainer();
            wr.BeginGetResponse(res =>
                {
                    var response = wr.EndGetResponse(res);
                    var sr = new SerializableHttpWebResponse(response as HttpWebResponse);
                    MemoryStream ms = new MemoryStream();
                    sr.Serialize(ms);
                    ms.Seek(0, SeekOrigin.Begin);
                    var dResponse = SerializableHttpWebResponse.Deserialize(ms);
                    Assert.AreEqual(response.ContentLength, dResponse.ContentLength);
                    Assert.AreEqual(response.ContentType, dResponse.ContentType);
                    Assert.AreEqual(response.ResponseUri, dResponse.ResponseUri);
                    Assert.AreEqual(response.SupportsHeaders, dResponse.SupportsHeaders);
                    Stream rs1 = response.GetResponseStream();
                    Stream rs2 = dResponse.GetResponseStream();
                    int cur1, cur2;
                    do
                    {
                        cur1 = rs1.ReadByte();
                        cur2 = rs2.ReadByte();
                        Assert.AreEqual(cur1, cur2);
                    } while (cur1 != -1 && cur2 != -1);
                    EnqueueTestComplete();
                }, null);
        }
    }
}