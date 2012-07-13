#region Using Directives

using System;
using System.Collections.Generic;
using System.IO;
using System.Windows;
using System.Windows.Resources;

#endregion

namespace SLaB.Utilities
{
    /// <summary>
    ///   Provides static methods for working with zip files.
    /// </summary>
    public static class ZipUtilities
    {

        private const uint CentralFileHeaderSignature = 0x02014b50;
        private const uint EndOfCentralDirSignature = 0x06054b50;




        /// <summary>
        ///   Gets a stream containing the file given a zip file and the path to the file.
        /// </summary>
        /// <param name = "zipFileStream">The stream of the zip file.</param>
        /// <param name = "filename">The file name to load.</param>
        /// <returns>A stream representing the file within the zip file.</returns>
        public static Stream GetFile(Stream zipFileStream, string filename)
        {
            return
                Application.GetResourceStream(new StreamResourceInfo(zipFileStream, "zip"),
                                              new Uri(filename, UriKind.Relative)).Stream;
        }

        /// <summary>
        ///   Reads the central directory of a zip file to find the file names contained within.
        /// </summary>
        /// <param name = "zipFileStream">The stream of the zip file.</param>
        /// <returns>The list of file names contained in the zip.</returns>
        public static IEnumerable<string> GetFilenames(Stream zipFileStream)
        {
            using (MemoryStream ms = new MemoryStream())
            {
                zipFileStream.CopyTo(ms);
                int countEntries = FindCentralDirectory(ms);
                List<string> filenames = new List<string>();
                for (int x = 0; x < countEntries; x++)
                    filenames.Add(ReadNextFileName(ms).Replace('\\', '/'));
                return filenames.AsReadOnly();
            }
        }

        private static int FindCentralDirectory(MemoryStream stream)
        {
            stream.Seek(-4, SeekOrigin.End);
            BinaryReader br = new BinaryReader(stream);
            while (EndOfCentralDirSignature != br.ReadUInt32())
                stream.Seek(-5, SeekOrigin.Current);
#pragma warning disable 168
            ushort diskNum = br.ReadUInt16();
            ushort startNum = br.ReadUInt16();
            ushort numLocalEntries = br.ReadUInt16();
            ushort numEntries = br.ReadUInt16();
            uint dirSize = br.ReadUInt32();
            uint offset = br.ReadUInt32();
#pragma warning restore 168
            stream.Seek(offset, SeekOrigin.Begin);
            return numEntries;
        }

        private static string ReadNextFileName(MemoryStream stream)
        {
            BinaryReader br = new BinaryReader(stream);
            uint signature = br.ReadUInt32();
            if (signature != CentralFileHeaderSignature)
                throw new FormatException("Invalid zip file format");
            stream.Seek(16, SeekOrigin.Current);
#pragma warning disable 168
            uint compressedSize = br.ReadUInt32();
            uint uncompressedSize = br.ReadUInt32();
            ushort fileNameLength = br.ReadUInt16();
            ushort extraFieldLength = br.ReadUInt16();
            ushort fileCommentLength = br.ReadUInt16();
#pragma warning restore 168
            stream.Seek(12, SeekOrigin.Current);
            long position = stream.Position;
            StreamReader sr = new StreamReader(stream);
            char[] fileNameBuffer = new char[fileNameLength];
            sr.ReadBlock(fileNameBuffer, 0, fileNameLength);
            stream.Seek(position + fileNameLength, SeekOrigin.Begin);
            stream.Seek(extraFieldLength + fileCommentLength, SeekOrigin.Current);
            return new string(fileNameBuffer);
        }
    }
}