#region Using Directives

using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Reflection;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Data;
using System.Windows.Media;
using System.Xml;
using System.Linq.Expressions;
using System.Windows.Documents;

#endregion

namespace SLaB.Utilities.Xaml.Serializer.UI
{
    /// <summary>
    ///   A utility class for serializing UI objects into Xaml such as controls and DependencyObjects.  Understands concepts like Bindings, StaticResources, Styles, and Templates.
    /// </summary>
    public class UiXamlSerializer : XamlSerializer
    {

        private static Dictionary<Type, List<AttachedProperty>> _BuiltInDPs;
        private static HashSet<Type> _BuiltInTypes;
        private readonly bool _IncludeUnsetValues;
        private readonly bool _SerializeBindings;
        private readonly Dictionary<Thread, SerializeState> _State;
        private static readonly Regex AttachedPropertyPathRegex;
        private static readonly PropertyInfo CursorProperty;
        private static readonly Binding PrototypeBinding;
        private static readonly PropertyInfo ResourcesProperty;
        private static readonly PropertyInfo SelectedItemProperty;
        private static readonly PropertyInfo SelectedItemsProperty;
        private static readonly PropertyInfo SelectedValueProperty;
        private static readonly PropertyInfo SetterPropertyProperty;
        private static readonly Dictionary<Type, IEnumerable<PropertyInfo>> StyleTypePropertyCache =
            new Dictionary<Type, IEnumerable<PropertyInfo>>();
        private const string SystemWindowsNamespace = "http://schemas.microsoft.com/client/2007";



        /// <summary>
        ///   Constructs a UiXamlSerializer.
        /// </summary>
        /// <param name = "serializeBindings">Determines whether bindings will be serialized.</param>
        /// <param name = "includeUnsetValues">Determines whether unset dependency properties will be serialized.</param>
        public UiXamlSerializer(bool serializeBindings = true, bool includeUnsetValues = false)
        {
            this._SerializeBindings = serializeBindings;
            this._IncludeUnsetValues = includeUnsetValues;

            this._State = new Dictionary<Thread, SerializeState>();
            this.TypeConverters[typeof(Thickness)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(Color)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(Geometry)] = GeometryTypeConverter.Instance;
            this.TypeConverters[typeof(Duration)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(ImageSource)] = ImageSourceTypeConverter.Instance;
            this.TypeConverters[typeof(CornerRadius)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(GridLength)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(FontFamily)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(FontStyle)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(FontStretch)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(FontWeight)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(Rect)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(Size)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(Thickness)] = ToStringTypeConverter.Instance;
            this.TypeConverters[typeof(TextDecorationCollection)] = TextDecorationsTypeConverter.Instance;

#if WINDOWS_PHONE
            this.PropertiesToSkip.Add(GetPropertyInfo<FrameworkElement>("Name"));
#else
            this.PropertiesToSkip.Add(GetPropertyInfo<RichTextBox>(rtb => rtb.Xaml));
            this.PropertiesToSkip.Add(GetPropertyInfo<UserControl>(uc => uc.Content));
            this.PropertiesToSkip.Add(GetPropertyInfo<FrameworkElement>(fe => fe.Name));
#endif
            if (_BuiltInDPs == null)
            {
                this.DiscoverAttachedProperties(typeof(FrameworkElement).Assembly);
                _BuiltInDPs = this.AttachedProperties;
                _BuiltInTypes = this.DiscoveredTypes;
                this.AttachedProperties = new Dictionary<Type, List<AttachedProperty>>();
                this.DiscoveredTypes = new HashSet<Type>();
            }
            foreach (Type t in _BuiltInDPs.Keys)
                this.AttachedProperties[t] = new List<AttachedProperty>(_BuiltInDPs[t]);
            foreach (Type t in _BuiltInTypes)
                this.DiscoveredTypes.Add(t);
        }

