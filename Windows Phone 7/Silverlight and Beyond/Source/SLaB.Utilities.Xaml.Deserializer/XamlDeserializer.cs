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
using System.Xml;
using System.IO;
using System.Collections.Generic;
using System.Windows.Markup;
using System.Linq;
using System.Diagnostics;
using System.ComponentModel;

namespace SLaB.Utilities.Xaml.Deserializer
{
    public class XamlDeserializer
    {
        internal XamlDeserializer()
        {

        }

        private Type ResolveType(string typeName, XamlContext context)
        {
            return null;
        }


        private IEnumerable<XamlNode> GetNodestream(string str, string uri)
        {
            List<XamlNode> nodeStream = new List<XamlNode>();
            Stack<XamlNode> nodeStack = new Stack<XamlNode>();
            Stack<XamlContext> contextStack = new Stack<XamlContext>();
            contextStack.Push(new XamlContext());
            using (var reader = XmlReader.Create(new StringReader(str)))
            {
                IXmlLineInfo lineInfo = (IXmlLineInfo)reader;
                while (reader.Read())
                {
                    HandleXmlNode(uri, nodeStream, nodeStack, contextStack, reader, lineInfo);
                }
            }
            return nodeStream;
        }

        private static void HandleXmlNode(string uri, List<XamlNode> nodeStream, Stack<XamlNode> nodeStack, Stack<XamlContext> contextStack, XmlReader reader, IXmlLineInfo lineInfo)
        {
            switch (reader.NodeType)
            {
                case XmlNodeType.Element:
                    {
                        contextStack.Push(new XamlContext(contextStack.Peek()));
                        // If the element contains a ".", it's a property.
                        bool isProperty = reader.Name.Contains(".");
                        if (!isProperty)
                        {
                            var start = new StartObject { ObjectTypeName = reader.LocalName, Prefix = reader.Prefix };
                            nodeStack.Push(start);
                            FinalizeNode(start, contextStack.Peek(), nodeStream, lineInfo, uri);
                        }
                        else
                        {
                            var start = new StartMember { Name = reader.LocalName, Prefix = reader.Prefix };
                            nodeStack.Push(start);
                            FinalizeNode(start, contextStack.Peek(), nodeStream, lineInfo, uri);
                        }
                        while (reader.MoveToNextAttribute())
                            HandleXmlNode(uri, nodeStream, nodeStack, contextStack, reader, lineInfo);
                        if (reader.IsEmptyElement)
                            goto case XmlNodeType.EndElement;
                    }
                    break;
                case XmlNodeType.Attribute:
                    {
                        if (reader.Prefix.Equals("xmlns"))
                        {
                            contextStack.Peek().UriMappings[reader.LocalName] = reader.Value;
                        }
                        else
                        {
                            var start = new StartMember { Name = reader.LocalName, Prefix = reader.Prefix };
                            var value = new Value { StringValue = reader.Value };
                            var end = new EndMember();
                            start.Nodes.Add(start);
                            start.Nodes.Add(value);
                            start.Nodes.Add(end);
                            FinalizeNode(start, contextStack.Peek(), nodeStream, lineInfo, uri);
                            FinalizeNode(value, contextStack.Peek(), nodeStream, lineInfo, uri);
                            FinalizeNode(end, contextStack.Peek(), nodeStream, lineInfo, uri);
                        }
                    }
                    break;
                case XmlNodeType.SignificantWhitespace:
                case XmlNodeType.Text:
                    {
                        var value = new Value { StringValue = reader.Value };
                        FinalizeNode(value, contextStack.Peek(), nodeStream, lineInfo, uri);
                    }
                    break;
                case XmlNodeType.EndElement:
                    {
                        int startIndex = nodeStream.Count;
                        var startNode = nodeStack.Pop();
                        XamlNode end = null;
                        switch (startNode.NodeType)
                        {
                            case NodeType.StartObject:
                                end = new EndObject();
                                break;
                            case NodeType.StartMember:
                                end = new EndMember();
                                break;
                        }
                        if (end != null)
                            FinalizeNode(end, contextStack.Peek(), nodeStream, lineInfo, uri);
                        while (startIndex >= 0 && nodeStream[--startIndex] != startNode) ;
                        startNode.Nodes.AddRange(nodeStream.Skip(startIndex));
                        contextStack.Pop();
                    }
                    break;
                #region Ignored nodes
                case XmlNodeType.Whitespace:
                case XmlNodeType.Comment:
                case XmlNodeType.Document:
                case XmlNodeType.DocumentFragment:
                case XmlNodeType.DocumentType:
                    break;
                case XmlNodeType.EntityReference:
                case XmlNodeType.EndEntity:
                case XmlNodeType.Entity:
                    throw new NotSupportedException("Entities are not supported in XAML");
                case XmlNodeType.Notation:
                    throw new NotSupportedException("Notations are not supported in XAML");
                case XmlNodeType.ProcessingInstruction:
                    throw new NotSupportedException("Processing instructions are not supported in XAML");
                case XmlNodeType.XmlDeclaration:
                    throw new NotSupportedException("Xml Declarations are not supported in XAML");
                case XmlNodeType.CDATA:
                    throw new NotSupportedException("CDATA not yet supported");
                #endregion
            }
        }

        private static void FinalizeNode(XamlNode node, XamlContext context, List<XamlNode> nodeStream, IXmlLineInfo lineInfo, string uri)
        {
            node.Line = lineInfo.LineNumber;
            node.Offset = lineInfo.LinePosition;
            node.Context = context;
            node.Uri = uri;
            nodeStream.Add(node);
        }

        private static object ProcessNodestream(IEnumerable<XamlNode> nodes, object root, string uri)
        {
            Stack<List<object>> creationStack = new Stack<List<object>>();
            Stack<XamlNode> nodeStack = new Stack<XamlNode>();
            bool isFirstObject = true;
            creationStack.Push(new List<object>());
            if (root != null)
                creationStack.Peek().Add(root);
            foreach (var node in nodes)
            {
                switch (node.NodeType)
                {
                    case NodeType.StartObject:
                        {
                            if (isFirstObject)
                                continue;
                            StartObject so = (StartObject)node;
                            var toCreate = so.Context.Resolve(so.PrefixedObjectTypeName);
                            var created = Activator.CreateInstance(toCreate);
                            creationStack.Peek().Add(created);
                            var ini = created as ISupportInitialize;
                            if (ini != null)
                                ini.BeginInit();
                        }
                        break;
                    case NodeType.EndObject:
                        break;
                    case NodeType.StartMember:
                        break;
                    case NodeType.EndMember:
                        break;
                    case NodeType.Value:
                        break;
                }
                isFirstObject = false;
            }
            return root;
        }

        private object Deserialize(string str, object root = null, string uri = null)
        {
            var nodeStream = GetNodestream(str, uri);
            foreach (var item in nodeStream)
                Debug.WriteLine(item);
            return ProcessNodestream(nodeStream, root, uri);
        }
        public static object Deserialize(string str)
        {
            return new XamlDeserializer().Deserialize(str, null);
        }
    }
}
