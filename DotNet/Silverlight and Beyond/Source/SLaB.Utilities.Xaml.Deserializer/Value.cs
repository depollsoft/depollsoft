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

namespace SLaB.Utilities.Xaml.Deserializer
{
    internal class Value : XamlNode
    {
        internal string StringValue { get; set; }
        internal override NodeType NodeType
        {
            get { return NodeType.Value; }
        }
        public override string ToString()
        {
            return "Value: " + StringValue;
        }
    }
}