        static UiXamlSerializer()
        {
#if WINDOWS_PHONE
            ResourcesProperty = GetPropertyInfo<FrameworkElement>("Resources");
            SelectedItemProperty = GetPropertyInfo<Selector>("SelectedItem");
            SelectedItemsProperty = GetPropertyInfo<ListBox>("SelectedItems");
            SelectedValueProperty = GetPropertyInfo<Selector>("SelectedValue");
            CursorProperty = GetPropertyInfo<FrameworkElement>("Cursor");
            SetterPropertyProperty = GetPropertyInfo<Setter>("Property");
#else
            ResourcesProperty = GetPropertyInfo<FrameworkElement>(fe => fe.Resources);
            SelectedItemProperty = GetPropertyInfo<Selector>(s => s.SelectedItem);
            SelectedItemsProperty = GetPropertyInfo<ListBox>(lb => lb.SelectedItems);
            SelectedValueProperty = GetPropertyInfo<Selector>(s => s.SelectedValue);
            CursorProperty = GetPropertyInfo<FrameworkElement>(fe => fe.Cursor);
            SetterPropertyProperty = GetPropertyInfo<Setter>(s => s.Property);
#endif
            AttachedPropertyPathRegex = new Regex(@"\(?((?<prefix>\w+):)?(?<className>\w+)\.(?<propName>\w+)\)?");
            PrototypeBinding = new Binding();
        }



        private SerializeState State
        {
            get { return this._State[Thread.CurrentThread]; }
        }




        /// <summary>
        ///   Gets a string for a property used to determine the order in which properties will be serialized.
        /// </summary>
        /// <param name = "prop">The property being serialized.</param>
        /// <returns>A sortable string representation for the property.</returns>
        protected override string GetPropertyOrder(PropertyInfo prop)
        {
            if (PropEquals.Equals(prop, ResourcesProperty))
                return "" + char.MinValue;
            return base.GetPropertyOrder(prop);
        }

        /// <summary>
        ///   Determines whether the given property can be written inline (as an attribute) rather than using object-element syntax.
        /// </summary>
        /// <param name = "obj">The object on which the property is being set.</param>
        /// <param name = "propValue">The value of the property being set.</param>
        /// <param name = "inf">The identifier for the property being set (a PropertyInfo for a property, and the getter MethodInfo for an attached property).</param>
        /// <param name = "cycleCheckObjects">The set of objects on the stack (for cycle detection).</param>
        /// <returns></returns>
        protected override bool IsInlinable(object obj,
                                            object propValue,
                                            MemberInfo inf,
                                            ISet<object> cycleCheckObjects)
        {
            try
            {
                var dObj = obj as DependencyObject;
                if (dObj != null)
                {
                    DependencyProperty dp = null;
                    if (inf is PropertyInfo)
                        dp = this.GetDependencyProperty((PropertyInfo)inf);
                    else if (inf is MethodInfo)
                        dp = this.GetDependencyProperty((MethodInfo)inf);
                    if (dp != null)
                    {
                        if (!this._IncludeUnsetValues && dObj.ReadLocalValue(dp) == DependencyProperty.UnsetValue)
                            return true;
                        Binding binding = GetBinding(dObj, dp);
                        if (this._SerializeBindings && binding != null)
                        {
                            return false;
                        }
                    }
                }
                if (propValue != null && this.State.Resources.ContainsKey(propValue))
                    return true;
                if (propValue is Type
                    && inf.Name.Equals("TargetType")
                    && (inf.DeclaringType.Equals(typeof(Style))
                        || inf.DeclaringType.Equals(typeof(ControlTemplate))))
                    return true;
                if ((inf is PropertyInfo && ((PropertyInfo)inf).PropertyType.Equals(typeof(PropertyPath))) ||
                    (inf is MethodInfo && ((MethodInfo)inf).ReturnType.Equals(typeof(PropertyPath))))
                    return true;
                if (PropEquals.Equals(inf as PropertyInfo, SetterPropertyProperty))
                    return true;
            }
            catch (Exception e)
            {
                Debug.WriteLine(e.ToString());
            }
            return base.IsInlinable(obj, propValue, inf, cycleCheckObjects);
        }

