#region Using Directives

using System;
using System.Windows;

#endregion

namespace SLaB.Utilities.Xaml
{
    /// <summary>
    ///   Creates a ResourceDictionary that silently fails if the Source for the ResourceDictionary is invalid or dependencies can't be resolved.
    /// </summary>
    public class TryImportResourceDictionary : ResourceDictionary
    {

        /// <summary>
        ///   Gets or sets the source for the TryImportResourceDictionary.
        /// </summary>
        public static readonly DependencyProperty SourceProperty =
            DependencyProperty.Register("Source",
                                        typeof(Uri),
                                        typeof(TryImportResourceDictionary),
                                        new PropertyMetadata(default(Uri), OnSourceChanged));



        /// <summary>
        ///   Gets or sets the source for the TryImportResourceDictionary.
        /// </summary>
        public new Uri Source
        {
            get { return (Uri)this.GetValue(SourceProperty); }
            set { this.SetValue(SourceProperty, value); }
        }




        private static void OnSourceChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((TryImportResourceDictionary)obj).OnSourceChanged((Uri)args.OldValue, (Uri)args.NewValue);
        }

        private void OnSourceChanged(Uri oldValue, Uri newValue)
        {
            try
            {
                base.Source = newValue;
            }
            catch
            {
            }
        }
    }
}