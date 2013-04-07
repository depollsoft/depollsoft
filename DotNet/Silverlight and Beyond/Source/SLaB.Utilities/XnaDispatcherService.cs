using System;
using System.Windows;
using System.Windows.Threading;
using Microsoft.Xna.Framework;
using Microsoft.Phone.Controls;

namespace SLaB.Utilities.Phone
{
    /// <summary>
    /// Invokes FrameworkDispatcher.Update() occasionally.
    /// </summary>
    public class XnaDispatcherService : IApplicationService
    {
        private DispatcherTimer _DispatcherTimer;

        /// <summary>
        /// Initializes a new instance of the <see cref="XnaDispatcherService"/> class.
        /// </summary>
        public XnaDispatcherService()
        {
            FrameworkDispatcher.Update();
        }




        /// <summary>
        /// Called by an application in order to initialize the application extension service.
        /// </summary>
        /// <param name="context">Provides information about the application state.</param>
        public void StartService(ApplicationServiceContext context)
        {
            _DispatcherTimer = new DispatcherTimer();
            _DispatcherTimer.Interval = TimeSpan.FromTicks(333333);
            _DispatcherTimer.Tick += (s, a) => FrameworkDispatcher.Update();
            _DispatcherTimer.Start();
            UiUtilities.Dispatcher.DelayUntil(() =>
                {
                    ((PhoneApplicationFrame)Application.Current.RootVisual).Obscured += (o, a) => { if (_DispatcherTimer.IsEnabled)_DispatcherTimer.Stop(); };
                    ((PhoneApplicationFrame)Application.Current.RootVisual).Unobscured += (o, a) => { if (!_DispatcherTimer.IsEnabled)_DispatcherTimer.Start(); };
                }, () => Application.Current != null && Application.Current.RootVisual != null && Application.Current.RootVisual is PhoneApplicationFrame);
        }

        /// <summary>
        /// Called by an application in order to stop the application extension service.
        /// </summary>
        public void StopService()
        {
            _DispatcherTimer.Stop();
            _DispatcherTimer = null;
        }
    }
}
