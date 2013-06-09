using System;
using System.Windows;
using System.Windows.Controls;
using SLaB.Utilities;

namespace SLaB.Controls.Phone
{
    /// <summary>
    /// A button that can be bound in order to take some action while it is depressed.
    /// </summary>
    public class ContinuousButton : Button
    {

        /// <summary>
        /// Gets or sets the delay time.
        /// </summary>
        public static readonly DependencyProperty DelayTimeProperty =
            DependencyProperty.Register("DelayTime", typeof(TimeSpan), typeof(ContinuousButton), new PropertyMetadata(TimeSpan.FromMilliseconds(50)));
        // Using a DependencyProperty as the backing store for IsDepressed.  This enables animation, styling, binding, etc...
        /// <summary>
        /// Gets or sets a value indicating whether this button is depressed.
        /// </summary>
        public static readonly DependencyProperty IsDepressedProperty =
            DependencyProperty.Register("IsDepressed", typeof(bool), typeof(ContinuousButton), new PropertyMetadata(false));



        /// <summary>
        /// Gets or sets the delay time.
        /// </summary>
        /// <value>The delay time.</value>
        public TimeSpan DelayTime
        {
            get { return (TimeSpan)GetValue(DelayTimeProperty); }
            set { SetValue(DelayTimeProperty, value); }
        }

        /// <summary>
        /// Gets or sets a value indicating whether this button is depressed.
        /// </summary>
        /// <value>
        /// 	<c>true</c> if this button is depressed; otherwise, <c>false</c>.
        /// </value>
        public bool IsDepressed
        {
            get { return (bool)GetValue(IsDepressedProperty); }
            set { SetValue(IsDepressedProperty, value); }
        }




        /// <summary>
        /// Called when the value of the <see cref="P:System.Windows.Controls.Primitives.ButtonBase.IsPressed"/> property changes.
        /// </summary>
        /// <param name="e">The data for <see cref="T:System.Windows.DependencyPropertyChangedEventArgs"/>.</param>
        protected override void OnIsPressedChanged(DependencyPropertyChangedEventArgs e)
        {
            base.OnIsPressedChanged(e);
            if (!IsPressed)
                IsDepressed = false;
            else
            {
                DateTime dt = DateTime.Now;
                Dispatcher.DelayUntil(() => IsDepressed = IsPressed, () => DateTime.Now - dt > DelayTime);
            }
        }
    }
}
