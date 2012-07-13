using Microsoft.Phone.Marketplace;

namespace SLaB.Utilities
{
    /// <summary>
    /// Provides utilities useful in Windows Phone applications.
    /// </summary>
    public static class PhoneUtilities
    {

        private static bool? _IsTrial;



        /// <summary>
        /// Gets a value indicating whether this application is in trial mode.
        /// </summary>
        /// <value><c>true</c> if this application is in trial mode; otherwise, <c>false</c>.</value>
        public static bool IsTrial
        {
            get
            {
                if (SimulateTrialMode)
                    return true;
                if (_IsTrial.HasValue)
                    return _IsTrial.Value;
                var license = new LicenseInformation();
                _IsTrial = license.IsTrial();
                return _IsTrial.Value;
            }
        }

        /// <summary>
        /// Gets or sets a value indicating whether trial mode should be simulated.  Consider
        /// placing this in a #if so that you can produce a "trial mode" build of your application for
        /// testing.
        /// </summary>
        /// <value><c>true</c> if trial mode should be simulated; otherwise, <c>false</c>.</value>
        public static bool SimulateTrialMode { get; set; }
    }
}
