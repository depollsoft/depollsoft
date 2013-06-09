#region Using Directives

using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Diagnostics;
using System.Linq;
using System.Reflection;
using System.Threading;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Input;
using System.Windows.Markup;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Printing;
using SLaB.Utilities;
using SLaB.Utilities.Xaml;
using SLaB.Utilities.Xaml.Converters;

#endregion

namespace SLaB.Printing.Controls
{
    /// <summary>
    ///   A control that can be used to either preview or print a paginated collection.
    /// </summary>
    [ContentProperty("ItemsSource")]
    [XamlDependency(typeof(BoolConverter))]
    public class CollectionPrinter : ContentControl
    {
        /// <summary>
        ///   Gets or sets the template for the body of each page.  This usually contains some sort of collection-displaying
        ///   control that grows as more items are added to the CurrentPrintContext.CurrentItems collection.
        /// </summary>
        public static readonly DependencyProperty BodyTemplateProperty =
            DependencyProperty.Register("BodyTemplate",
                                        typeof(DataTemplate),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(DataTemplate), OnBodyTemplateChanged));

        /// <summary>
        ///   Gets whether the control is ready for printing.
        /// </summary>
        public static readonly DependencyProperty CanPrintProperty =
            DependencyProperty.Register("CanPrint",
                                        typeof(bool),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(bool), OnCanPrintChanged));

        /// <summary>
        ///   Gets or sets the index of the page being displayed by the control.
        /// </summary>
        public static readonly DependencyProperty CurrentPageIndexProperty =
            DependencyProperty.Register("CurrentPageIndex",
                                        typeof(int),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(int), OnCurrentPageIndexChanged));

        /// <summary>
        ///   Gets the CollectionPrintContext for the page being printed or displayed.
        /// </summary>
        public static readonly DependencyProperty CurrentPrintContextProperty =
            DependencyProperty.Register("CurrentPrintContext",
                                        typeof(CollectionPrintContext),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(CollectionPrintContext),
                                                             OnCurrentPrintContextChanged));

        /// <summary>
        ///   Gets the one-based index of the page being printed.
        /// </summary>
        public static readonly DependencyProperty CurrentlyPrintingPageProperty =
            DependencyProperty.Register("CurrentlyPrintingPage",
                                        typeof(int),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(0));

        /// <summary>
        ///   Gets the one-based index of the page being spooled.
        /// </summary>
        public static readonly DependencyProperty CurrentlySpoolingPageProperty =
            DependencyProperty.Register("CurrentlySpoolingPage",
                                        typeof(int),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(0));

        /// <summary>
        ///   Gets or sets the template that will be used for the footer of each page.
        /// </summary>
        public static readonly DependencyProperty FooterTemplateProperty =
            DependencyProperty.Register("FooterTemplate",
                                        typeof(DataTemplate),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(DataTemplate), OnFooterTemplateChanged));

        /// <summary>
        ///   Gets or sets the template that will be used for the header of each page.
        /// </summary>
        public static readonly DependencyProperty HeaderTemplateProperty =
            DependencyProperty.Register("HeaderTemplate",
                                        typeof(DataTemplate),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(DataTemplate), OnHeaderTemplateChanged));

        /// <summary>
        ///   Gets or sets whether the CollectionPrinter should ignore overflow in the horizontal direction when paginating.
        /// </summary>
        public static readonly DependencyProperty IgnoreHorizontalOverflowProperty =
            DependencyProperty.Register("IgnoreHorizontalOverflow",
                                        typeof(bool),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(bool), OnIgnoreHorizontalOverflowChanged));

        /// <summary>
        ///   Gets whether the CollectionPrinter is currently printing.
        /// </summary>
        public static readonly DependencyProperty IsPrintingProperty =
            DependencyProperty.Register("IsPrinting",
                                        typeof(bool),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(false));

        /// <summary>
        ///   Gets or sets the template for each item (used in the default ItemsControl-based template for
        ///   the CollectionPrinter).
        /// </summary>
        public static readonly DependencyProperty ItemTemplateProperty =
            DependencyProperty.Register("ItemTemplate",
                                        typeof(DataTemplate),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(DataTemplate), OnItemTemplateChanged));

        /// <summary>
        ///   Gets or sets the ItemsPanelTemplate for the ItemsControl-based default template for printing.
        /// </summary>
        public static readonly DependencyProperty ItemsPanelProperty =
            DependencyProperty.Register("ItemsPanel",
                                        typeof(ItemsPanelTemplate),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(ItemsPanelTemplate), OnItemsPanelChanged));

        /// <summary>
        ///   Gets or sets the collection to be printed.
        /// </summary>
        [Category("Common Properties")]
        public static readonly DependencyProperty ItemsSourceProperty =
            DependencyProperty.Register("ItemsSource",
                                        typeof(IEnumerable),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(IEnumerable), OnItemsSourceChanged));

        /// <summary>
        ///   Gets or sets the maximum number of items to place on a page.  Set this value to -1 to
        ///   print as many items as possible on the page.
        /// </summary>
        public static readonly DependencyProperty MaximumItemsPerPageProperty =
            DependencyProperty.Register("MaximumItemsPerPage",
                                        typeof(int),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(-1, OnMaximumItemsPerPageChanged));

        /// <summary>
        ///   Gets or sets the template for the background of each page.
        /// </summary>
        public static readonly DependencyProperty PageBackgroundTemplateProperty =
            DependencyProperty.Register("PageBackgroundTemplate",
                                        typeof(DataTemplate),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata(default(DataTemplate), OnPageBackgroundTemplateChanged));

        /// <summary>
        ///   Gets or sets the name of the document to be printed.
        /// </summary>
        public static readonly DependencyProperty PrintDocumentNameProperty =
            DependencyProperty.Register("PrintDocumentName",
                                        typeof(string),
                                        typeof(CollectionPrinter),
                                        new PropertyMetadata("Silverlight Document", OnPrintDocumentNameChanged));

        private static readonly MethodInfo CalculateChildMethodInfo;
        private readonly object _CalcLock = new object();

        private readonly bool _IsRoot;
        private readonly LambdaCommand<object> _PrintCommand;
        private readonly object _UnloadLock = new object();
        private bool _CalculateInitialized;
        private bool _CalculationComplete;
        private List<CollectionPrintContext> _Contexts;
        private PrintDocument _CurrentPrintDocument;
        private int _CurrentlyPrintingPage;
        private bool _IsCalculating;
        private List<CollectionPrinter> _PagePrinters;
        private bool _ProgressivePrint;
        private int _RetryCount;
        private bool _ShouldEndPrinting;
        private UIElement _VisualChild;

        static CollectionPrinter()
        {
            CalculateChildMethodInfo =
                ReflectionUtilities.GetMethodInfo<CollectionPrinter>(cp => cp.CalculateChild<object>(null)).
                    GetGenericMethodDefinition();
        }

        /// <summary>
        ///   Creates a CollectionPrinter.
        /// </summary>
        public CollectionPrinter()
        {
            if (DesignerProperties.IsInDesignTool)
                UiUtilities.InitializeExecuteOnUiThread();
            this._IsRoot = true;
            this.DefaultStyleKey = typeof(CollectionPrinter);
            this.Loaded += this.CollectionPrinterLoaded;
            this._PrintCommand = new LambdaCommand<object>(param => this.Print(), param => this.CanPrint);
            this.LayoutUpdated += CollectionPrinterLayoutUpdated;
            this.Unloaded += this.CollectionPrinterUnloaded;
        }

        private CollectionPrinter(CollectionPrinter parent, CollectionPrintContext context)
        {
            if (DesignerProperties.IsInDesignTool)
                UiUtilities.InitializeExecuteOnUiThread();
            this._IsRoot = false;
            this._PrintCommand = new LambdaCommand<object>(param => this.Print(), param => this.CanPrint);
            this.DefaultStyleKey = typeof(CollectionPrinter);
            this.Style = parent.Style;
            this.CurrentPrintContext = context;
            this.CurrentPageIndex = context.CurrentPageIndex;
            this.HeaderTemplate = parent.HeaderTemplate;
            this.FooterTemplate = parent.FooterTemplate;
            this.IgnoreHorizontalOverflow = parent.IgnoreHorizontalOverflow;
            //this.ItemsSource = parent.ItemsSource;
            this.ItemTemplate = parent.ItemTemplate;
            this.ItemsPanel = parent.ItemsPanel;
            this.Content = parent.Content;
            this.ContentTemplate = parent.ContentTemplate;
            this.BodyTemplate = parent.BodyTemplate;
            this.MaximumItemsPerPage = parent.MaximumItemsPerPage;
        }

        /// <summary>
        ///   Gets or sets the template for the body of each page.  This usually contains some sort of collection-displaying
        ///   control that grows as more items are added to the CurrentPrintContext.CurrentItems collection.
        /// </summary>
        [Category("Common Properties")]
        public DataTemplate BodyTemplate
        {
            get { return (DataTemplate)this.GetValue(BodyTemplateProperty); }
            set { this.SetValue(BodyTemplateProperty, value); }
        }

        /// <summary>
        ///   Gets whether the control is ready for printing.
        /// </summary>
        public bool CanPrint
        {
            get { return (bool)this.GetValue(CanPrintProperty); }
            private set { this.SetValue(CanPrintProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the index of the page being displayed by the control.
        /// </summary>
        [Category("Common Properties")]
        public int CurrentPageIndex
        {
            get { return (int)this.GetValue(CurrentPageIndexProperty); }
            set { this.SetValue(CurrentPageIndexProperty, value); }
        }

        /// <summary>
        ///   Gets the CollectionPrintContext for the page being printed or displayed.
        /// </summary>
        public CollectionPrintContext CurrentPrintContext
        {
            get { return (CollectionPrintContext)this.GetValue(CurrentPrintContextProperty); }
            private set { this.SetValue(CurrentPrintContextProperty, value); }
        }

        /// <summary>
        ///   Gets the one-based index of the page being printed.
        /// </summary>
        public int CurrentlyPrintingPage
        {
            get { return (int)this.GetValue(CurrentlyPrintingPageProperty); }
            private set { this.SetValue(CurrentlyPrintingPageProperty, value); }
        }

        /// <summary>
        ///   Gets the one-based index of the page being spooled.
        /// </summary>
        public int CurrentlySpoolingPage
        {
            get { return (int)this.GetValue(CurrentlySpoolingPageProperty); }
            private set { this.SetValue(CurrentlySpoolingPageProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the template that will be used for the footer of each page.
        /// </summary>
        [Category("Common Properties")]
        public DataTemplate FooterTemplate
        {
            get { return (DataTemplate)this.GetValue(FooterTemplateProperty); }
            set { this.SetValue(FooterTemplateProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the template that will be used for the header of each page.
        /// </summary>
        [Category("Common Properties")]
        public DataTemplate HeaderTemplate
        {
            get { return (DataTemplate)this.GetValue(HeaderTemplateProperty); }
            set { this.SetValue(HeaderTemplateProperty, value); }
        }

        /// <summary>
        ///   Gets or sets whether the CollectionPrinter should ignore overflow in the horizontal direction when paginating.
        /// </summary>
        [Category("Common Properties")]
        public bool IgnoreHorizontalOverflow
        {
            get { return (bool)this.GetValue(IgnoreHorizontalOverflowProperty); }
            set { this.SetValue(IgnoreHorizontalOverflowProperty, value); }
        }

        /// <summary>
        ///   Gets whether the CollectionPrinter is currently printing.
        /// </summary>
        public bool IsPrinting
        {
            get { return (bool)this.GetValue(IsPrintingProperty); }
            private set { this.SetValue(IsPrintingProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the template for each item (used in the default ItemsControl-based template for
        ///   the CollectionPrinter).
        /// </summary>
        [Category("Common Properties")]
        public DataTemplate ItemTemplate
        {
            get { return (DataTemplate)this.GetValue(ItemTemplateProperty); }
            set { this.SetValue(ItemTemplateProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the ItemsPanelTemplate for the ItemsControl-based default template for printing.
        /// </summary>
        public ItemsPanelTemplate ItemsPanel
        {
            get { return (ItemsPanelTemplate)this.GetValue(ItemsPanelProperty); }
            set { this.SetValue(ItemsPanelProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the collection to be printed.
        /// </summary>
        [Category("Common Properties")]
        public IEnumerable ItemsSource
        {
            get { return (IEnumerable)this.GetValue(ItemsSourceProperty); }
            set { this.SetValue(ItemsSourceProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the maximum number of items to place on a page.  Set this value to -1 to
        ///   print as many items as possible on the page.
        /// </summary>
        [Category("Common Properties")]
        public int MaximumItemsPerPage
        {
            get { return (int)this.GetValue(MaximumItemsPerPageProperty); }
            set { this.SetValue(MaximumItemsPerPageProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the template for the background of each page.
        /// </summary>
        public DataTemplate PageBackgroundTemplate
        {
            get { return (DataTemplate)this.GetValue(PageBackgroundTemplateProperty); }
            set { this.SetValue(PageBackgroundTemplateProperty, value); }
        }

        /// <summary>
        ///   A command that will cause printing to occur when executed.
        /// </summary>
        public ICommand PrintCommand
        {
            get { return this._PrintCommand; }
        }

        /// <summary>
        ///   Gets or sets the name of the document to be printed.
        /// </summary>
        [Category("Common Properties")]
        public string PrintDocumentName
        {
            get { return (string)this.GetValue(PrintDocumentNameProperty); }
            set { this.SetValue(PrintDocumentNameProperty, value); }
        }

        /// <summary>
        ///   Called when a template is applied to the control.
        /// </summary>
        public override void OnApplyTemplate()
        {
            base.OnApplyTemplate();
            this._VisualChild = (UIElement)VisualTreeHelper.GetChild(this, 0);
        }

        /// <summary>
        ///   Prints a document from end to end.
        /// </summary>
        public void Print()
        {
            PrintDocument pd = new PrintDocument();
            pd.BeginPrint += PrintBeginPrint;
            pd.EndPrint += this.PrintEndPrint;
            this._ShouldEndPrinting = true;
            this.Print(pd);
            pd.Print(this.PrintDocumentName);
        }

        /// <summary>
        ///   Prints the collection into an existing PrintDocument.  The CollectionPrinter will never set
        ///   HasMorePages to be true -- that is left to the original creator of the PrintDocument.
        /// </summary>
        /// <param name = "document">PrintDocument on which to print.</param>
        public void Print(PrintDocument document)
        {
            if (this._CurrentPrintDocument != null)
                throw new Exception("Cannot print two things at once");
            this._CurrentPrintDocument = document;
            this._CalculateInitialized = false;
            this._CurrentlyPrintingPage = 0;
            this.CurrentlySpoolingPage = 0;
            this.CurrentlyPrintingPage = 0;
            this._ProgressivePrint = false;
            this.IsPrinting = true;
            document.PrintPage += this.PrintDocPrintPage;
        }

        private static void CollectionPrinterLayoutUpdated(object sender, EventArgs e)
        {
            //if (DesignerProperties.GetIsInDesignMode(this))
            //    OnRenderSpecificPropertyChanged();
        }

        private static void DriveAnimationsToCompletion(FrameworkElement element)
        {
            if (element == null)
                return;
            var groups = VisualStateManager.GetVisualStateGroups(element);
            foreach (VisualStateGroup group in groups)
            {
                foreach (
                    Storyboard board in group.Transitions.Cast<VisualTransition>().Select(trans => trans.Storyboard)
                        .Concat(group.States.Cast<VisualState>().Select(state => state.Storyboard))
                        .Where(board => board.GetCurrentState() != ClockState.Stopped))
                {
                    board.SkipToFill();
                }
            }
            foreach (
                FrameworkElement child in
                    Enumerable.Range(0, VisualTreeHelper.GetChildrenCount(element)).Select(
                        index => VisualTreeHelper.GetChild(element, index)).OfType<FrameworkElement>())
                DriveAnimationsToCompletion(child);
        }

        private static void OnBodyTemplateChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CollectionPrinter)obj).OnBodyTemplateChanged((DataTemplate)args.OldValue, (DataTemplate)args.NewValue);
        }

        private static void OnCanPrintChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CollectionPrinter)obj).OnCanPrintChanged((bool)args.OldValue, (bool)args.NewValue);
        }

        private static void OnCurrentPageIndexChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CollectionPrinter)obj).OnCurrentPageIndexChanged((int)args.OldValue, (int)args.NewValue);
        }

        private static void OnCurrentPrintContextChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            OnCurrentPrintContextChanged((CollectionPrintContext)args.OldValue, (CollectionPrintContext)args.NewValue);
        }

        private static void OnCurrentPrintContextChanged(CollectionPrintContext oldValue,
                                                         CollectionPrintContext newValue)
        {
        }

        private static void OnFooterTemplateChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CollectionPrinter)obj).OnFooterTemplateChanged((DataTemplate)args.OldValue, (DataTemplate)args.NewValue);
        }

        private static void OnHeaderTemplateChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CollectionPrinter)obj).OnHeaderTemplateChanged((DataTemplate)args.OldValue, (DataTemplate)args.NewValue);
        }

        private static void OnIgnoreHorizontalOverflowChanged(DependencyObject obj,
                                                              DependencyPropertyChangedEventArgs args)
        {
            ((CollectionPrinter)obj).OnIgnoreHorizontalOverflowChanged((bool)args.OldValue, (bool)args.NewValue);
        }

        private static void OnItemTemplateChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CollectionPrinter)obj).OnItemTemplateChanged((DataTemplate)args.OldValue, (DataTemplate)args.NewValue);
        }

        private static void OnItemsPanelChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CollectionPrinter)obj).OnItemsPanelChanged((ItemsPanelTemplate)args.OldValue,
                                                         (ItemsPanelTemplate)args.NewValue);
        }

        private static void OnItemsSourceChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CollectionPrinter)obj).OnItemsSourceChanged((IEnumerable)args.OldValue, (IEnumerable)args.NewValue);
        }

        private static void OnMaximumItemsPerPageChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            OnMaximumItemsPerPageChanged((int)args.OldValue, (int)args.NewValue);
        }

        private static void OnMaximumItemsPerPageChanged(int oldValue, int newValue)
        {
            if (newValue < 1 && newValue != -1)
                throw new ArgumentException();
        }

        private static void OnPageBackgroundTemplateChanged(DependencyObject obj,
                                                            DependencyPropertyChangedEventArgs args)
        {
            ((CollectionPrinter)obj).OnPageBackgroundTemplateChanged((DataTemplate)args.OldValue,
                                                                     (DataTemplate)args.NewValue);
        }

        private static void OnPrintDocumentNameChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            OnPrintDocumentNameChanged((string)args.OldValue, (string)args.NewValue);
        }

        private static void OnPrintDocumentNameChanged(string oldValue, string newValue)
        {
        }

        private static void PrintBeginPrint(object sender, BeginPrintEventArgs e)
        {
        }

        private void Calculate(Size? printableArea = null, Thickness? pageMargins = null)
        {
            bool wasExplicit = printableArea != null && pageMargins != null;
            if (!wasExplicit && this.Parent == null)
                return;
            object signal = new object();
            if (double.IsNaN(this.Width) || double.IsNaN(this.Height))
                return;
            if (printableArea == null)
                printableArea = new Size(this.Width, this.Height);
            if (pageMargins == null)
                pageMargins = new Thickness(100);
            var items = this.ItemsSource ?? new object[0];
            Debug.Assert(this._IsRoot);
            if (!this._IsCalculating || wasExplicit)
            {
                this.Dispatcher.DelayUntil(() =>
                    {
                        this._IsCalculating = true;
                        ThreadPool.QueueUserWorkItem(args =>
                            {
                                try
                                {
                                    lock (this._CalcLock)
                                    {
                                        int itemsSoFar = 0;
                                        int currentPage = 0;
                                        this._Contexts = new List<CollectionPrintContext>();
                                        this._PagePrinters = new List<CollectionPrinter>();
                                        while (itemsSoFar == 0 || itemsSoFar < items.Cast<object>().Count())
                                        {
                                            var context = new CollectionPrintContext(this);
                                            context.IsFirstPage = currentPage == 0;
                                            context.CurrentPageIndex = currentPage;
                                            context.FirstItemIndex = itemsSoFar;
                                            context.PrintableArea = printableArea.Value;
                                            context.PageMargins = pageMargins.Value;
                                            CollectionPrinter printer = null;
                                            UiUtilities.ExecuteOnUiThread(() =>
                                                {
                                                    if (wasExplicit)
                                                    {
                                                        this.CurrentlySpoolingPage = this._CurrentPrintDocument != null
                                                                                         ? context.CurrentPage
                                                                                         : 0;
                                                    }
                                                    printer = new CollectionPrinter(this, context);
                                                    printer.ItemsSource = items;
                                                    printer.CalculateChild(signal, items);
                                                    lock (this._UnloadLock)
                                                    {
                                                        if (this.CurrentPageIndex < currentPage &&
                                                            this._Contexts != null)
                                                            this.CurrentPrintContext =
                                                                this._Contexts[this.CurrentPageIndex];
                                                    }
                                                });
                                            lock (signal)
                                                Monitor.Wait(signal);
                                            itemsSoFar += context.CurrentItems.Cast<object>().Count();
                                            currentPage++;
                                            if (itemsSoFar == items.Cast<object>().Count() && this._ProgressivePrint)
                                                this._CalculationComplete = true;
                                            lock (this._UnloadLock)
                                            {
                                                if (this._Contexts != null)
                                                    this._Contexts.Add(context);
                                                if (this._PagePrinters != null)
                                                    this._PagePrinters.Add(printer);
                                            }
                                            if (itemsSoFar == 0)
                                                break;
                                        }
                                        UiUtilities.ExecuteOnUiThread(() =>
                                            {
                                                lock (this._UnloadLock)
                                                {
                                                    if (!this._ProgressivePrint && this._Contexts != null)
                                                        foreach (var context in this._Contexts)
                                                            context.PageCount = currentPage;
                                                }
                                                if (wasExplicit)
                                                {
                                                    this.CurrentlySpoolingPage = 0;
                                                    if (this._CurrentPrintDocument != null)
                                                        this.CurrentlyPrintingPage = 1;
                                                }
                                            });
                                        this._IsCalculating = false;
                                        this._CalculationComplete = true;
                                        UiUtilities.ExecuteOnUiThread(() =>
                                            {
                                                if (this._Contexts != null)
                                                    this.CurrentPrintContext =
                                                        this._Contexts[
                                                            Math.Min(this.CurrentPageIndex, this._Contexts.Count - 1)];
                                            });
                                    }
                                }
                                catch (Exception e)
                                {
                                    UiUtilities.ExecuteOnUiThread(() => MessageBox.Show(e.ToString()));
                                }
                            });
                    },
                                           () => !this._IsCalculating);
            }
        }

        private void CalculateChild(object signal, IEnumerable itemsSource)
        {
            var interfaces =
                itemsSource.GetType().GetInterfaces().Where(
                    t => t.IsGenericType && t.GetGenericTypeDefinition().Equals(typeof(IEnumerable<>)));
            if (interfaces.Count() > 0)
            {
                CalculateChildMethodInfo.MakeGenericMethod(interfaces.First().GetGenericArguments()[0]).Invoke(this,
                                                                                                               new[]
                                                                                                                   {
                                                                                                                       signal
                                                                                                                   });
            }
            else
            {
                this.CalculateChild<object>(signal);
            }
        }

        private void CalculateChild<T>(object signal)
        {
            Debug.Assert(!this._IsRoot);
            Popup popup = new Popup();
            Grid layoutContainer;
            popup.Child = layoutContainer = new Grid();
            layoutContainer.Children.Add(this);
            popup.Opacity = 0;
            layoutContainer.Opacity = 0;
            layoutContainer.IsHitTestVisible = false;
            popup.IsHitTestVisible = false;
            this.Width = popup.Width = this.CurrentPrintContext.PrintableArea.Width;
            this.Height = popup.Height = this.CurrentPrintContext.PrintableArea.Height;
            var items = (this.ItemsSource ?? new T[0]).Cast<T>().Skip(this.CurrentPrintContext.FirstItemIndex).ToArray();
            ObservableCollection<T> pageItems = new ObservableCollection<T>();
            this.CurrentPrintContext.CurrentItems = pageItems;
            this.Dispatcher.DelayUntil(() =>
                {
                    try
                    {
                        this.CurrentPrintContext.IsLastPage = false;
                        bool reachedEnd = false;
                        int itemsAdded = 0;
                        do
                        {
                            if (itemsAdded >= items.Length ||
                                (this.MaximumItemsPerPage != -1 && itemsAdded >= this.MaximumItemsPerPage))
                            {
                                reachedEnd = true;
                                break;
                            }
                            pageItems.Add(items[itemsAdded++]);
                            this.CurrentPrintContext.LastItemIndex = this.CurrentPrintContext.FirstItemIndex +
                                                                     itemsAdded - 1;
                            if (itemsAdded == items.Length)
                                this.CurrentPrintContext.IsLastPage = true;
                            this.CurrentPrintContext.NotifyPropertiesChanged();
                            //popup.UpdateLayout();
                            this._VisualChild.Measure(this.CurrentPrintContext.PrintableArea);
                        } while (this.CheckWidth() && this.CheckHeight());
                        while (!reachedEnd && itemsAdded != 1 &&
                               !(this.CheckExpandedWidth() && this.CheckExpandedHeight() &&
                                 this.CheckExpandedWidthHeight()))
                        {
                            pageItems.RemoveAt(pageItems.Count - 1);
                            itemsAdded--;
                            this.CurrentPrintContext.LastItemIndex = this.CurrentPrintContext.FirstItemIndex +
                                                                     itemsAdded - 1;
                            this.CurrentPrintContext.IsLastPage = false;
                            this.CurrentPrintContext.NotifyPropertiesChanged();
                        }
                        this.Dispatcher.BeginInvoke(() =>
                            {
                                DriveAnimationsToCompletion(popup);
                                this.Dispatcher.BeginInvoke(() =>
                                    {
                                        popup.IsOpen = false;
                                        popup.Child = null;
                                        layoutContainer.Children.Clear();
                                        lock (signal)
                                            Monitor.Pulse(signal);
                                    });
                            });
                    }
                    catch (Exception e)
                    {
                        MessageBox.Show(e.ToString());
                    }
                },
                                       () => this._VisualChild != null);
            popup.IsOpen = true;
        }

        private bool CheckExpandedHeight()
        {
            if (this.CheckHeight())
                return true;
            this._VisualChild.Measure(new Size(this.CurrentPrintContext.PrintableArea.Width,
                                               this.CurrentPrintContext.PrintableArea.Height + 1));
            return this.CheckHeight();
        }

        private bool CheckExpandedWidth()
        {
            if (this.CheckWidth())
                return true;
            this._VisualChild.Measure(new Size(this.CurrentPrintContext.PrintableArea.Width + 1,
                                               this.CurrentPrintContext.PrintableArea.Height));
            return this.CheckWidth();
        }

        private bool CheckExpandedWidthHeight()
        {
            if (this.CheckHeight() || this.CheckWidth())
                return true;
            this._VisualChild.Measure(new Size(this.CurrentPrintContext.PrintableArea.Width + 1,
                                               this.CurrentPrintContext.PrintableArea.Height + 1));
            return this.CheckSize();
        }

        private bool CheckHeight()
        {
            return this._VisualChild.DesiredSize.Height < this.CurrentPrintContext.PrintableArea.Height;
        }

        private bool CheckSize()
        {
            return this.CheckHeight() && this.CheckWidth();
        }

        private bool CheckWidth()
        {
            return this.IgnoreHorizontalOverflow ||
                   this._VisualChild.DesiredSize.Width < this.CurrentPrintContext.PrintableArea.Width;
        }

        private void CollectionPrinterCollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            this.OnRenderSpecificPropertyChanged();
        }

        private void CollectionPrinterLoaded(object sender, RoutedEventArgs e)
        {
            //if (!DesignerProperties.GetIsInDesignMode(this) && (this.Parent != null || !(this.Parent is Popup)))
            //    throw new Exception("Cannot place CollectionPrinter in the visual tree");
            this.OnRenderSpecificPropertyChanged();
        }

        private void CollectionPrinterUnloaded(object sender, RoutedEventArgs e)
        {
            lock (this._UnloadLock)
            {
                this._Contexts = null;
                this._PagePrinters = null;
            }
        }

        private void OnBodyTemplateChanged(DataTemplate oldValue, DataTemplate newValue)
        {
            this.OnRenderSpecificPropertyChanged();
        }

        private void OnCanPrintChanged(bool oldValue, bool newValue)
        {
            this._PrintCommand.RefreshCanExecute();
        }

        private void OnCurrentPageIndexChanged(int oldValue, int newValue)
        {
            if (this._Contexts == null)
                this.OnRenderSpecificPropertyChanged();
            else
                this.CurrentPrintContext =
                    this._Contexts[this.CurrentPageIndex < this._Contexts.Count ? this.CurrentPageIndex : 0];
        }

        private void OnFooterTemplateChanged(DataTemplate oldValue, DataTemplate newValue)
        {
            this.OnRenderSpecificPropertyChanged();
        }

        private void OnHeaderTemplateChanged(DataTemplate oldValue, DataTemplate newValue)
        {
            this.OnRenderSpecificPropertyChanged();
        }

        private void OnIgnoreHorizontalOverflowChanged(bool oldValue, bool newValue)
        {
            this.OnRenderSpecificPropertyChanged();
        }

        private void OnItemTemplateChanged(DataTemplate oldValue, DataTemplate newValue)
        {
            this.OnRenderSpecificPropertyChanged();
        }

        private void OnItemsPanelChanged(ItemsPanelTemplate oldValue, ItemsPanelTemplate newValue)
        {
            this.OnRenderSpecificPropertyChanged();
        }

        private void OnItemsSourceChanged(IEnumerable oldValue, IEnumerable newValue)
        {
            if (oldValue != null && oldValue is INotifyCollectionChanged)
                ((INotifyCollectionChanged)oldValue).CollectionChanged -= this.CollectionPrinterCollectionChanged;
            if (newValue != null && newValue is INotifyCollectionChanged)
                ((INotifyCollectionChanged)newValue).CollectionChanged += this.CollectionPrinterCollectionChanged;
            this.OnRenderSpecificPropertyChanged();
            this.UpdateCanPrint();
        }

        private void OnPageBackgroundTemplateChanged(DataTemplate oldValue, DataTemplate newValue)
        {
            this.OnRenderSpecificPropertyChanged();
        }

        private void OnRenderSpecificPropertyChanged()
        {
            if (this._IsRoot)
                this.Dispatcher.BeginInvoke(() => this.Calculate());
        }

        private void PrintDocPrintPage(object sender, PrintPageEventArgs e)
        {
            if (this._RetryCount >= 7)
                this._ProgressivePrint = true;
            if (!this._CalculateInitialized)
            {
                this._CalculationComplete = false;
                this.Calculate(e.PrintableArea, e.PageMargins);
                this._CalculateInitialized = true;
                this._RetryCount = 0;
            }
            else if (!this._CalculationComplete &&
                     (!this._ProgressivePrint || this._CurrentlyPrintingPage >= this._PagePrinters.Count) ||
                     this._RetryCount <= 3)
            {
                this.CurrentlyPrintingPage = this._CurrentlyPrintingPage;
                e.PageVisual = null;
                e.HasMorePages = true;
                this._RetryCount++;
            }
            else
            {
                e.PageVisual = this._PagePrinters[this._CurrentlyPrintingPage];
                e.PageVisual.Measure(e.PrintableArea);
                e.PageVisual.UpdateLayout();
                e.HasMorePages = true;
                this._CurrentlyPrintingPage++;
                this.CurrentlyPrintingPage = this._CurrentlyPrintingPage;
                if (this._CalculationComplete && this._CurrentlyPrintingPage >= this._PagePrinters.Count)
                {
                    if (this._ShouldEndPrinting)
                        e.HasMorePages = false;
                    else
                    {
                        this._CurrentPrintDocument = null;
                        this._CurrentlyPrintingPage = 0;
                        this.CurrentlySpoolingPage = 0;
                        this.CurrentlyPrintingPage = 0;
                        this._Contexts = null;
                        this._PagePrinters = null;
                    }
                    this.IsPrinting = false;
                    ((PrintDocument)sender).PrintPage -= this.PrintDocPrintPage;
                }
                this._RetryCount = 0;
            }
        }

        private void PrintEndPrint(object sender, EndPrintEventArgs e)
        {
            this._CurrentPrintDocument = null;
            this._CurrentlyPrintingPage = 0;
            this.CurrentlySpoolingPage = 0;
            this.CurrentlyPrintingPage = 0;
            this.IsPrinting = false;
            if (e.Error != null)
            {
                throw new Exception("Printing failed", e.Error);
            }
            this._ShouldEndPrinting = false;
        }

        //TODO: Decide how to handle the Content property.
        //protected override void OnContentChanged(object oldContent, object newContent)
        //{
        //    base.OnContentChanged(oldContent, newContent);
        //    UpdateCanPrint();
        //}

        private void UpdateCanPrint()
        {
            this.CanPrint = this.ItemsSource != null || this.Content != null;
        }
    }
}