        /// <summary>
        ///   Called after serialization (and can be used for cleanup).
        /// </summary>
        /// <param name = "obj">The object being serialized.</param>
        /// <param name = "str">The serialized Xaml, which can be modified for cleanup during this method.</param>
        protected override void PostSerialize(object obj, ref string str)
        {
            lock (this._State)
                this._State.Remove(Thread.CurrentThread);
            base.PostSerialize(obj, ref str);
        }

        /// <summary>
        ///   Called before serialization (and can be used for initialization).
        /// </summary>
        /// <param name = "obj">The object being serialized.</param>
        /// <param name = "prefixMappings">The initial dictionary of prefixes, which can be primed for custom serializers.</param>
        /// <param name = "cycleCheckObjects">The set of objects on the stack, which can be primed if certain objects must be avoided.</param>
        protected override void PreSerialize(object obj,
                                             Dictionary<string, string> prefixMappings,
                                             ISet<object> cycleCheckObjects)
        {
            lock (this._State)
                this._State[Thread.CurrentThread] = new SerializeState();
            base.PreSerialize(obj, prefixMappings, cycleCheckObjects);
            prefixMappings[SystemWindowsNamespace] = String.Empty;
        }

        /// <summary>
        ///   Determines whether a property should be serialized (based upon the DefaultAttribute and ShouldSerialize methods).
        /// </summary>
        /// <param name = "obj">The object being serialized.</param>
        /// <param name = "propValue">The value of the property being serialized.</param>
        /// <param name = "prop">The property being serialized.</param>
        /// <returns>true if the property should be serialized, false otherwise.</returns>
        protected override bool ShouldSerialize(object obj, object propValue, PropertyInfo prop)
        {
            if (PropEquals.Equals(prop, ResourcesProperty) && propValue.GetType().Equals(typeof(ResourceDictionary)) &&
                ((ResourceDictionary)propValue).Count == 0)
                return false;
            if (obj is ListBox && PropEquals.Equals(prop, SelectedItemsProperty) && ((ListBox)obj).SelectionMode == SelectionMode.Single)
                return false;
            if (obj is Selector && (PropEquals.Equals(prop, SelectedItemProperty) || PropEquals.Equals(prop, SelectedValueProperty)))
                return false;
            if (obj is Binding && Equals(GetPropertyGetter(prop)(PrototypeBinding), propValue))
                return false;
#if WINDOWS_PHONE
            if (obj is FrameworkElement && PropEquals.Equals(prop, CursorProperty) && propValue == null)
                return false;
#endif
            return base.ShouldSerialize(obj, propValue, prop);
        }

        /// <summary>
        ///   Called after all properties that can be written as attributes (rather than in object-element syntax) are written, but before an object-element content is written.  Use this virtual as an opportunity to inject additional attributes before the object is written.
        /// </summary>
        /// <param name = "obj">The object being serialized.</param>
        /// <param name = "writer">The writer being used for serialization.</param>
        /// <param name = "prefixMappings">A mapping of xml namespaces to prefixes.</param>
        /// <param name = "cycleCheckObjects">The set of objects on the stack (for cycle detection).</param>
        protected override void VisitAfterAttributes(object obj,
                                                     XmlWriter writer,
                                                     IDictionary<string, string> prefixMappings,
                                                     ISet<object> cycleCheckObjects)
        {
            base.VisitAfterAttributes(obj, writer, prefixMappings, cycleCheckObjects);
#if WINDOWS_PHONE
#else
            if (obj is Block)
                writer.WriteString("");
#endif
        }

