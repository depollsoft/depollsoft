using System.Windows;
using System.Windows.Controls;
using SLaB.Utilities;

namespace SLaB.Controls.Phone
{
    /// <summary>
    /// A control that displays a link to the marketplace to purchase an app when running in trial mode.
    /// </summary>
    public class TrialBlocker : ContentControl
    {

        /// <summary>
        /// Gets a value indicating whether the app is running in trial mode.
        /// </summary>
        public static readonly DependencyProperty IsTrialProperty =
            DependencyProperty.Register("IsTrial", typeof(bool), typeof(TrialBlocker), new PropertyMetadata(false));
        /// <summary>
        /// Gets or sets the content to display when the app is in trial mode.
        /// </summary>
        public static readonly DependencyProperty TrialContentProperty =
            DependencyProperty.Register("TrialContent", typeof(object), typeof(TrialBlocker), new PropertyMetadata(null));
        /// <summary>
        /// Gets or sets the trial content template.
        /// </summary>
        public static readonly DependencyProperty TrialContentTemplateProperty =
            DependencyProperty.Register("TrialContentTemplate", typeof(DataTemplate), typeof(TrialBlocker), new PropertyMetadata(null));



        /// <summary>
        /// Initializes a new instance of the <see cref="TrialBlocker"/> class.
        /// </summary>
        public TrialBlocker()
        {
            DefaultStyleKey = typeof(TrialBlocker);
            IsTrial = PhoneUtilities.IsTrial;
        }



        /// <summary>
        /// Gets a value indicating whether the app is running in trial mode.
        /// </summary>
        /// <value><c>true</c> if this instance is trial; otherwise, <c>false</c>.</value>
        public bool IsTrial
        {
            get { return (bool)GetValue(IsTrialProperty); }
            private set { SetValue(IsTrialProperty, value); }
        }

        /// <summary>
        /// Gets or sets the content to display when the app is in trial mode.
        /// </summary>
        /// <value>The content of the trial.</value>
        public object TrialContent
        {
            get { return (object)GetValue(TrialContentProperty); }
            set { SetValue(TrialContentProperty, value); }
        }

        /// <summary>
        /// Gets or sets the trial content template.
        /// </summary>
        /// <value>The trial content template.</value>
        public DataTemplate TrialContentTemplate
        {
            get { return (DataTemplate)GetValue(TrialContentTemplateProperty); }
            set { SetValue(TrialContentTemplateProperty, value); }
        }
    }
}
