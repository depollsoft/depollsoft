#define SLAB_PRIVATE

#pragma warning disable 1699

#region Using Directives

using System.Reflection;
using System.Runtime.InteropServices;
using System.Windows.Markup;

#endregion

[assembly: AssemblyDescription("")]
[assembly: AssemblyConfiguration("")]
[assembly: AssemblyCompany("David Poll")]
[assembly: AssemblyProduct("Silverlight and Beyond")]
[assembly: AssemblyCopyright("Copyright © David Poll 2011")]
[assembly: AssemblyTrademark("")]
[assembly: AssemblyCulture("")]
[assembly: ComVisible(false)]
[assembly: AssemblyVersion("0.11.5.0")]

#if SLAB_PRIVATE

[assembly: AssemblyKeyFile("../SLaBKeyPrivate.snk")]
#else
[assembly: AssemblyKeyFile("../SLaBKey.snk")]
#endif

[assembly: XmlnsPrefix(Constants.XmlNamespace, "SLaB")]

internal static class Constants
{

    internal const string XmlNamespace = "http://www.davidpoll.com/SLaB";
}

#pragma warning restore 1699