using System;

namespace SLaB.Offline
{
    [Flags]
    public enum InstallMode
    {
        Never = 0x0,
        OutOfBrowser = 0x1,
        InBrowser = 0x2,
        Delayed = 0x4,
    }
}
