#region Using Directives

using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Windows;


#endregion

namespace SLaB.Utilities.Xap
{
    /// <summary>
    ///   Represents a downloaded Xap, including the bytes that were downloaded, the assemblies that were loaded, and a parsed
    ///   version of the AppManifest.
    /// </summary>
    public class Xap : DependencyObject
    {

        internal Xap(Deployment.Deployment d,
                     IEnumerable<Assembly> assemblies)
        {
            this.Manifest = d;
            this.Assemblies = new List<Assembly>(assemblies).AsReadOnly();
        }



        /// <summary>
        ///   Gets the set of Assemblies (including "cached assemblies") found within the Xap.
        /// </summary>
        public IEnumerable<Assembly> Assemblies { get; private set; }

        /// <summary>
        ///   Gets the entry point assembly (indicated by the manifest) for the Xap.
        /// </summary>
        public Assembly EntryPointAssembly
        {
            get
            {
                return (from asm in this.Assemblies
                        where asm.FullName.Split(',')[0].Equals(this.Manifest.EntryPointAssembly)
                        select asm).SingleOrDefault();
            }
        }

        /// <summary>
        ///   Gets the entry point type (indicated by the manifest) for the Xap.
        /// </summary>
        public Type EntryPointType
        {
            get { return this.EntryPointAssembly.GetType(this.Manifest.EntryPointType); }
        }

        /// <summary>
        ///   Gets the parsed AppManifest of the Xap.
        /// </summary>
        public Deployment.Deployment Manifest { get; private set; }
    }
}