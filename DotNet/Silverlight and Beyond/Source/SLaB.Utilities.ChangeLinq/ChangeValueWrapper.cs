#region Using Directives

using System.Collections.Generic;
using System.ComponentModel;

#endregion

namespace SLaB.Utilities.ChangeLinq
{
    internal class ChangeValueWrapper<T> : SelectObservableCollection<T, T>
    {

        private readonly IChangeValue<IEnumerable<T>> _Source;



        public ChangeValueWrapper(IChangeValue<IEnumerable<T>> source)
            : base(source.Value, i => i)
        {
            this._Source = source;
        }




        protected override void AttachToSource()
        {
            this._Source.PropertyChanged += this.SourcePropertyChanged;
            base.AttachToSource();
        }

        protected override void DetachFromSource()
        {
            this._Source.PropertyChanged -= this.SourcePropertyChanged;
            base.DetachFromSource();
        }

        private void OnPropertyChanged()
        {
            this.Original = this._Source.Value;
        }

        private void SourcePropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            this.OnPropertyChanged();
        }
    }
}