        /// <summary>
        ///   Called immediately after the BeginElement for the object being serialized has been written.
        /// </summary>
        /// <param name = "obj">The object being serialized.</param>
        /// <param name = "writer">The writer being used for serialization.</param>
        /// <param name = "prefixMappings">A mapping of xml namespaces to prefixes.</param>
        /// <param name = "cycleCheckObjects">The set of objects on the stack (for cycle detection).</param>
        protected override void VisitAfterBeginElement(object obj,
                                                       XmlWriter writer,
                                                       IDictionary<string, string> prefixMappings,
                                                       ISet<object> cycleCheckObjects)
        {
            if (obj is Style)
                this.State.StyleTypeStack.Push(((Style)obj).TargetType);
            DependencyObject dObj = obj as DependencyObject;
            if (dObj != null && dObj.ReadLocalValue(FrameworkElement.NameProperty) != DependencyProperty.UnsetValue)
            {
                string name = dObj.GetValue(FrameworkElement.NameProperty) as string;
                writer.WriteAttributeString(this.GetPrefix(XamlNamespace, prefixMappings, writer),
                                            "Name",
                                            XamlNamespace,
                                            name);
            }
        }

        /// <summary>
        ///   Called when an object's Content property was not set, allowing special types (e.g. Templates) whose content properties are not discoverable publicly, to be serialized.
        /// </summary>
        /// <param name = "obj">The object being serialized.</param>
        /// <param name = "writer">The writer being used for serialization.</param>
        /// <param name = "prefixMappings">A mapping of xml namespaces to prefixes.</param>
        /// <param name = "cycleCheckObjects">The set of objects on the stack (for cycle detection).</param>
        protected override void VisitAlternateContent(object obj,
                                                      XmlWriter writer,
                                                      IDictionary<string, string> prefixMappings,
                                                      ISet<object> cycleCheckObjects)
        {
            if (obj is DataTemplate)
                this.VisitObject(((DataTemplate)obj).LoadContent(), writer, prefixMappings, cycleCheckObjects);
            if (obj is ControlTemplate)
            {
                var ct = (ControlTemplate)obj;
                var proto = Activator.CreateInstance(ct.TargetType) as Control;
                proto.Template = ct;
                proto.ApplyTemplate();
                this.State.TemplateStack.Push(new Tuple<ControlTemplate, Control>(ct, proto));
                if (VisualTreeHelper.GetChildrenCount(proto) > 0)
                    this.VisitObject(VisualTreeHelper.GetChild(proto, 0), writer, prefixMappings, cycleCheckObjects);
                this.State.TemplateStack.Pop();
            }
            if (obj is ItemsPanelTemplate)
            {
                Popup p = new Popup();
                p.Opacity = 0;
                p.IsHitTestVisible = false;
                p.IsOpen = true;
                ItemsPanelTemplate ipt = (ItemsPanelTemplate)obj;
                ItemsControl ic = new ItemsControl();
                p.Child = ic;
                ic.ItemsPanel = ipt;
                ic.ApplyTemplate();
                p.UpdateLayout();
                ItemsPresenter ip = VisualTreeHelper.GetChild(ic, 0) as ItemsPresenter;
                object panel = VisualTreeHelper.GetChild(ip, 0);
                p.IsOpen = false;
                this.VisitObject(panel, writer, prefixMappings, cycleCheckObjects);
            }
            base.VisitAlternateContent(obj, writer, prefixMappings, cycleCheckObjects);
        }

