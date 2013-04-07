using System.Windows.Markup;

namespace BarbershopTags {
  [ContentProperty("Value")]
  public class KeyValue {
    public object Key { get; set; }
    public object Value { get; set; }
    public override string ToString() {
      return string.Format("{0}", Key);
    }
  }
}
