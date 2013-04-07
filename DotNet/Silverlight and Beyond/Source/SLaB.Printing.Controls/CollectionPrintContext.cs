using System.Collections;
using System.ComponentModel;
using System.Linq;
using System.Windows;
using SLaB.Utilities;

namespace SLaB.Printing.Controls
{
    /// <summary>
    /// Represents the context for a page that is being printed, containing such information as
    /// items to be printed, page number, page count, page size, and margins.
    /// </summary>
    public class CollectionPrintContext : INotifyPropertyChanged
    {
        /// <summary>
        /// Creates a CollectionPrintContext.
        /// </summary>
        /// <param name="host">Specifies a host for the context (usually a control such as the
        /// CollectionPrinter).</param>
        public CollectionPrintContext(object host = null)
        {
            Host = host;
        }

        private object _Host;
        /// <summary>
        /// Gets the host for the CollectionPrintContext (usually a control such as the CollectionPrinter).
        /// </summary>
        public object Host
        {
            get
            {
                return _Host;
            }
            internal set
            {
                if (!object.Equals(_Host, value))
                {
                    _Host = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Host"));
                }
            }
        }

        private Size _PrintableArea;
        /// <summary>
        /// Gets the PrintableArea for the printing page.
        /// </summary>
        public Size PrintableArea
        {
            get
            {
                return _PrintableArea;
            }
            internal set
            {
                if (!object.Equals(_PrintableArea, value))
                {
                    _PrintableArea = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("PrintableArea"));
                }
            }
        }

        private Thickness _PageMargins;
        /// <summary>
        /// Gets the PageMargins for the printing page.
        /// </summary>
        public Thickness PageMargins
        {
            get
            {
                return _PageMargins;
            }
            internal set
            {
                if (!object.Equals(_PageMargins, value))
                {
                    _PageMargins = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("PageMargins"));
                }
            }
        }

        /// <summary>
        /// Raises property change notifications for all properties, forcing a binding refresh.
        /// </summary>
        public void NotifyPropertiesChanged()
        {
            PropertyChanged.Raise(this, new PropertyChangedEventArgs(""));
        }

        private int _FirstItemIndex;
        /// <summary>
        /// Gets the index of the first item in the source collection.
        /// </summary>
        public int FirstItemIndex
        {
            get
            {
                return _FirstItemIndex;
            }
            internal set
            {
                if (!object.Equals(_FirstItemIndex, value))
                {
                    _FirstItemIndex = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("FirstItemIndex"));
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("FirstItem"));
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("FirstItemValue"));
                }
            }
        }

        /// <summary>
        /// Gets the one-based index of the first item in the source collection.
        /// </summary>
        public int FirstItem
        {
            get
            {
                return FirstItemIndex + 1;
            }
        }

        /// <summary>
        /// Gets the value of the first item on the page.
        /// </summary>
        public object FirstItemValue
        {
            get
            {
                return CurrentItems.Cast<object>().FirstOrDefault();
            }
        }

        private int _LastItemIndex;
        /// <summary>
        /// Gets the index of the last item in the source collection.
        /// </summary>
        public int LastItemIndex
        {
            get
            {
                return _LastItemIndex;
            }
            internal set
            {
                if (!object.Equals(_LastItemIndex, value))
                {
                    _LastItemIndex = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("LastItemIndex"));
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("LastItem"));
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("LastItemValue"));
                }
            }
        }

        /// <summary>
        /// Gets the one-based index of the last item in the source collection.
        /// </summary>
        public int LastItem
        {
            get
            {
                return LastItemIndex + 1;
            }
        }

        /// <summary>
        /// Gets the value of the last item on the page.
        /// </summary>
        public object LastItemValue
        {
            get
            {
                return CurrentItems.Cast<object>().LastOrDefault();
            }
        }

        private IEnumerable _CurrentItems;
        /// <summary>
        /// Gets the set of items that should be displayed on the current page.
        /// </summary>
        public IEnumerable CurrentItems
        {
            get
            {
                return _CurrentItems;
            }
            internal set
            {
                if (!object.Equals(_CurrentItems, value))
                {
                    _CurrentItems = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("CurrentItems"));
                }
            }
        }

        private int _CurrentPageIndex;
        /// <summary>
        /// Gets the index of the current page.
        /// </summary>
        public int CurrentPageIndex
        {
            get
            {
                return _CurrentPageIndex;
            }
            internal set
            {
                if (!object.Equals(_CurrentPageIndex, value))
                {
                    _CurrentPageIndex = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("CurrentPageIndex"));
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("CurrentPage"));
                }
            }
        }

        /// <summary>
        /// Gets the one-based index of the current page.
        /// </summary>
        public int CurrentPage
        {
            get
            {
                return CurrentPageIndex + 1;
            }
        }

        private int? _PageCount;
        /// <summary>
        /// Gets the total number of pages in the print job.  This value will be null if
        /// this number cannot be calculated without printing timing out.
        /// </summary>
        public int? PageCount
        {
            get
            {
                return _PageCount;
            }
            internal set
            {
                if (!object.Equals(_PageCount, value))
                {
                    _PageCount = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("PageCount"));
                }
            }
        }

        private bool _IsLastPage;
        /// <summary>
        /// Gets whether this page is the last page in the print job.
        /// </summary>
        public bool IsLastPage
        {
            get
            {
                return _IsLastPage;
            }
            internal set
            {
                if (!object.Equals(_IsLastPage, value))
                {
                    _IsLastPage = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("IsLastPage"));
                }
            }
        }

        private bool _IsFirstPage;
        /// <summary>
        /// Gets whether this page is the first page in the print job.
        /// </summary>
        public bool IsFirstPage
        {
            get
            {
                return _IsFirstPage;
            }
            internal set
            {
                if (!object.Equals(_IsFirstPage, value))
                {
                    _IsFirstPage = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("IsFirstPage"));
                }
            }
        }

        /// <summary>
        /// An event raised when a property's value changes.
        /// </summary>
        public event PropertyChangedEventHandler PropertyChanged;
    }
}
