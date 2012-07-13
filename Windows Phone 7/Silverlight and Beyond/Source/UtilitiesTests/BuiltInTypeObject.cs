using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Windows;
using System.Windows.Markup;

[assembly: XmlnsPrefix("clr-namespace:UtilitiesTests;assembly=UtilitiesTests", "utilTests")]
namespace UtilitiesTests
{
    public class BuiltInTypeObject
    {
        public int IntValue { get; set; }
        public double DoubleValue { get; set; }
        public Uri UriValue { get; set; }
        public bool BoolValue { get; set; }
        public string StringValue { get; set; }
    }

    public class NestedObject
    {
        public NestedObject()
        {
            DictValue = new Dictionary<object, string>();
            ListValue = new List<int>();
            AttachedDictionary = new Dictionary<string, string>();
        }
        public int IntValue { get; set; }
        public double DoubleValue { get; set; }
        [DefaultValue(null)]
        public Uri UriValue { get; set; }
        public bool BoolValue { get; set; }
        public string StringValue { get; set; }
        public NestedObject NestedValue { get; set; }
        public bool ShouldSerializeNestedValue()
        {
            return NestedValue != null;
        }
        public object OtherObject { get; set; }
        public Dictionary<object, string> DictValue { get; private set; }
        public List<int> ListValue { get; private set; }
        internal object AttachedObject { get; set; }
        internal Dictionary<string, string> AttachedDictionary { get; set; }
    }

    public class AttachedProps
    {
        public static object GetAttachedObject(NestedObject obj)
        {
            return obj.AttachedObject;
        }
        public static void SetAttachedObject(NestedObject obj, object value)
        {
            obj.AttachedObject = value;
        }
        public static Dictionary<string, string> GetAttachedDictionary(NestedObject obj)
        {
            return obj.AttachedDictionary;
        }


        public static string GetAttachedDpString(DependencyObject obj)
        {
            return (string)obj.GetValue(AttachedDpStringProperty);
        }

        public static void SetAttachedDpString(DependencyObject obj, string value)
        {
            obj.SetValue(AttachedDpStringProperty, value);
        }

        // Using a DependencyProperty as the backing store for AttachedDPString.  This enables animation, styling, binding, etc...
        public static readonly DependencyProperty AttachedDpStringProperty =
            DependencyProperty.RegisterAttached("AttachedDpString", typeof(string), typeof(AttachedProps), new PropertyMetadata("Hello, world!"));


    }
}