        /// <summary>
        ///   Serializes an attached property on an object.
        /// </summary>
        /// <param name = "obj">The object on which the attached property is set.</param>
        /// <param name = "propValue">The value of the attached property.</param>
        /// <param name = "propertyName">The name of the attached property.</param>
        /// <param name = "getter">The getter method for the attached property.</param>
        /// <param name = "setter">The setter method for the attached property.</param>
        /// <param name = "writer">The writer being used to serialize the object.</param>
        /// <param name = "prefixMappings">A mapping of xml namespaces to prefixes.</param>
        /// <param name = "cycleCheckObjects">The set of objects currently on the stack (for cycle detection).</param>
        protected override void VisitAttachedProperty(object obj,
                                                      object propValue,
                                                      string propertyName,
                                                      MethodInfo getter,
                                                      MethodInfo setter,
                                                      XmlWriter writer,
                                                      IDictionary<string, string> prefixMappings,
                                                      ISet<object> cycleCheckObjects)
        {
            string ns = this.GetNamespace(getter.DeclaringType, prefixMappings);
            bool prefixFound = writer.LookupPrefix(ns) != null;
            if (getter.ReturnType.Equals(typeof(PropertyPath)) && propValue != null)
            {
                this.VisitPropertyPath((propValue as PropertyPath).Path,
                                       string.Format("{0}.{1}", getter.DeclaringType.Name, propertyName),
                                       prefixFound ? null : ns,
                                       writer,
                                       prefixMappings,
                                       cycleCheckObjects);
                return;
            }
            if (propValue != null && this.State.Resources.ContainsKey(propValue))
            {
                writer.WriteAttributeString(this.GetPrefix(ns, prefixMappings, writer),
                                            string.Format("{0}.{1}", getter.DeclaringType.Name, propertyName),
                                            prefixFound ? null : ns,
                                            string.Format("{{StaticResource {0}}}", this.State.Resources[propValue]));
                return;
            }
            var dObj = obj as DependencyObject;
            if (dObj != null)
            {
                DependencyProperty dp = GetDependencyProperty(getter);
                if (dp != null)
                {
                    if (!this._IncludeUnsetValues && dObj.ReadLocalValue(dp) == DependencyProperty.UnsetValue)
                        return;
                    Binding binding = GetBinding(dObj, dp);
                    if (!this._SerializeBindings && binding != null)
                    {
                        base.VisitAttachedProperty(obj,
                                                   binding,
                                                   propertyName,
                                                   getter,
                                                   setter,
                                                   writer,
                                                   prefixMappings,
                                                   cycleCheckObjects);
                        return;
                    }
                }
            }

            base.VisitAttachedProperty(obj,
                                       propValue,
                                       propertyName,
                                       getter,
                                       setter,
                                       writer,
                                       prefixMappings,
                                       cycleCheckObjects);
        }

        /// <summary>
        ///   Called immediately before the EndElement for the object being serialized is called.  Can be used for cleanup.
        /// </summary>
        /// <param name = "obj">The object being serialized.</param>
        /// <param name = "writer">The writer being used for serialization.</param>
        /// <param name = "prefixMappings">A mapping of xml namespaces to prefixes.</param>
        /// <param name = "cycleCheckObjects">The set of objects on the stack (for cycle detection).</param>
        protected override void VisitBeforeEndElement(object obj,
                                                      XmlWriter writer,
                                                      IDictionary<string, string> prefixMappings,
                                                      ISet<object> cycleCheckObjects)
        {
            if (obj is Style)
                this.State.StyleTypeStack.Pop();
        }

        /// <summary>
        ///   Serializes the contents of a dictionary.
        /// </summary>
        /// <param name = "dict">The dictionary being serialized.</param>
        /// <param name = "writer">The writer being used for serialization.</param>
        /// <param name = "prefixMappings">A mapping of xml namespaces to prefixes.</param>
        /// <param name = "cycleCheckObjects">The set of objects on the stack (for cycle detection).</param>
        protected override void VisitDictionaryContents(IDictionary dict,
                                                        XmlWriter writer,
                                                        IDictionary<string, string> prefixMappings,
                                                        ISet<object> cycleCheckObjects)
        {
            if (this.State.IsInResources)
                foreach (object key in dict.Keys)
                    if (!(key is Type))
                        this.State.Resources[dict[key]] = key;
            this.State.IsInResources = false;

            base.VisitDictionaryContents(dict, writer, prefixMappings, cycleCheckObjects);
        }

