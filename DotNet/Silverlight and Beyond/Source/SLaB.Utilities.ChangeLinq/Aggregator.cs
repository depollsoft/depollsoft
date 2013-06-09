#region Using Directives

using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Linq;

#endregion

namespace SLaB.Utilities.ChangeLinq
{
    internal class Aggregator<TIn, TOut> : ChangeValue<TOut>, IRefreshable
    {

        private readonly Func<TOut, TIn, TOut> _Aggregator;
        private readonly TOut _InitialValue;
        private readonly IEnumerable<TIn> _Original;




        private void AggregatorCollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            this.Reset();
        }

        private void Reset()
        {
            TOut result = this._InitialValue;
            result = this._Original.Aggregate(result, (current, item) => this._Aggregator(current, item));
            this.Value = result;
        }


        public Aggregator(IEnumerable<TIn> original, Func<TOut, TIn, TOut> aggregator, TOut initialValue = default(TOut))
        {
            this._Aggregator = aggregator;
            this._InitialValue = initialValue;
            if (!(original is INotifyCollectionChanged))
                this._Original = original.ToArray();
            else
            {
                this._Original = original;
                ((INotifyCollectionChanged)original).CollectionChanged += this.AggregatorCollectionChanged;
            }
            this.Reset();
        }


        #region IRefreshable Members

        public override void Refresh()
        {
            if (this._Original is IRefreshable)
                ((IRefreshable)this._Original).Refresh();
            this.Reset();
        }

        #endregion
    }
}