#region Using Directives

using System;
using System.ComponentModel;
using System.Reflection;
using System.Threading;
using System.Windows;
using System.Windows.Markup;
using System.Windows.Threading;

#endregion

namespace SLaB.Utilities
{
    /// <summary>
    ///   A collection of useful functions for working with UI in Silverlight.
    /// </summary>
    public static class UiUtilities
    {

        private static Dispatcher _Dispatcher;
        private static readonly string ClrNamespacePattern = @"clr-namespace:{0};assembly={1}";



        /// <summary>
        /// Gets an always-accessible dispatcher.
        /// </summary>
        public static Dispatcher Dispatcher
        {
            get
            {
                if (_Dispatcher == null)
                    InitializeExecuteOnUiThread();
                return _Dispatcher;
            }
        }




        /// <summary>
        ///   Delays taking some action by enqueuing dispatcher BeginInvokes until the condition is true.
        /// </summary>
        /// <param name = "dispatcher">The dispatcher to use.</param>
        /// <param name = "action">The action to take.</param>
        /// <param name = "condition">The condition to be met before taking the action.</param>
        public static void DelayUntil(this Dispatcher dispatcher, Action action, Func<bool> condition)
        {
            if (DesignerProperties.IsInDesignTool)
                return;
            if (!condition())
                Dispatcher.BeginInvoke(() => dispatcher.DelayUntil(action, condition));
            else
                action();
        }

        /// <summary>
        /// Gets the registered DependencyProperty based on the name and ownerType.
        /// </summary>
        /// <param name="name">The name.</param>
        /// <param name="ownerType">Type of the owner.</param>
        /// <returns>The registered DependencyProperty based on the name and ownerType.</returns>
        public static DependencyProperty DependencyPropertyFromName(string name, Type ownerType)
        {
            if (ownerType == null)
                return null;
            string ns = string.Format(ClrNamespacePattern, ownerType.Namespace, new AssemblyName(ownerType.Assembly.FullName).Name);
            try
            {
                string xaml = string.Format(DependencyPropertyPattern, ns, ownerType.Name, name);
                Style style = (Style)XamlReader.Load(xaml);
                return (style.Setters[0] as Setter).Property;
            }
            catch
            {
            }
            return DependencyPropertyFromName(name, ownerType.BaseType);
        }

        /// <summary>
        ///   Executes a function on the UI thread, blocking until the result has been retrieved.
        ///   This method is safe to use whether or not execution is already on the UI thread, since
        ///   it only switches threads if necessary.
        /// </summary>
        /// <typeparam name = "T">The return type of the function.</typeparam>
        /// <param name = "func">The function to execute (usually a lambda).</param>
        /// <returns>The value returned by the function.</returns>
        /// <exception cref = "System.Exception">Any exception thrown by the function will be re-thrown by this method
        ///   on the initiating thread.</exception>
        public static T ExecuteOnUiThread<T>(Func<T> func)
        {
            if (DesignerProperties.IsInDesignTool)
                return default(T);
            if (Dispatcher.CheckAccess())
            {
                return func();
            }
            T result = default(T);
            object waitLock = new object();
            Exception error = null;
            lock (waitLock)
            {
                Dispatcher.BeginInvoke(() =>
                    {
                        try
                        {
                            result = func();
                        }
                        catch (Exception e)
                        {
                            error = e;
                        }
                        lock (waitLock)
                            Monitor.Pulse(waitLock);
                    });
                Monitor.Wait(waitLock);
            }
            if (error != null)
                throw error;
            return result;
        }

        /// <summary>
        ///   Executes an action on the UI thread, blocking until the execution has completed.
        ///   This method is safe to use whether or not execution is already on the UI thread, since
        ///   it only switches threads if necessary.
        /// </summary>
        /// <param name = "action">The action to execute (usually a lambda).</param>
        /// <exception cref = "System.Exception">Any exception thrown by the action will be re-thrown by this method
        ///   on the initiating thread.</exception>
        public static void ExecuteOnUiThread(Action action)
        {
            ExecuteOnUiThread<object>(() =>
                {
                    action();
                    return null;
                });
        }

        /// <summary>
        ///   Sets up UiUtilities for use.  Only needs to be called explicitly in design mode.
        /// </summary>
        public static void InitializeExecuteOnUiThread()
        {
            try
            {
                var visual = Application.Current.RootVisual;
                _Dispatcher = visual.Dispatcher;
            }
            catch
            {
                if (!DesignerProperties.IsInDesignTool)
                    _Dispatcher = Deployment.Current.Dispatcher;
            }
        }

        /// <summary>
        ///   Invokes a delegate (or no-ops if the delegate is null).
        /// </summary>
        /// <param name = "del">The delegate to invoke.</param>
        /// <param name = "arguments">Arguments to the delegate.</param>
        /// <returns>The value returned by the delegate.  Null if the delegate is null.</returns>
        public static object Raise(this Delegate del, params object[] arguments)
        {
            if (del != null)
                return del.DynamicInvoke(arguments);
            return null;
        }

        /// <summary>
        ///   Raises an EventHandler&lt;T&gt; or no-ops if the event handler is null.
        /// </summary>
        /// <typeparam name = "T">The type of EventArgs for the event handler.</typeparam>
        /// <param name = "eh">The event handler to raise.</param>
        /// <param name = "sender">The sender.</param>
        /// <param name = "args">The event arguments.</param>
        public static void Raise<T>(this EventHandler<T> eh, object sender, T args) where T : EventArgs
        {
            if (eh != null)
                eh(sender, args);
        }

        /// <summary>
        ///   Raises an EventHandler&lt;T&gt; on the UI thread or no-ops if the event handler is null.
        /// </summary>
        /// <typeparam name = "T">The type of EventArgs for the event handler.</typeparam>
        /// <param name = "eh">The event handler to raise.</param>
        /// <param name = "sender">The sender.</param>
        /// <param name = "args">The event arguments.</param>
        public static void RaiseOnUiThread<T>(this EventHandler<T> eh, object sender, T args) where T : EventArgs
        {
            Dispatcher.BeginInvoke(() => eh.Raise(sender, args));
        }


        private static readonly string DependencyPropertyPattern =
                    @"<Style
                         xmlns=""http://schemas.microsoft.com/winfx/2006/xaml/presentation""
                         xmlns:x=""http://schemas.microsoft.com/winfx/2006/xaml""
                         xmlns:custom=""{0}""
                         TargetType=""custom:{1}"">
                         <Setter Property=""{2}"" Value=""{{x:Null}}"" />
                      </Style>";
    }
}