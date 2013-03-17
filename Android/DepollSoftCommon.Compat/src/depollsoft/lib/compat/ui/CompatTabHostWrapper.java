package depollsoft.lib.compat.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;

import com.bindroid.utils.Function;

import depollsoft.lib.compat.Compatibility;
import depollsoft.lib.compat.RunnableFactory;

@TargetApi(11)
public class CompatTabHostWrapper {
  private Activity activity;
  private TabHost host;
  private boolean hasActionBar;

  public CompatTabHostWrapper(Activity activity, TabHost host) {
    this.activity = activity;
    this.host = host;
    hasActionBar = ActionBars.hasActionBar(activity);

    if (hasActionBar && Compatibility.tryWithFallback(new RunnableFactory() {
      @Override
      public Runnable create() {
        return new Runnable() {
          @Override
          public void run() {
            CompatTabHostWrapper.this.activity.getActionBar()
                .setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
          }
        };
      }
    })) {
      host.getTabWidget().setVisibility(View.GONE);
    }
    else {
      host.getTabWidget().setVisibility(View.VISIBLE);
    }
  }

  public void saveInstanceState(final String key, final Bundle bundle) {
    if (hasActionBar) {
      Compatibility.tryWithFallback(new RunnableFactory() {
        @Override
        public Runnable create() {
          return new Runnable() {
            @Override
            public void run() {
              bundle.putInt(key, activity.getActionBar().getSelectedTab()
                  .getPosition());
            }
          };
        }
      });
    }
    else {
      bundle.putString(key, host.getCurrentTabTag());
    }
  }

  public void restoreInstanceState(final String key, final Bundle bundle) {
    if (hasActionBar) {
      Compatibility.tryWithFallback(new RunnableFactory() {
        @Override
        public Runnable create() {
          return new Runnable() {
            @Override
            public void run() {
              activity.getActionBar().getTabAt(bundle.getInt(key)).select();
            }
          };
        }
      });
    }
    else {
      host.setCurrentTabByTag(bundle.getString(key));
    }
  }

  public CompatTabHostWrapper.TabSpec newTabSpec(String tag) {
    return new TabSpec(tag);
  }

  public void addTab(final CompatTabHostWrapper.TabSpec spec) {
    host.addTab(spec.buildTabSpec());
    if (hasActionBar) {
      Compatibility.tryWithFallback(new RunnableFactory() {
        @Override
        public Runnable create() {
          return new Runnable() {
            @Override
            public void run() {
              activity.getActionBar().addTab((Tab) spec.buildTab());
            }
          };
        }
      });
    }
  }

  public void clearAllTabs() {
    host.clearAllTabs();
    if (hasActionBar) {
      Compatibility.tryWithFallback(new RunnableFactory() {
        @Override
        public Runnable create() {
          return new Runnable() {
            @Override
            public void run() {
              activity.getActionBar().removeAllTabs();
            }
          };
        }
      });
    }
  }

  public class TabSpec {
    private Function<TabHost.TabSpec> tabSpecBuilder;
    private Function<ActionBar.Tab> tabBuilder;
    private final String tag;
    private TabListener setCurrentTabListener;

    public TabSpec(String tag) {
      this.tag = tag;

      Compatibility.tryWithFallback(new RunnableFactory() {
        @Override
        public Runnable create() {
          return new Runnable() {
            @Override
            public void run() {
              setCurrentTabListener = new TabListener() {
                @Override
                public void onTabReselected(Tab tab, FragmentTransaction ft) {
                }

                @Override
                public void onTabSelected(Tab tab, FragmentTransaction ft) {
                  host.setCurrentTabByTag(TabSpec.this.tag);
                }

                @Override
                public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                }
              };
            }
          };
        }
      });

      tabSpecBuilder = new Function<TabHost.TabSpec>() {
        @Override
        public android.widget.TabHost.TabSpec evaluate() {
          return host.newTabSpec(TabSpec.this.tag);
        }
      };
      try {
        tabBuilder = new Function<ActionBar.Tab>() {
          @Override
          public Tab evaluate() {
            return activity.getActionBar().newTab().setTag(TabSpec.this.tag);
          }
        };
      }
      catch (VerifyError e) {
      }
    }

