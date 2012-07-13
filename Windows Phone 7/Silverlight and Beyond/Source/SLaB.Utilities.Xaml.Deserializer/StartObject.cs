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
    internal class StartObject : XamlNode
    {
        internal string ObjectTypeName { get; set; }
        internal string Prefix { get; set; }
        internal string PrefixedObjectTypeName
        {
            get
            {
                return string.IsNullOrEmpty(Prefix) ? ObjectTypeName : (Prefix + ':' + ObjectTypeName);
            }
        }
        internal override NodeType NodeType
        {
            get { return NodeType.StartObject; }
        }

        public override string ToString()
        {
            return "StartObject: " + Prefix + ":" + ObjectTypeName;
        }
    }
}
