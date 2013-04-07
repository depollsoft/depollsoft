using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Reflection;

namespace SLaB.Utilities.Xaml.Deserializer
{
    public class XamlMember
    {
        private static IDictionary<MemberInfo, XamlMember> _XamlMembers;

        static XamlMember()
        {
        }

        internal static XamlMember GetXamlMember(MemberInfo member)
        {
            XamlMember xMember = null;
            if (_XamlMembers.TryGetValue(member, out xMember))
                return xMember;
            return _XamlMembers[member] = new XamlMember(member);
        }
        private XamlMember(MemberInfo member)
        {
            Member = member;
        }
        internal MemberInfo Member { get; private set; }
        private bool _TypeResolved;
        private XamlType _XamlType;
        internal XamlType XamlType
        {
            get
            {
                if (!_TypeResolved)
                {
                    PropertyInfo pi = Member as PropertyInfo;
                    if (pi != null)
                        _XamlType = XamlType.GetXamlType(pi.PropertyType);
                    else
                    {
                        EventInfo ei = Member as EventInfo;
                        if (ei != null)
                            _XamlType = XamlType.GetXamlType(ei.EventHandlerType);
                    }
                    _TypeResolved = true;
                }
                return _XamlType;
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

        protected internal virtual TypeConverter GetTypeConverter()
        {
            var tca = Member.GetCustomAttributes(typeof(TypeConverterAttribute), false).Cast<TypeConverterAttribute>().FirstOrDefault();
            if (tca != null)
                return Activator.CreateInstance(Type.GetType(tca.ConverterTypeName)) as TypeConverter;
            if (XamlType.TypeConverter != null)
                return XamlType.TypeConverter;
            if (Member is EventInfo)
            {
                //TODO: Handle event-related TypeConversion
            }
            return null;
        }
    }
}
