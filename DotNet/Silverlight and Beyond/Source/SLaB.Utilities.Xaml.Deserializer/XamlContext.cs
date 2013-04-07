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
using System.Reflection;
using System.Windows.Markup;
using System.Linq;
using System.ComponentModel;

namespace SLaB.Utilities.Xaml.Deserializer
{
    internal class XamlContext : ITypeDescriptorContext, IXamlTypeResolver
    {
        private class TypeResolver
        {
            internal TypeResolver()
            {
                _FoundTypes = new Dictionary<string, Type>();
            }
            internal string Namespace { get; set; }
            internal Assembly Assembly { get; set; }
            private Dictionary<string, Type> _FoundTypes;
            internal Type ResolveType(string name)
            {
                Type foundType = null;
                if (!_FoundTypes.TryGetValue(name, out foundType))
                    foundType = _FoundTypes[name] = Assembly.GetType(Namespace + '.' + name, true);
                return foundType;
            }
        }
        private static ISet<Assembly> _DiscoveredAssemblies;
        private static IDictionary<string, List<TypeResolver>> _XmlnsMappings;
        static XamlContext()
        {
            _DiscoveredAssemblies = new HashSet<Assembly>();
            _XmlnsMappings = new Dictionary<string, List<TypeResolver>>();
            foreach (var part in Deployment.Current.Parts)
            {
                RegisterAssembly(part.Load(Application.GetResourceStream(new Uri(part.Source, UriKind.Relative)).Stream));
            }
        }
        internal XamlContext()
        {
            UriMappings = new Dictionary<string, string>();
        }
        internal XamlContext(XamlContext toCopy)
        {
            UriMappings = new Dictionary<string, string>(toCopy.UriMappings);
        }
        internal IDictionary<string, string> UriMappings { get; private set; }
        internal Type Resolve(string type)
        {
            var parts = type.Split(':');
            string prefix = parts.Length == 2 ? parts[0] : "";
            string typeName = parts.Last();
            string ns = UriMappings[prefix];
            return FindType(ns, typeName);
        }
        internal static Type FindType(string xmlns, string typeName)
        {
            foreach (var resolver in _XmlnsMappings[xmlns])
            {
                var type = resolver.ResolveType(typeName);
                if (type != null)
                    return type;
            }
            return null;
        }
        internal static void RegisterAssembly(Assembly asm)
        {
            if (_DiscoveredAssemblies.Contains(asm))
                return;
            var atts = asm.GetCustomAttributes(typeof(XmlnsDefinitionAttribute), false).Cast<XmlnsDefinitionAttribute>();
            foreach (var att in atts)
            {
                if (!_XmlnsMappings.ContainsKey(att.XmlNamespace))
                    _XmlnsMappings[att.XmlNamespace] = new List<TypeResolver>();
                _XmlnsMappings[att.XmlNamespace].Add(new TypeResolver { Assembly = asm, Namespace = att.ClrNamespace });
            }
            _DiscoveredAssemblies.Add(asm);
        }

        object IServiceProvider.GetService(Type serviceType)
        {
            return this;
        }


        #region ITypeDescriptorContext... not implemented
        IContainer ITypeDescriptorContext.Container
        {
            get { throw new NotImplementedException(); }
        }

        object ITypeDescriptorContext.Instance
        {
            get { throw new NotImplementedException(); }
        }

        void ITypeDescriptorContext.OnComponentChanged()
        {
            throw new NotImplementedException();
        }

        bool ITypeDescriptorContext.OnComponentChanging()
        {
            throw new NotImplementedException();
        }

        PropertyDescriptor ITypeDescriptorContext.PropertyDescriptor
        {
            get { throw new NotImplementedException(); }
        }
        #endregion
    }
}
