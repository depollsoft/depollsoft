using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;

namespace PostProcessor
{
    public class Program
    {
        private const string SharedAssemblyInfoPath = "./SLaB/Source/SharedAssemblyInfo.cs";
        public static void Main(string[] args)
        {
            CleanupSharedAssemblyInfo();
        }

        private static void CleanupSharedAssemblyInfo()
        {
            string text = File.ReadAllText(SharedAssemblyInfoPath);
            text = text.Replace("#define SLAB_PRIVATE", "");
            text = text.Trim();
            File.WriteAllText(SharedAssemblyInfoPath, text);
            Console.WriteLine("Stripped: #define SLAB_PRIVATE");
        }
    }
}
