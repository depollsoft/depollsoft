using System.Collections;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.DataVisualization.Charting;
using System.Windows.Navigation;
using Expression.Blend.SampleData.People;
using SLaB.Utilities.Xaml;

namespace ScratchPrintingProject
{
    [XamlDependency(typeof(DataGrid))]
    [XamlDependency(typeof(Chart))]
    public partial class PrintingPage : Page
    {
        private readonly People _People;
        public PrintingPage()
        {
            InitializeComponent();
            this._People = new People();
            slider.Maximum = this._People.Person.Count;
            ItemCount = 20;
        }

        // Executes when the user navigates to this page.
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
        }


        public double ItemCount
        {
            get { return (double)GetValue(ItemCountProperty); }
            set { SetValue(ItemCountProperty, value); }
        }

        public static readonly DependencyProperty ItemCountProperty =
            DependencyProperty.Register("ItemCount", typeof(double), typeof(PrintingPage), new PropertyMetadata(default(double), OnItemCountChanged));

        private static void OnItemCountChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((PrintingPage)obj).OnItemCountChanged((double)args.OldValue, (double)args.NewValue);
        }

        protected virtual void OnItemCountChanged(double oldValue, double newValue)
        {
            ItemsToPrint = this._People.Person.Take((int)ItemCount);
        }

        public IEnumerable ItemsToPrint
        {
            get { return (IEnumerable)GetValue(ItemsToPrintProperty); }
            set { SetValue(ItemsToPrintProperty, value); }
        }

        public static readonly DependencyProperty ItemsToPrintProperty =
            DependencyProperty.Register("ItemsToPrint", typeof(IEnumerable), typeof(PrintingPage), new PropertyMetadata(default(IEnumerable), OnItemsToPrintChanged));

        private static void OnItemsToPrintChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((PrintingPage)obj).OnItemsToPrintChanged((IEnumerable)args.OldValue, (IEnumerable)args.NewValue);
        }

        protected virtual void OnItemsToPrintChanged(IEnumerable oldValue, IEnumerable newValue)
        {
            printer.ItemsSource = newValue;
            printPreview.ItemsSource = newValue;
        }
    }
}
