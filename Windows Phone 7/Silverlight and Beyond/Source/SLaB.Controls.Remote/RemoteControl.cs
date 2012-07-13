#region Using Directives

using System;
using System.ComponentModel;
using System.Linq;
using System.Reflection;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;
using SLaB.Utilities;
using SLaB.Utilities.Xap;

#endregion

namespace SLaB.Controls.Remote
{
    /// <summary>
    ///   A control that downloads a Xap and its dependencies, then displays some content given a class name to instantiate.
    /// </summary>
    [TemplateVisualState(GroupName = RemoteContentLoadingStatusGroup, Name = RemoteContentLoadingState)]
    [TemplateVisualState(GroupName = RemoteContentLoadingStatusGroup, Name = RemoteContentLoadedState)]
    [StyleTypedProperty(Property = "ProgressStyle", StyleTargetType = typeof(ProgressBar))]
    [ContentProperty("Setters")]
    [DefaultEvent("RemoteContentLoaded")]
    public class RemoteControl : Control, ISupportInitialize
    {

        private bool _IsSettingContent;
        private bool _IsSettingProgress;
        /// <summary>
        ///   Gets the loaded content.  Null if the content has not yet been loaded.
        /// </summary>
        public static readonly DependencyProperty ContentProperty =
            DependencyProperty.Register("Content",
                                        typeof(object),
                                        typeof(RemoteControl),
                                        new PropertyMetadata(default(object), OnContentChanged));
        /// <summary>
        ///   Gets or sets the DataTemplate for the content if necessary.
        /// </summary>
        public static readonly DependencyProperty ContentTemplateProperty =
            DependencyProperty.Register("ContentTemplate",
                                        typeof(DataTemplate),
                                        typeof(RemoteControl),
                                        new PropertyMetadata(default(DataTemplate)));
        /// <summary>
        ///   Gets the progress of the Xap download.
        /// </summary>
        public static readonly DependencyProperty ProgressProperty =
            DependencyProperty.Register("Progress",
                                        typeof(double),
                                        typeof(RemoteControl),
                                        new PropertyMetadata(default(double), OnProgressChanged));
        /// <summary>
        ///   Gets or sets the style for the progress bar that is displayed before the content has been loaded.
        /// </summary>
        public static readonly DependencyProperty ProgressStyleProperty =
            DependencyProperty.Register("ProgressStyle",
                                        typeof(Style),
                                        typeof(RemoteControl),
                                        new PropertyMetadata(default(Style)));
        /// <summary>
        ///   The name of the RemoteContentLoaded state.
        /// </summary>
        public const string RemoteContentLoadedState = "RemoteContentLoaded";
        /// <summary>
        ///   The name of the RemoteContentLoading state.
        /// </summary>
        public const string RemoteContentLoadingState = "RemoteContentLoading";
        /// <summary>
        ///   The name of the state group for remote content loading.
        /// </summary>
        public const string RemoteContentLoadingStatusGroup = "RemoteContentStatus";



        /// <summary>
        ///   Constructs a RemoteControl.
        /// </summary>
        public RemoteControl()
        {
            this.DefaultStyleKey = typeof(RemoteControl);
        }



        /// <summary>
        ///   Gets or sets the name of the assembly in which the class to be loaded can be found.
        /// </summary>
        [Category("Common Properties")]
        public string AssemblyName { get; set; }

        /// <summary>
        ///   Gets or sets the full name of the class to load.
        /// </summary>
        [Category("Common Properties")]
        public string ClassName { get; set; }

        /// <summary>
        ///   Gets the loaded content.  Null if the content has not yet been loaded.
        /// </summary>
        public object Content
        {
            get { return (object)this.GetValue(ContentProperty); }
            private set
            {
                this._IsSettingContent = true;
                this.SetValue(ContentProperty, value);
                this._IsSettingContent = false;
            }
        }

        /// <summary>
        ///   Gets or sets the DataTemplate for the content if necessary.
        /// </summary>
        public DataTemplate ContentTemplate
        {
            get { return (DataTemplate)this.GetValue(ContentTemplateProperty); }
            set { this.SetValue(ContentTemplateProperty, value); }
        }

