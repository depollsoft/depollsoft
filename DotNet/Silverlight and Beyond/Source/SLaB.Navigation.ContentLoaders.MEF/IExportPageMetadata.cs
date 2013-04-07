namespace SLaB.Navigation.ContentLoaders.MEF
{
    /// <summary>
    ///   Defines Page Export metadata.
    /// </summary>
    public interface IExportPageMetadata
    {

        /// <summary>
        ///   The Uri for the page.
        /// </summary>
        string NavigateUri { get; }
    }
}