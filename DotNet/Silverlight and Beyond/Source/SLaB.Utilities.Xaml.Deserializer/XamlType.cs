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
using System.Reflection;
using System.ComponentModel;
using System.Linq;
using System.Windows.Markup;
using System.Collections.Generic;
using System.Globalization;

namespace SLaB.Utilities.Xaml.Deserializer
{
    internal class XamlType
    {
        private static IDictionary<Type, XamlType> _XamlTypes;

        static XamlType()
        {
            _XamlTypes = new Dictionary<Type, XamlType>();
            _XamlTypes[typeof(int)] = new FlexibleXamlType(typeof(int), new DelegateTypeConverter<int>((context, culture, val) => int.Parse(val, culture)));
            _XamlTypes[typeof(short)] = new FlexibleXamlType(typeof(short), new DelegateTypeConverter<short>((context, culture, val) => short.Parse(val, culture)));
            _XamlTypes[typeof(char)] = new FlexibleXamlType(typeof(char), new DelegateTypeConverter<char>((context, culture, val) => val[0]));
            _XamlTypes[typeof(bool)] = new FlexibleXamlType(typeof(bool), new DelegateTypeConverter<bool>((context, culture, val) =>
                {
                    if (val.Equals("True"))
                        return true;
                    if (val.Equals("False"))
                        return false;
                    throw new ArgumentException();
                }));
            _XamlTypes[typeof(decimal)] = new FlexibleXamlType(typeof(decimal), new DelegateTypeConverter<decimal>((context, culture, val) => decimal.Parse(val, culture)));
            _XamlTypes[typeof(long)] = new FlexibleXamlType(typeof(long), new DelegateTypeConverter<long>((context, culture, val) => long.Parse(val, culture)));
            _XamlTypes[typeof(TimeSpan)] = new FlexibleXamlType(typeof(TimeSpan), new DelegateTypeConverter<TimeSpan>((context, culture, val) => TimeSpan.Parse(val, culture)));
            _XamlTypes[typeof(Uri)] = new FlexibleXamlType(typeof(Uri), new DelegateTypeConverter<Uri>((context, culture, val) => new Uri(val)));
            _XamlTypes[typeof(byte)] = new FlexibleXamlType(typeof(byte), new DelegateTypeConverter<byte>((context, culture, val) => byte.Parse(val, culture)));
            _XamlTypes[typeof(Type)] = new FlexibleXamlType(typeof(Type), new DelegateTypeConverter<Type>((context, culture, val) => ((IXamlTypeResolver)context.GetService(typeof(IXamlTypeResolver))).Resolve(val)));
        }

        internal static XamlType GetXamlType(Type clrType)
        {
            XamlType type = null;
            if (_XamlTypes.TryGetValue(clrType, out type))
                return type;
            return _XamlTypes[clrType] = new XamlType(clrType);
        }
        private XamlType(Type clrType)
        {
            ClrType = clrType;
        }
        internal Type ClrType { get; private set; }

        private bool _ContentPropertyResolved;
        private PropertyInfo _ContentProperty;
        internal PropertyInfo ContentProperty
        {
            get
            {
                if (!_ContentPropertyResolved)
                {
                    var atts = ClrType.GetCustomAttributes(typeof(ContentPropertyAttribute), true).Cast<ContentPropertyAttribute>();
                    var cpa = atts.FirstOrDefault();
                    if (cpa != null)
                        _ContentProperty = ClrType.GetProperty(cpa.Name);
                    _ContentPropertyResolved = true;
                }
                return _ContentProperty;
            }
        }

        private bool _TypeConverterResolved;
        private TypeConverter _TypeConverter;
        internal TypeConverter TypeConverter
        {
            get
            {
                if (!_TypeConverterResolved)
                {
                    _TypeConverter = GetTypeConverter();
                    _TypeConverterResolved = true;
                }
                return _TypeConverter;
            }
        }

        public bool IsList
        {
            get
            {
                return false;
            }
        }
        public bool IsDictionary
        {
            get
            {
                return false;
            }
        }

        protected internal virtual TypeConverter GetTypeConverter()
        {
            var tca = ClrType.GetCustomAttributes(typeof(TypeConverterAttribute), false).Cast<TypeConverterAttribute>().FirstOrDefault();
            if (tca != null)
                return Activator.CreateInstance(Type.GetType(tca.ConverterTypeName)) as TypeConverter;
            if (ClrType.IsEnum)
                return new DelegateTypeConverter<object>((context, culture, val) => Enum.Parse(ClrType, val, false));
            if (ClrType.IsGenericType && ClrType.GetGenericTypeDefinition().Equals(typeof(Nullable<>)))
            {
                var nullableKind = Nullable.GetUnderlyingType(ClrType);
                var nullableType = ClrType;
                var baseTypeConverter = XamlType.GetXamlType(nullableKind).TypeConverter;
                return new DelegateTypeConverter<object>((context, culture, val) =>
                    {
                        var result = baseTypeConverter.ConvertFrom(context, culture, val);
                        return Activator.CreateInstance(nullableType, result);
                    });
            }
            return null;
        }

        private class FlexibleXamlType : XamlType
        {
            private TypeConverter _TypeConverter;
            public FlexibleXamlType(Type t, TypeConverter converter)
                : base(t)
            {
                _TypeConverter = converter;
            }
            protected internal override TypeConverter GetTypeConverter()
            {
                return _TypeConverter;
            }
        }
    }
}
