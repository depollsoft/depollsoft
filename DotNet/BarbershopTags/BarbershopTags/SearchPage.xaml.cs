using System;
using System.Collections.Generic;
using System.Text;
using System.Windows;
using System.Windows.Input;
using System.Windows.Navigation;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Shell;
using SLaB.Utilities;
using System.ComponentModel;

namespace BarbershopTags {
  public partial class SearchPage : PhoneApplicationPage {
    private bool _LoadedOnce;
    public SearchPage() {
      InitializeComponent();
      this.Loaded += OnLoaded;
      PhoneApplicationService.Current.Deactivated += CurrentDeactivated;
      if (!DesignerProperties.IsInDesignTool) {
        IDictionary<string, object> state = PhoneApplicationService.Current.State;
        state.SetIfContainsKey<string>("query", val => QueryBox.Text = val);
        state.SetIfContainsKey<int>("sheet", val => SheetMusic.SelectedIndex = val);
        state.SetIfContainsKey<int>("parts", val => Parts.SelectedIndex = val);
        state.SetIfContainsKey<int>("learning", val => LearningTracks.SelectedIndex = val);
        state.SetIfContainsKey<int>("collection", val => TagCollection.SelectedIndex = val);
        state.SetIfContainsKey<int>("sortby", val => SortBy.SelectedIndex = val);
      }
    }

    private void CurrentDeactivated(object sender, DeactivatedEventArgs e) {
      IDictionary<string, object> state = PhoneApplicationService.Current.State;
      state.SetIfNotInDesignMode("query", QueryBox.Text);
      state.SetIfNotInDesignMode("sheet", SheetMusic.SelectedIndex);
      state.SetIfNotInDesignMode("parts", Parts.SelectedIndex);
      state.SetIfNotInDesignMode("learning", LearningTracks.SelectedIndex);
      state.SetIfNotInDesignMode("collection", TagCollection.SelectedIndex);
      state.SetIfNotInDesignMode("sortby", SortBy.SelectedIndex);
    }

    protected override void OnNavigatedTo(System.Windows.Navigation.NavigationEventArgs e) {
      base.OnNavigatedTo(e);
    }

    private void OnLoaded(object sender, RoutedEventArgs e) {
      if (!_LoadedOnce)
        QueryBox.Focus();
      _LoadedOnce = true;
      (ApplicationBar.Buttons[0] as ApplicationBarIconButton).IsEnabled = !PhoneUtilities.IsTrial;
    }

    private void SearchButtonClick(object sender, EventArgs e) {
      Dictionary<string, string> queryString = new Dictionary<string, string>();
      if (QueryBox.Text != null)
        queryString["query"] = QueryBox.Text;
      if (((KeyValue)SheetMusic.SelectedItem).Value != null)
        queryString["sheet"] = ((KeyValue)SheetMusic.SelectedItem).Value.ToString();
      if (((KeyValue)Parts.SelectedItem).Value != null)
        queryString["parts"] = ((KeyValue)Parts.SelectedItem).Value.ToString();
      if (((KeyValue)LearningTracks.SelectedItem).Value != null)
        queryString["learning"] = ((KeyValue)LearningTracks.SelectedItem).Value.ToString();
      if (((KeyValue)TagCollection.SelectedItem).Value != null)
        queryString["collection"] = ((KeyValue)TagCollection.SelectedItem).Value.ToString();
      if (((KeyValue)SortBy.SelectedItem).Value != null)
        queryString["sortby"] = ((KeyValue)SortBy.SelectedItem).Value.ToString();
      NavigationService.Navigate(new Uri(string.Format("/SearchResultsPage.xaml?{0}", CreateQueryString(queryString)), UriKind.Relative));
    }
    private string CreateQueryString(IDictionary<string, string> keyvaluepairs) {
      StringBuilder sb = new StringBuilder();
      foreach (var v in keyvaluepairs) {
        sb.Append(Uri.EscapeDataString(v.Key));
        sb.Append('=');
        sb.Append(Uri.EscapeDataString(v.Value));
        sb.Append('&');
      }
      if (keyvaluepairs.Count > 0)
        sb.Remove(sb.Length - 1, 1);
      return sb.ToString();
    }

    private void OnKeyTyped(object sender, KeyEventArgs e) {
      if (e.Key == Key.Enter)
        SearchButtonClick(sender, null);
    }
  }
}