        /// <summary>
        ///   Gets the progress of the Xap download.
        /// </summary>
        public double Progress
        {
            get { return (double)this.GetValue(ProgressProperty); }
            private set
            {
                this._IsSettingProgress = true;
                this.SetValue(ProgressProperty, value);
                this._IsSettingProgress = false;
            }
        }

        /// <summary>
        ///   Gets or sets the style for the progress bar that is displayed before the content has been loaded.
        /// </summary>
        public Style ProgressStyle
        {
            get { return (Style)this.GetValue(ProgressStyleProperty); }
            set { this.SetValue(ProgressStyleProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the Uri for the Xap from which the class will be loaded.
        /// </summary>
        [Category("Common Properties")]
        public Uri XapLocation { get; set; }




        /// <summary>
        ///   An event raised when remote content has finished loading.
        /// </summary>
        public event EventHandler<EventArgs> RemoteContentLoaded;




        /// <summary>
        ///   Causes the control to load its content from the remote Xap.  Automatically called upon EndInit() for usages that
        ///   recognize/use ISupportInitialize, such as the XAML parser.
        /// </summary>
        public void Load()
        {
            this.Dispatcher.BeginInvoke(() =>
                {
                    if (this.Content != null || DesignerProperties.IsInDesignTool)
                        return;
                    VisualStateManager.GoToState(this, RemoteContentLoadingState, true);
                    if (this.ClassName == null || this.AssemblyName == null || this.XapLocation == null)
                        throw new ArgumentNullException("ClassName, AssemblyName, and XapLocation must not be null.");
                    XapLoader loader = new XapLoader();
                    XapLoader.XapAsyncResult result =
                        (XapLoader.XapAsyncResult)
                        loader.BeginLoadXap(this.XapLocation,
                                            r =>
                                            this.Dispatcher.BeginInvoke(() => this.LoadFromXap(loader.EndLoadXap(r))),
                                            null);
                    result.PropertyChanged += (sender, args) =>
                        {
                            if (args.PropertyName.Equals("Progress"))
                                UiUtilities.ExecuteOnUiThread(() => this.Progress = result.Progress);
                        };
                });
        }

        private void LoadFromXap(Xap xap)
        {
            var foundAssembly = (from asm in xap.Assemblies
                                 where
                                     System.Reflection.AssemblyName.ReferenceMatchesDefinition(
                                         new AssemblyName(asm.FullName), new AssemblyName(this.AssemblyName))
                                 select asm).Single();
            Type t = foundAssembly.GetType(this.ClassName, true);
            string rdLocation = string.Format("/{0};component/Resources.xaml",
                                              new AssemblyName(foundAssembly.FullName).Name);
            try
            {
                Application.Current.Resources.MergedDictionaries.Add(new ResourceDictionary
                    { Source = new Uri(rdLocation, UriKind.Relative) });
            }
            catch
            {
            }
            this.Content = Activator.CreateInstance(t);
            VisualStateManager.GoToState(this, RemoteContentLoadedState, true);
            this.RemoteContentLoaded.Raise(this, new EventArgs());
        }

        private static void OnContentChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((RemoteControl)obj).OnContentChanged((object)args.OldValue, (object)args.NewValue);
        }

        private void OnContentChanged(object oldValue, object newValue)
        {
            if (!this._IsSettingContent)
            {
                this.Content = oldValue;
                throw new AccessViolationException("Cannot set the Content property");
            }
        }

        private static void OnProgressChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((RemoteControl)obj).OnProgressChanged((double)args.OldValue, (double)args.NewValue);
        }

        private void OnProgressChanged(double oldValue, double newValue)
        {
            if (!this._IsSettingProgress)
            {
                this.Progress = oldValue;
                throw new AccessViolationException("Cannot set the Progress property");
            }
        }




        #region ISupportInitialize Members

        /// <summary>
        ///   Called when the component is being initialized.
        /// </summary>
        void ISupportInitialize.BeginInit()
        {
        }

        /// <summary>
        ///   Called when initialization has ended.  Automatically calls the Load() method.
        /// </summary>
        void ISupportInitialize.EndInit()
        {
            this.Load();
        }

        #endregion
    }
}