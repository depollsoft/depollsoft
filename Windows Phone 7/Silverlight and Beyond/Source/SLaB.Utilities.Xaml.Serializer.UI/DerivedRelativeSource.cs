using System.Windows.Data;

namespace SLaB.Utilities.Xaml.Serializer.UI.Phone
{
    /// <summary>
    /// Provides a RelativeSource that can be instantiated in XAML without using curly-brace syntax
    /// (for pre-SL4 parsers).
    /// </summary>
    public class DerivedRelativeSource : RelativeSource
    {

        /// <summary>
        /// Initializes a new instance of the <see cref="DerivedRelativeSource"/> class.
        /// </summary>
        /// <param name="rs">The source RelativeSource.</param>
        public DerivedRelativeSource(RelativeSource rs)
        {
            this.Mode = rs.Mode;
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="DerivedRelativeSource"/> class.
        /// </summary>
        public DerivedRelativeSource() { }
    }
}
