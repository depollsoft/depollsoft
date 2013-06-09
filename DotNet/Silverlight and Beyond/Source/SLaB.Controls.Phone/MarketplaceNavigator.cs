using System;
using System.Windows.Controls;
using Microsoft.Phone.Tasks;

namespace SLaB.Controls.Phone
{
    /// <summary>
    /// A control within which HyperlinkButtons can navigate to marketplace items using a
    /// "marketplace://&lt;content-id&gt;" scheme.  "marketplace:///" will launch the marketplace
    /// for the running application.
    /// </summary>
    public class MarketplaceNavigator : ContentControl, INavigate
    {


        /// <summary>
        /// Displays the content located at the specified URI.
        /// </summary>
        /// <param name="source">The URI of the content to display.</param>
        /// <returns>
        /// true if the content was successfully displayed; otherwise, false.
        /// </returns>
        public bool Navigate(Uri source)
        {
            if (source.Scheme.ToLower().Equals("marketplace"))
            {
                MarketplaceDetailTask mdt = new MarketplaceDetailTask();
                if (!string.IsNullOrEmpty(source.Host))
                    mdt.ContentIdentifier = source.Host;
                mdt.Show();
                return true;
            }
            else
                return false;
        }
    }
}
