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
using System.Collections.Generic;

namespace SLaB.Utilities.Xaml.Deserializer
{
    internal abstract class XamlNode
    {
        internal XamlNode()
        {
            Nodes = new List<XamlNode>();
        }
        internal XamlContext Context { get; set; }
        internal int Line { get; set; }
        internal int Offset { get; set; }
        internal string Uri { get; set; }
        internal List<XamlNode> Nodes { get; private set; }
        internal abstract NodeType NodeType { get; }
    }

    internal enum NodeType
    {
        StartMember,
        StartObject,
        EndMember,
        EndObject,
        Value,
    }
}
