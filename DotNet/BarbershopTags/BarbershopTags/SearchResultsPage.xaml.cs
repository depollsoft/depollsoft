using System;
using System.Windows.Navigation;
using BarbershopTags.Barbershop;
using Microsoft.Phone.Controls;

namespace BarbershopTags {
  public partial class SearchResultsPage : PhoneApplicationPage {
    private bool _HasBeenNavigatedTo;
    private QueryModel _Query;
    public SearchResultsPage() {
      InitializeComponent();
      LayoutRoot.DataContext = _Query = new QueryModel() { MaxResults = int.MaxValue };
    }

    protected override void OnNavigatedTo(NavigationEventArgs e) {
      base.OnNavigatedTo(e);
      if (_HasBeenNavigatedTo)
        return;
      _HasBeenNavigatedTo = true;
      foreach (var kvp in NavigationContext.QueryString) {
        string unescaped = Uri.UnescapeDataString(kvp.Value);
        switch (kvp.Key) {
          case "query":
            _Query.Query = unescaped;
            break;
          case "sheet":
            _Query.HasSheetMusic = int.Parse(unescaped) == 1;
            break;
          case "parts":
            _Query.Parts = int.Parse(unescaped);
            break;
          case "learning":
            _Query.HasLearningTracks = int.Parse(unescaped) == 1;
            break;
          case "collection":
            _Query.Collection = (TagCollection)Enum.Parse(typeof(TagCollection), unescaped, true);
            break;
          case "sortby":
            _Query.SortBy = (TagSortOptions)Enum.Parse(typeof(TagSortOptions), unescaped, true);
            break;
        }
      }
      _Query.FetchResults();
      this.SearchTitle.Text = _Query.Query;
    }
  }
}