        /// <summary>
        ///   Serializes a property.
        /// </summary>
        /// <param name = "obj">The object on which the property is set.</param>
        /// <param name = "propValue">The value of the property.</param>
        /// <param name = "prop">The property being set.</param>
        /// <param name = "isContentProperty">A value indicating that the property is the ContentProperty for the object, and thus no property elements need to be generated.</param>
        /// <param name = "writer">The writer being used for serialization.</param>
        /// <param name = "prefixMappings">A mapping of xml namespaces to prefixes.</param>
        /// <param name = "cycleCheckObjects">The set of objects on the stack (for cycle detection).</param>
        protected override void VisitProperty(object obj,
                                              object propValue,
                                              PropertyInfo prop,
                                              bool isContentProperty,
                                              XmlWriter writer,
                                              IDictionary<string, string> prefixMappings,
                                              ISet<object> cycleCheckObjects)
        {
            if (obj is ResourceDictionary
                && prop.Name.Equals("Source")
                && prop.DeclaringType.Equals(typeof(ResourceDictionary))
                && (propValue == null || ((Uri)propValue).OriginalString.Equals(string.Empty)))
                return;

            if (obj is Setter && prop.Name.Equals("Property") && prop.DeclaringType.Equals(typeof(Setter)))
            {
                Setter s = obj as Setter;
                var dp =
                    (from p in
                         StyleTypePropertyCache.ContainsKey(this.State.StyleTypeStack.Peek())
                             ? StyleTypePropertyCache[this.State.StyleTypeStack.Peek()]
                             : StyleTypePropertyCache[this.State.StyleTypeStack.Peek()] =
                               this.State.StyleTypeStack.Peek().GetProperties(BindingFlags.FlattenHierarchy |
                                                                              BindingFlags.Public |
                                                                              BindingFlags.Instance)
                     let dProp = GetDependencyProperty(p)
                     where dProp != null && dProp == s.Property
                     select new { Property = p, DependencyProperty = dProp }).FirstOrDefault();
                if (dp != null)
                {
                    this.VisitPropertyPath(dp.Property.Name, "Property", null, writer, prefixMappings, cycleCheckObjects);
                    return;
                }
                var adp = (from ap in this.GetAttachedProperties(this.State.StyleTypeStack.Peek())
                           let dProp = GetDependencyProperty(ap.Getter)
                           where dProp != null && dProp == s.Property
                           select new { Property = ap, DependencyProperty = dProp }).FirstOrDefault();
                if (adp != null)
                {
                    string ns = this.GetNamespace(adp.Property.Getter.DeclaringType, prefixMappings);
                    string prefix = this.GetPrefix(ns, prefixMappings, writer);
                    string path = string.Empty.Equals(prefix)
                                      ? string.Format("{0}.{1}",
                                                      adp.Property.Getter.DeclaringType.Name,
                                                      adp.Property.Name)
                                      : string.Format("{0}:{1}.{2}",
                                                      prefix,
                                                      adp.Property.Getter.DeclaringType.Name,
                                                      adp.Property.Name);
                    this.VisitPropertyPath(path, "Property", null, writer, prefixMappings, cycleCheckObjects);
                    return;
                }
            }

            if (propValue is Type
                && prop.Name.Equals("TargetType")
                && (prop.DeclaringType.Equals(typeof(Style))
                    || prop.DeclaringType.Equals(typeof(ControlTemplate))))
            {
                string ns = this.GetNamespace(propValue as Type, prefixMappings);
                string prefix = writer.LookupPrefix(ns);
                if (prefix == null)
                {
                    prefix = this.GetPrefix(ns, prefixMappings, writer);
                    writer.WriteAttributeString("xmlns", prefix, null, ns);
                }
                if (!string.IsNullOrEmpty(prefix))
                    prefix = prefix + ":";
                writer.WriteAttributeString(prop.Name, prefix + ((Type)propValue).Name);
                return;
            }
            if (prop.PropertyType.Equals(typeof(PropertyPath)) && propValue != null)
            {
                this.VisitPropertyPath((propValue as PropertyPath).Path,
                                       prop.Name,
                                       null,
                                       writer,
                                       prefixMappings,
                                       cycleCheckObjects);
                return;
            }
            if (propValue != null && this.State.Resources.ContainsKey(propValue))
            {
                writer.WriteAttributeString(prop.Name,
                                            string.Format("{{StaticResource {0}}}", this.State.Resources[propValue]));
                return;
            }
#if WINDOWS_PHONE
            if (propValue.GetType().Equals(typeof(RelativeSource)))
            {
                VisitProperty(obj,
                              new Phone.DerivedRelativeSource(propValue as RelativeSource),
                              prop,
                              isContentProperty,
                              writer,
                              prefixMappings,
                              cycleCheckObjects);
                return;
            }
#endif
            var dObj = obj as DependencyObject;
            if (dObj != null)
            {
                DependencyProperty dp = GetDependencyProperty(prop);
                if (dp != null)
                {
                    if (!this._IncludeUnsetValues && dObj.ReadLocalValue(dp) == DependencyProperty.UnsetValue)
                        return;
                    Binding binding = GetBinding(dObj, dp);
                    if (this._SerializeBindings && binding != null)
                    {
                        base.VisitProperty(obj,
                                           binding,
                                           prop,
                                           isContentProperty,
                                           writer,
                                           prefixMappings,
                                           cycleCheckObjects);
                        return;
                    }
                }
            }

            if (PropEquals.Equals(prop, ResourcesProperty))
                this.State.IsInResources = true;
            base.VisitProperty(obj, propValue, prop, isContentProperty, writer, prefixMappings, cycleCheckObjects);
            if (PropEquals.Equals(prop, ResourcesProperty))
                this.State.IsInResources = false;
        }

