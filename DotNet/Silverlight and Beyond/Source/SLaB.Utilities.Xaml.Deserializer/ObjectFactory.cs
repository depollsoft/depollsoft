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
using System.Globalization;
using System.ComponentModel;

namespace SLaB.Utilities.Xaml.Deserializer
{
    internal class ObjectFactory
    {
        private static readonly CultureInfo EnUsCulture = new CultureInfo("en-us");
        private XamlContext _CreationContext;
        private XamlType _XamlType;
        private XamlMember _XamlMember;
        private List<Tuple<XamlMember, ObjectFactory>> _Setters;
        private List<Tuple<object, bool>> _ContentSetters;
        private TypeConverter Converter
        {
            get
            {
                if (_XamlType != null)
                    return _XamlType.TypeConverter;
                return _XamlMember.TypeConverter;
            }
        }
        private bool _CanAddContent;
        private ObjectFactory(XamlContext creationContext)
        {
            _CreationContext = creationContext;
            _Setters = new List<Tuple<XamlMember, ObjectFactory>>();
            _ContentSetters = new List<Tuple<object, bool>>();
            _CanAddContent = true;
        }
        internal ObjectFactory(XamlType type, XamlContext creationContext)
            : this(creationContext)
        {
            _XamlType = type;
        }
        internal ObjectFactory(XamlMember member, XamlContext creationContext)
            : this(creationContext)
        {
            _XamlMember = member;
        }

        internal XamlType XamlType
        {
            get
            {
                return _XamlType;
            }
        }

        internal object GetValue()
        {
            // No setters found -- should use TypeConversion.
            if (_ContentSetters.Count == 1 && _Setters.Count == 0 && _ContentSetters[0].Item2)
                return Converter.ConvertFrom(_CreationContext, EnUsCulture, _ContentSetters[0].Item1);

            return null;
        }

        internal void AddContent(object value)
        {
            if (!_CanAddContent)
                throw new InvalidOperationException();
            _ContentSetters.Add(new Tuple<object, bool>(value, false));
        }

        internal void AddContent(string value)
        {
            if (!_CanAddContent)
                throw new InvalidOperationException();
            _ContentSetters.Add(new Tuple<object, bool>(value, true));
        }

        internal void SetMember(XamlMember member, ObjectFactory value)
        {
            _Setters.Add(new Tuple<XamlMember, ObjectFactory>(member, value));
            if (_ContentSetters.Count > 0)
                _CanAddContent = false;
        }

        internal void SetMember(XamlMember member, string value, XamlContext context)
        {
            if (!member.TypeConverter.CanConvertFrom(typeof(string)))
                throw new InvalidOperationException();
            var factory = new ObjectFactory(member, context);
            factory.AddContent(value);
            SetMember(member, factory);
        }
    }
}
