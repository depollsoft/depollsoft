#region Using Directives

using System;

#endregion

namespace SLaB.Utilities.Xaml
{
    /// <summary>
    ///   Allows a type to be specified as a dependent type where the dependency is only in XAML.
    ///   This creates an assembly dependency that tools will recognize.
    /// </summary>
    [AttributeUsage(AttributeTargets.Class, AllowMultiple = true, Inherited = false)]
    public class XamlDependencyAttribute : Attribute
    {

        /// <summary>
        ///   Creates a XamlDependencyAttribute.
        /// </summary>
        /// <param name = "type">The dependant type.</param>
        public XamlDependencyAttribute(Type type)
        {
            this.Type = type;
        }



        /// <summary>
        ///   Gets or sets the dependant type.
        /// </summary>
        public Type Type { get; set; }
    }
}