using System;
using System.Collections.ObjectModel;
using Microsoft.Phone.Controls;

namespace ScratchPhoneApplication
{
    public partial class FastItemsControlTestPage : PhoneApplicationPage
    {
        private ObservableCollection<int> items = null;
        private bool isLoading;
        public FastItemsControlTestPage()
        {
            InitializeComponent();
            items = new ObservableCollection<int>();
            for (int x = 0; x < 200; x++)
                items.Add(x);
            //var items = from i in Enumerable.Range(0, 50)
            //            from c in "abcdefghijklmnopqrstuvwxyz"
            //            select new { IntVal = i, CharVal = c };
            ic.ItemsSource = items;
        }

        private void ic_DelayLoadAction(object sender, EventArgs e)
        {
            if (isLoading)
                return;
            isLoading = true;
            for (int x = 0; x < 1; x++)
                items.Add(items[items.Count - 1] + 1);
            if (items.Count > 500)
                ic.HasMoreItems = false;
            isLoading = false;
        }
    }
}