namespace SLaB.Utilities
{
    /// <summary>
    ///   Represents an object whose contents can be explicitly refreshed.
    /// </summary>
    public interface IRefreshable
    {

        /// <summary>
        ///   Refreshes the object.
        /// </summary>
        void Refresh();
    }
}