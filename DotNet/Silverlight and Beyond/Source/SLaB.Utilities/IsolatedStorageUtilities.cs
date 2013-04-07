using System;
using System.Collections.Generic;
using System.ComponentModel;

namespace SLaB.Utilities
{
    /// <summary>
    /// Contains utilities for working with IsolatedStorage.
    /// </summary>
    public static class IsolatedStorageUtilities
    {


        /// <summary>
        /// Invokes the setter if the dictionary contains the given key.
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="settings">The settings.</param>
        /// <param name="key">The key.</param>
        /// <param name="setter">The setter.</param>
        public static void SetIfContainsKey<T>(this IDictionary<string, object> settings, string key, Action<T> setter)
        {
            if (!DesignerProperties.IsInDesignTool && settings.ContainsKey(key))
                setter((T)settings[key]);
        }

        /// <summary>
        /// Sets the key in the dictionary to the value if not in design mode.
        /// </summary>
        /// <param name="settings">The settings.</param>
        /// <param name="key">The key.</param>
        /// <param name="value">The value.</param>
        public static void SetIfNotInDesignMode(this IDictionary<string, object> settings, string key, object value)
        {
            if (!DesignerProperties.IsInDesignTool)
                settings[key] = value;
        }
    }
}
