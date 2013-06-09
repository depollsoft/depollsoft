using System.Windows.Controls;

namespace UtilitiesTests
{
    public partial class TestUserControl : UserControl
    {
        public TestUserControl()
        {
            InitializeComponent();
        }

        public object RealContent { get { return base.Content; } }
    }
}