        /// <summary>
        ///   Serializes a PropertyPath.
        /// </summary>
        /// <param name = "path">The path being serialized.</param>
        /// <param name = "propertyName">The name of the property being serialized.</param>
        /// <param name = "propNs">The namespace for the property being serialized.</param>
        /// <param name = "writer">The writer being used for serialization.</param>
        /// <param name = "prefixMappings">A mapping of xml namespaces to prefixes.</param>
        /// <param name = "cycleCheckObjects">The set of objects on the stack (for cycle detection).</param>
        protected virtual void VisitPropertyPath(string path,
                                                 string propertyName,
                                                 string propNs,
                                                 XmlWriter writer,
                                                 IDictionary<string, string> prefixMappings,
                                                 ISet<object> cycleCheckObjects)
        {
            string newPropertyPath = path;
            MatchCollection apMatches = AttachedPropertyPathRegex.Matches(path);
            foreach (Match m in apMatches)
            {
                string prefix = m.Groups["prefix"].Value;
                string className = m.Groups["className"].Value;
                string propName = m.Groups["propName"].Value;
                AttachedProperty ap = this.FindAttachedProperty(className, propName);
                string ns = this.GetNamespace(ap.Getter.DeclaringType, prefixMappings);
                string xmlPrefix = writer.LookupPrefix(ns);
                if (xmlPrefix == null || !xmlPrefix.Equals(prefix))
                    writer.WriteAttributeString("xmlns", prefix, null, ns);
            }
            if (propNs == null)
                writer.WriteAttributeString(propertyName, newPropertyPath);
            else
                writer.WriteAttributeString(this.GetPrefix(propNs, prefixMappings, writer),
                                            propertyName,
                                            propNs,
                                            newPropertyPath);
        }

        /// <summary>
        ///   Called during attribute serialization on the root object.  Can be used for global namespace declaration.
        /// </summary>
        /// <param name = "obj">The root object.</param>
        /// <param name = "writer">The writer being used for serialization.</param>
        /// <param name = "prefixMappings">A mapping of xml namespaces to prefixes.</param>
        /// <param name = "cycleCheckObjects">The set of objects on the stack (for cycle detection).</param>
        protected override void VisitRootAttribute(object obj,
                                                   XmlWriter writer,
                                                   IDictionary<string, string> prefixMappings,
                                                   ISet<object> cycleCheckObjects)
        {
            writer.WriteAttributeString("xmlns", null, null, SystemWindowsNamespace);
            base.VisitRootAttribute(obj, writer, prefixMappings, cycleCheckObjects);
        }

