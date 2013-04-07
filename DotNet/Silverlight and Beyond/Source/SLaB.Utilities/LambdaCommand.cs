#region Using Directives

using System;
using System.Windows.Input;

#endregion

namespace SLaB.Utilities
{
    /// <summary>
    ///   An ICommand that can be created easily using lambdas and explicitly refreshed.
    /// </summary>
    /// <typeparam name = "T">The CommandParameter type.</typeparam>
    public class LambdaCommand<T> : ICommand
    {

        private readonly Func<T, bool> _CanExecute;
        private readonly Action<T> _Execute;



        /// <summary>
        ///   Constructs a LambdaCommand.
        /// </summary>
        /// <param name = "execute">The action to execute.</param>
        /// <param name = "canExecute">The CanExecute predicate for the command.</param>
        public LambdaCommand(Action<T> execute, Func<T, bool> canExecute = null)
        {
            if (execute == null)
                throw new ArgumentNullException("execute");
            if (canExecute == null)
                canExecute = parameter => true;
            this._Execute = execute;
            this._CanExecute = canExecute;
        }




        /// <summary>
        ///   Raises CanExecuteChanged on the LambdaCommand.
        /// </summary>
        public void RefreshCanExecute()
        {
            this.CanExecuteChanged.Raise(this, new EventArgs());
        }




        #region ICommand Members

        /// <summary>
        ///   Indicates that the result of CanExecute may have changed.
        /// </summary>
        public event EventHandler CanExecuteChanged;

        /// <summary>
        ///   Indicates whether the command can be executed.
        /// </summary>
        /// <param name = "parameter">The CommandParameter.</param>
        /// <returns>true if the command can be executed.  false otherwise.</returns>
        public bool CanExecute(object parameter)
        {
            if (!(parameter is T) && parameter != null)
                return false;
            return this._CanExecute((T)parameter);
        }

        /// <summary>
        ///   Executes the command with the given parameter.
        /// </summary>
        /// <param name = "parameter">The CommandParameter.</param>
        public void Execute(object parameter)
        {
            if (!this.CanExecute(parameter))
                return;
            this._Execute((T)parameter);
        }

        #endregion
    }
}