using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Microsoft.Build.Utilities;
using System.Reflection;
using System.IO;
using Microsoft.Build.Framework;
using System.Security.Policy;

namespace CreateExtmap
{
    [LoadInSeparateAppDomain]
    public class CreateExtmap : AppDomainIsolatedTask
    {
        public string DownloadUri { get; set; }
        [Required]
        public string AssemblyFile { get; set; }

        public override bool Execute()
        {
            try
            {
                string extmapTemplate = new StreamReader(this.GetType().Assembly.GetManifestResourceStream("CreateExtmap.ExtmapTemplate.txt")).ReadToEnd();
                Assembly asm = Assembly.LoadFrom(AssemblyFile);
                string result = extmapTemplate.Replace("%name%", asm.GetName().Name)
                    .Replace("%version%", asm.GetName().Version.ToString())
                    .Replace("%publickey%", asm.GetName().GetPublicKeyToken().Select((b) => b.ToString("x")).Aggregate("", (s1, s2) => s1 + s2))
                    .Replace("%relpath%", Path.GetFileName(AssemblyFile))
                    .Replace("%downloaduri%", DownloadUri ?? Path.GetFileName(AssemblyFile).Replace(".dll", ".zip"));
                File.WriteAllText(AssemblyFile.Replace(".dll", ".extmap.xml"), result);
                return true;
            }
            catch (Exception e)
            {
                this.BuildEngine.LogMessageEvent(new BuildMessageEventArgs(e.Message, "", "CreateExtmap", MessageImportance.High));
                return false;
            }
        }

        public static void Main(string[] args)
        {
            CreateExtmap ce = new CreateExtmap();
            ce.AssemblyFile = args[0];
            if (args.Length > 1)
                ce.DownloadUri = args[1];
            ce.Execute();
        }
    }
}
/*
<?xml version="1.0"?>
<manifest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xmlns:xsd="http://www.w3.org/2001/XMLSchema">
  <assembly>
    <name>%name%</name>
    <version>%version%</version>
    <publickeytoken>%publickey%</publickeytoken>
    <relpath>%relpath%</relpath>
    <extension downloadUri="%downloaduri%" />
  </assembly>

</manifest>*/