    public TabSpec setContent(final int viewId) {
      final Function<TabHost.TabSpec> currentTabSpecBuilder = tabSpecBuilder;
      final Function<ActionBar.Tab> currentTabBuilder = tabBuilder;
      tabSpecBuilder = new Function<TabHost.TabSpec>() {
        @Override
        public android.widget.TabHost.TabSpec evaluate() {
          return currentTabSpecBuilder.evaluate().setContent(viewId);
        }
      };
      try {
        tabBuilder = new Function<ActionBar.Tab>() {
          @Override
          public Tab evaluate() {
            return currentTabBuilder.evaluate().setTabListener(
                setCurrentTabListener);
          }
        };
      }
      catch (VerifyError e) {
      }
      return this;
    }

    public TabSpec setContent(final Intent intent) {
      final Function<TabHost.TabSpec> currentTabSpecBuilder = tabSpecBuilder;
      final Function<ActionBar.Tab> currentTabBuilder = tabBuilder;
      tabSpecBuilder = new Function<TabHost.TabSpec>() {
        @Override
        public android.widget.TabHost.TabSpec evaluate() {
          return currentTabSpecBuilder.evaluate().setContent(intent);
        }
      };
      try {
        tabBuilder = new Function<ActionBar.Tab>() {
          @Override
          public Tab evaluate() {
            return currentTabBuilder.evaluate().setTabListener(
                setCurrentTabListener);
          }
        };
      }
      catch (VerifyError e) {
      }
      return this;
    }

    public TabSpec setContent(final TabHost.TabContentFactory contentFactory) {
      final Function<TabHost.TabSpec> currentTabSpecBuilder = tabSpecBuilder;
      final Function<ActionBar.Tab> currentTabBuilder = tabBuilder;
      tabSpecBuilder = new Function<TabHost.TabSpec>() {
        @Override
        public android.widget.TabHost.TabSpec evaluate() {
          return currentTabSpecBuilder.evaluate().setContent(contentFactory);
        }
      };
      try {
        tabBuilder = new Function<ActionBar.Tab>() {
          @Override
          public Tab evaluate() {
            return currentTabBuilder.evaluate().setTabListener(
                setCurrentTabListener);
          }
        };
      }
      catch (VerifyError e) {
      }
      return this;
    }

    public TabSpec setIndicator(final CharSequence label) {
      final Function<TabHost.TabSpec> currentTabSpecBuilder = tabSpecBuilder;
      final Function<ActionBar.Tab> currentTabBuilder = tabBuilder;
      tabSpecBuilder = new Function<TabHost.TabSpec>() {
        @Override
        public android.widget.TabHost.TabSpec evaluate() {
          return currentTabSpecBuilder.evaluate().setIndicator(label);
        }
      };
      try {
        tabBuilder = new Function<ActionBar.Tab>() {
          @Override
          public Tab evaluate() {
            return currentTabBuilder.evaluate().setText(label);
          }
        };
      }
      catch (VerifyError e) {
      }
      return this;
    }

    public TabSpec setIndicator(final View view) {
      final Function<TabHost.TabSpec> currentTabSpecBuilder = tabSpecBuilder;
      final Function<ActionBar.Tab> currentTabBuilder = tabBuilder;
      tabSpecBuilder = new Function<TabHost.TabSpec>() {
        @Override
        public android.widget.TabHost.TabSpec evaluate() {
          return currentTabSpecBuilder.evaluate().setIndicator(view);
        }
      };
      try {
        tabBuilder = new Function<ActionBar.Tab>() {
          @Override
          public Tab evaluate() {
            return currentTabBuilder.evaluate().setCustomView(view);
          }
        };
      }
      catch (VerifyError e) {
      }
      return this;
    }

    public TabSpec setIndicator(final CharSequence label, final Drawable icon) {
      final Function<TabHost.TabSpec> currentTabSpecBuilder = tabSpecBuilder;
      final Function<ActionBar.Tab> currentTabBuilder = tabBuilder;
      tabSpecBuilder = new Function<TabHost.TabSpec>() {
        @Override
        public android.widget.TabHost.TabSpec evaluate() {
          return currentTabSpecBuilder.evaluate().setIndicator(label, icon);
        }
      };
      try {
        tabBuilder = new Function<ActionBar.Tab>() {
          @Override
          public Tab evaluate() {
            return currentTabBuilder.evaluate().setText(label).setIcon(icon);
          }
        };
      }
      catch (VerifyError e) {
      }
      return this;
    }

    public TabHost.TabSpec buildTabSpec() {
      return tabSpecBuilder.evaluate();
    }

    public Object buildTab() {
      return tabBuilder.evaluate();
    }
  }
}
