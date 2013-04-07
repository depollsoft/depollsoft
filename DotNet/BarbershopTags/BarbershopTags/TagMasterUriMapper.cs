using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Navigation;
using Windows.Foundation;

namespace BarbershopTags {
  class TagMasterUriMapper : UriMapperBase {
    private IDictionary<string, string> ParseQueryString(string queryString) {
      var dict = new Dictionary<string, string>();
      var pairs = queryString.Split('&');
      foreach (var pair in pairs) {
        var parts = pair.Split(new[] { '=' }, 2);
        dict[parts[0]] = parts.Length == 2 ? HttpUtility.UrlDecode(parts[1]) : null;
      }
      return dict;
    }
    public override Uri MapUri(Uri uri) {
      var rootedUri = uri.IsAbsoluteUri ? uri : new Uri(new Uri("dummy://dummy", UriKind.Absolute), uri);
      if (rootedUri.PathAndQuery.StartsWith("/Protocol")) {
        var qs = ParseQueryString(rootedUri.Query.Substring(1));
        var encodedUri = qs["encodedLaunchUri"];
        var decodedUri = new Uri(HttpUtility.UrlDecode(encodedUri), UriKind.Absolute);
        if (decodedUri.PathAndQuery.StartsWith("/tag?")) {
          return new Uri("/TagPresenterPage.xaml" + decodedUri.Query, UriKind.Relative);
        }
      }
      return uri;
    }
  }
}
