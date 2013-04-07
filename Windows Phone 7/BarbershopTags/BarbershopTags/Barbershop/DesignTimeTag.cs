using System;
using System.Linq;
using System.Windows;
using System.Xml.Linq;

namespace BarbershopTags.Barbershop {
  public class DesignTimeTag : Tag {
    public DesignTimeTag() {
      var input = Application.GetResourceStream(new Uri("/BarbershopTags;component/Barbershop/DesignTimeData.xml", UriKind.Relative)).Stream;
      var doc = XDocument.Load(input);
      this.ParseFromXml(doc.Descendants("tag").First());
      this.AlternativeTitle = "Another title goes here...";
      this.Rating = 5;
    }
  }
}
