#region Using Directives

using System.Windows;

#endregion

namespace SLaB.Utilities.Xap.Deployment
{
    /// <summary>
    ///   Provides application part and localization information in the application manifest when deploying a Silverlight-based application.
    /// </summary>
    public class Deployment : DependencyObject
    {
        /// <summary>
        ///   Gets or sets a string name that identifies which part in the System.Windows.Deployment is the entry point assembly.
        /// </summary>
        public static readonly DependencyProperty EntryPointAssemblyProperty =
            DependencyProperty.Register("EntryPointAssembly",
                                        typeof(string),
                                        typeof(Deployment),
                                        new PropertyMetadata(""));

        /// <summary>
        ///   Gets or sets a string that identifies the namespace and type name of the class that contains the System.Windows.Application entry point for your application.
        /// </summary>
        public static readonly DependencyProperty EntryPointTypeProperty =
            DependencyProperty.Register("EntryPointType", typeof(string), typeof(Deployment), new PropertyMetadata(""));

        /// <summary>
        ///   Gets or sets a value that indicates the level of access that cross-domain callers have to the Silverlight-based application in this deployment.
        /// </summary>
        public static readonly DependencyProperty ExternalCallersFromCrossDomainProperty =
            DependencyProperty.Register("ExternalCallersFromCrossDomain",
                                        typeof(CrossDomainAccess),
                                        typeof(Deployment),
                                        new PropertyMetadata(CrossDomainAccess.NoAccess));

        /// <summary>
        ///   Gets or sets a collection of System.Windows.ExternalPart instances that represent the external assemblies required by the application.
        /// </summary>
        public static readonly DependencyProperty ExternalPartsProperty =
            DependencyProperty.Register("ExternalParts",
                                        typeof(ExternalPartCollection),
                                        typeof(Deployment),
                                        new PropertyMetadata(null));

        /// <summary>
        ///   Gets or sets an object that contains information about the application that is used for out-of-browser support.
        /// </summary>
        public static readonly DependencyProperty OutOfBrowserSettingsProperty =
            DependencyProperty.Register("OutOfBrowserSettings",
                                        typeof(OutOfBrowserSettings),
                                        typeof(Deployment),
                                        new PropertyMetadata(null));

        /// <summary>
        ///   Gets or sets a collection of assembly parts that are included in the deployment.
        /// </summary>
        public static readonly DependencyProperty PartsProperty =
            DependencyProperty.Register("Parts",
                                        typeof(AssemblyPartCollection),
                                        typeof(Deployment),
                                        new PropertyMetadata(null));

        /// <summary>
        ///   Gets or sets the Silverlight runtime version that this deployment supports.
        /// </summary>
        public static readonly DependencyProperty RuntimeVersionProperty =
            DependencyProperty.Register("RuntimeVersion", typeof(string), typeof(Deployment), new PropertyMetadata(""));

        /// <summary>
        ///   Constructs a manifest.
        /// </summary>
        public Deployment()
        {
            this.ExternalParts = new ExternalPartCollection();
            this.Parts = new AssemblyPartCollection();
        }

        /// <summary>
        ///   Gets or sets a string name that identifies which part in the System.Windows.Deployment is the entry point assembly.
        /// </summary>
        public string EntryPointAssembly
        {
            get { return (string)this.GetValue(EntryPointAssemblyProperty); }
            set { this.SetValue(EntryPointAssemblyProperty, value); }
        }

        /// <summary>
        ///   Gets or sets a string that identifies the namespace and type name of the class that contains the System.Windows.Application entry point for your application.
        /// </summary>
        public string EntryPointType
        {
            get { return (string)this.GetValue(EntryPointTypeProperty); }
            set { this.SetValue(EntryPointTypeProperty, value); }
        }

        /// <summary>
        ///   Gets or sets a value that indicates the level of access that cross-domain callers have to the Silverlight-based application in this deployment.
        /// </summary>
        public CrossDomainAccess ExternalCallersFromCrossDomain
        {
            get { return (CrossDomainAccess)this.GetValue(ExternalCallersFromCrossDomainProperty); }
            set { this.SetValue(ExternalCallersFromCrossDomainProperty, value); }
        }

        /// <summary>
        ///   Gets or sets a collection of System.Windows.ExternalPart instances that represent the external assemblies required by the application.
        /// </summary>
        public ExternalPartCollection ExternalParts
        {
            get { return (ExternalPartCollection)this.GetValue(ExternalPartsProperty); }
            set { this.SetValue(ExternalPartsProperty, value); }
        }

        /// <summary>
        ///   Gets or sets an object that contains information about the application that is used for out-of-browser support.
        /// </summary>
        public OutOfBrowserSettings OutOfBrowserSettings
        {
            get { return (OutOfBrowserSettings)this.GetValue(OutOfBrowserSettingsProperty); }
            set { this.SetValue(OutOfBrowserSettingsProperty, value); }
        }

        /// <summary>
        ///   Gets or sets a collection of assembly parts that are included in the deployment.
        /// </summary>
        public AssemblyPartCollection Parts
        {
            get { return (AssemblyPartCollection)this.GetValue(PartsProperty); }
            set { this.SetValue(PartsProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the Silverlight runtime version that this deployment supports.
        /// </summary>
        public string RuntimeVersion
        {
            get { return (string)this.GetValue(RuntimeVersionProperty); }
            set { this.SetValue(RuntimeVersionProperty, value); }
        }
    }
}