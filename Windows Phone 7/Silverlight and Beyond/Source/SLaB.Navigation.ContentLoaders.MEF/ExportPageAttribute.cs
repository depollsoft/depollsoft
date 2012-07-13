#region Using Directives

using System;
using System.ComponentModel.Composition;
using System.Windows.Controls;

#endregion

namespace SLaB.Navigation.ContentLoaders.MEF
{
    /// <summary>
    ///   Defines a Page export that associates a Page with a Uri.
    /// </summary>
    [MetadataAttribute]
    [AttributeUsage(AttributeTargets.Class | AttributeTargets.Property, AllowMultiple = false)]
    public class ExportPageAttribute : ExportAttribute
    {

        /// <summary>
        ///   Creates an ExportPageAttribute with the given NavigateUri.
        /// </summary>
        /// <param name = "navigateUri"></param>
        public ExportPageAttribute(string navigateUri)
            : this()
        {
            this.NavigateUri = navigateUri;
        }

        /// <summary>
        ///   Creates an ExportPageAttribute.
        /// </summary>
        public ExportPageAttribute() : base(typeof(Page))
        {
        }



        /// <summary>
        ///   Gets or sets the NavigateUri for the ExportPageAttribute.
        /// </summary>
        public string NavigateUri { get; set; }
    }
}