        private AttachedProperty FindAttachedProperty(string shortClassName, string propName)
        {
            IEnumerable<AttachedProperty> matches = from apList in this.AttachedProperties.Values
                                                    from ap in apList
                                                    where
                                                        ap.Getter.DeclaringType.Name.Equals(shortClassName) &&
                                                        ap.Name.Equals(propName)
                                                    select ap;
            return matches.FirstOrDefault();
        }

        private static Binding GetBinding(DependencyObject obj, DependencyProperty dp)
        {
            var exp = obj.ReadLocalValue(dp) as BindingExpression;
            return exp == null ? null : exp.ParentBinding;
        }

        private DependencyProperty GetDependencyProperty(PropertyInfo prop)
        {
            return this.GetDependencyProperty(prop.Name, prop.DeclaringType);
        }

        private DependencyProperty GetDependencyProperty(MethodInfo meth)
        {
            return this.GetDependencyProperty(meth.Name.Substring(3), meth.DeclaringType);
        }

        private DependencyProperty GetDependencyProperty(string propName, Type declaringType)
        {
            var tuple = new Tuple<Type, string>(declaringType, propName);
            if (this.State.DependencyProperties.ContainsKey(tuple))
                return this.State.DependencyProperties[tuple];
            FieldInfo dpField = declaringType.GetField(propName + "Property",
                                                       BindingFlags.Public | BindingFlags.Static |
                                                       BindingFlags.FlattenHierarchy);
            if (dpField == null)
                return this.State.DependencyProperties[tuple] = null;
            if (dpField.FieldType.Equals(typeof(DependencyProperty)) ||
                dpField.FieldType.IsSubclassOf(typeof(DependencyProperty)))
                return this.State.DependencyProperties[tuple] = dpField.GetValue(null) as DependencyProperty;
            return this.State.DependencyProperties[tuple] = null;
        }




        private class SerializeState
        {
		#region Constructors (1) 

            public SerializeState()
            {
                this.Resources = new Dictionary<object, object>(RefEquals);
                this.TemplateStack = new Stack<Tuple<ControlTemplate, Control>>();
                this.StyleTypeStack = new Stack<Type>();
                this.DependencyProperties = new Dictionary<Tuple<Type, string>, DependencyProperty>();
            }

		#endregion Constructors 

		#region Properties (5) 

            public Dictionary<Tuple<Type, string>, DependencyProperty> DependencyProperties { get; private set; }

            public bool IsInResources { get; set; }

            public Dictionary<object, object> Resources { get; private set; }

            public Stack<Type> StyleTypeStack { get; private set; }

            public Stack<Tuple<ControlTemplate, Control>> TemplateStack { get; private set; }

		#endregion Properties 
        }
#if WINDOWS_PHONE
        /// <summary>
        /// Checks to see whether an object can be serialized in Xaml.
        /// </summary>
        /// <param name="obj">The object to be serialized.</param>
        /// <param name="cycleCheckObjects">The set of objects currently on the stack (used to avoid cycles).</param>
        /// <returns>
        /// true if the object can be serialized, false otherwise.
        /// </returns>
        protected override bool CanWriteObject(object obj, ISet<object> cycleCheckObjects)
        {
            if (obj is Setter || obj is PathGeometry)
                return false;
            return base.CanWriteObject(obj, cycleCheckObjects);
        }
#endif
#if WINDOWS_PHONE
        private static PropertyInfo GetPropertyInfo<TTargetType>(string propertyName)
        {
            return typeof(TTargetType).GetProperty(propertyName);
        }
#else
        private static PropertyInfo GetPropertyInfo<TTargetType>(Expression<Func<TTargetType, object>> expression)
        {
            LambdaExpression expr = expression;
            if (expr.Body.NodeType != ExpressionType.MemberAccess)
                throw new ArgumentException();
            var mcExpr = (MemberExpression)expr.Body;
            return mcExpr.Member as PropertyInfo;
        }
#endif
    }
}