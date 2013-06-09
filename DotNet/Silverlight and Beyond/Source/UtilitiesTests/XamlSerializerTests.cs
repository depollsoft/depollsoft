using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Markup;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SLaB.Utilities.Xaml.Collections;
using SLaB.Utilities.Xaml.Serializer;
using SLaB.Utilities.Xaml.Serializer.UI;
//using SLaB.Utilities.Xaml.Deserializer;

namespace UtilitiesTests
{
    [TestClass]
    public class XamlSerializerTests
    {
        public XamlSerializerTests()
        {
            new UiXamlSerializer();
        }

        //[TestMethod]
        //public void TestDeserialize()
        //{
        //    string xaml = new StreamReader(Application.GetResourceStream(new Uri("/UtilitiesTests;component/TestUserControl.xaml", UriKind.Relative)).Stream).ReadToEnd();
        //    var result = XamlDeserializer.Deserialize(xaml);
        //}

        [TestMethod]
        public void TestBasicBuiltInOnly()
        {
            BuiltInTypeObject obj = new BuiltInTypeObject
            {
                BoolValue = true,
                DoubleValue = 1.9493419349,
                StringValue = "Hello, world!",
                IntValue = 1337,
                UriValue = new Uri("http://www.davidpoll.com", UriKind.Absolute)
            };
            string result = new XamlSerializer().Serialize(obj);
            var output = XamlReader.Load(result);
            Assert.IsInstanceOfType(output, typeof(BuiltInTypeObject));
            BuiltInTypeObject afterObj = output as BuiltInTypeObject;
            Assert.AreEqual(obj.BoolValue, afterObj.BoolValue);
            Assert.AreEqual(obj.DoubleValue, afterObj.DoubleValue, 0.0001);
            Assert.AreEqual(obj.IntValue, afterObj.IntValue);
            Assert.AreEqual(obj.StringValue, afterObj.StringValue);
            Assert.AreEqual(obj.UriValue, afterObj.UriValue);
        }

        [TestMethod]
        public void TestBasicBuiltInOnly2()
        {
            BuiltInTypeObject obj = new BuiltInTypeObject
            {
                BoolValue = false,
                DoubleValue = 1000001.2,
                StringValue = "Bonjour, Monde!",
                IntValue = 7847009,
                UriValue = new Uri("/Woohoo!", UriKind.Relative)
            };
            string result = new XamlSerializer().Serialize(obj);
            var output = XamlReader.Load(result);
            Assert.IsInstanceOfType(output, typeof(BuiltInTypeObject));
            BuiltInTypeObject afterObj = output as BuiltInTypeObject;
            Assert.AreEqual(obj.BoolValue, afterObj.BoolValue);
            Assert.AreEqual(obj.DoubleValue, afterObj.DoubleValue, 0.1);
            Assert.AreEqual(obj.IntValue, afterObj.IntValue);
            Assert.AreEqual(obj.StringValue, afterObj.StringValue);
            Assert.AreEqual(obj.UriValue, afterObj.UriValue);
        }

        [TestMethod]
        public void TestBuiltIns()
        {
            XamlSerializer xs = new XamlSerializer();
            string serialized = xs.Serialize("Hello, world!");
            Assert.AreEqual("Hello, world!", (string)XamlReader.Load(serialized));
            serialized = xs.Serialize(123456789);
            Assert.AreEqual(123456789, (int)XamlReader.Load(serialized));
            serialized = xs.Serialize(1.023414);
            Assert.AreEqual(1.023414, (double)XamlReader.Load(serialized), 0.0001);
            serialized = xs.Serialize(false);
            Assert.AreEqual(false, (bool)XamlReader.Load(serialized));
        }

        [TestMethod]
        public void TestBuiltInsBraces()
        {
            XamlSerializer xs = new XamlSerializer();
            string serialized = xs.Serialize("Hello, {world!}");
            Assert.AreEqual("Hello, {world!}", (string)XamlReader.Load(serialized));
            serialized = xs.Serialize(123456789);
            Assert.AreEqual(123456789, (int)XamlReader.Load(serialized));
            serialized = xs.Serialize(1.023414);
            Assert.AreEqual(1.023414, (double)XamlReader.Load(serialized), 0.0001);
            serialized = xs.Serialize(false);
            Assert.AreEqual(false, (bool)XamlReader.Load(serialized));
        }

        [TestMethod]
        public void TestBuiltInsBraces2()
        {
            XamlSerializer xs = new XamlSerializer();
            string serialized = xs.Serialize("{Hello, world!}");
            Assert.AreEqual("{Hello, world!}", (string)XamlReader.Load(serialized));
            serialized = xs.Serialize(123456789);
            Assert.AreEqual(123456789, (int)XamlReader.Load(serialized));
            serialized = xs.Serialize(1.023414);
            Assert.AreEqual(1.023414, (double)XamlReader.Load(serialized), 0.0001);
            serialized = xs.Serialize(false);
            Assert.AreEqual(false, (bool)XamlReader.Load(serialized));
        }

        [TestMethod]
        public void TestNestedAndBuiltIns()
        {
            NestedObject obj = new NestedObject
            {
                BoolValue = true,
                DoubleValue = 1.9493419349,
                StringValue = "Hello, world!",
                IntValue = 1337,
                UriValue = new Uri("http://www.davidpoll.com", UriKind.Absolute),
                NestedValue = new NestedObject
                {
                    BoolValue = false,
                    DoubleValue = 1000001.2,
                    StringValue = "Bonjour, Monde!",
                    IntValue = 7847009,
                    UriValue = new Uri("/Woohoo!", UriKind.Relative),
                    //OtherObject = new Button
                    //{
                    //    Tag = "Hiya!",
                    //    Content = new TextBlock { Text = "Howdy!" },
                    //    Resources = new ResourceDictionary() { { "a", "b" }, { "c", new Style() } }
                    //}
                },
            };
            obj.ListValue.Add(1);
            obj.ListValue.Add(2);
            obj.ListValue.Add(1);
            obj.ListValue.Add(4);
            obj.DictValue["1234"] = "Hello \n !";
            string result = new XamlSerializer().Serialize(obj);
            var output = XamlReader.Load(result);
            Assert.IsInstanceOfType(output, typeof(NestedObject));
            NestedObject afterObj = output as NestedObject;
            Assert.AreEqual(obj.BoolValue, afterObj.BoolValue);
            Assert.AreEqual(obj.DoubleValue, afterObj.DoubleValue, 0.0001);
            Assert.AreEqual(obj.IntValue, afterObj.IntValue);
            Assert.AreEqual(obj.StringValue, afterObj.StringValue);
            Assert.AreEqual(obj.UriValue, afterObj.UriValue);
            obj = obj.NestedValue;
            afterObj = afterObj.NestedValue;
            Assert.AreEqual(obj.BoolValue, afterObj.BoolValue);
            Assert.AreEqual(obj.DoubleValue, afterObj.DoubleValue, 0.1);
            Assert.AreEqual(obj.IntValue, afterObj.IntValue);
            Assert.AreEqual(obj.StringValue, afterObj.StringValue);
            Assert.AreEqual(obj.UriValue, afterObj.UriValue);
        }

        [TestMethod]
        public void TestNestedAndBuiltInsBraces()
        {
            NestedObject obj = new NestedObject
            {
                BoolValue = true,
                DoubleValue = 1.9493419349,
                StringValue = "Hello, {world!}",
                IntValue = 1337,
                UriValue = new Uri("http://www.davidpoll.com", UriKind.Absolute),
                NestedValue = new NestedObject
                {
                    BoolValue = false,
                    DoubleValue = 1000001.2,
                    StringValue = "Bonjour, Monde!",
                    IntValue = 7847009,
                    UriValue = new Uri("/Woohoo!", UriKind.Relative),
                },
            };
            obj.ListValue.Add(1);
            obj.ListValue.Add(2);
            obj.ListValue.Add(1);
            obj.ListValue.Add(4);
            obj.DictValue["1234"] = "Hello \n !";
            string result = new XamlSerializer().Serialize(obj);
            var output = XamlReader.Load(result);
            Assert.IsInstanceOfType(output, typeof(NestedObject));
            NestedObject afterObj = output as NestedObject;
            Assert.AreEqual(obj.BoolValue, afterObj.BoolValue);
            Assert.AreEqual(obj.DoubleValue, afterObj.DoubleValue, 0.0001);
            Assert.AreEqual(obj.IntValue, afterObj.IntValue);
            Assert.AreEqual(obj.StringValue, afterObj.StringValue);
            Assert.AreEqual(obj.UriValue, afterObj.UriValue);
            obj = obj.NestedValue;
            afterObj = afterObj.NestedValue;
            Assert.AreEqual(obj.BoolValue, afterObj.BoolValue);
            Assert.AreEqual(obj.DoubleValue, afterObj.DoubleValue, 0.1);
            Assert.AreEqual(obj.IntValue, afterObj.IntValue);
            Assert.AreEqual(obj.StringValue, afterObj.StringValue);
            Assert.AreEqual(obj.UriValue, afterObj.UriValue);
        }

        [TestMethod]
        public void TestNestedAndBuiltInsBraces2()
        {
            NestedObject obj = new NestedObject
            {
                BoolValue = true,
                DoubleValue = 1.9493419349,
                StringValue = "{Hello, world!}",
                IntValue = 1337,
                UriValue = new Uri("http://www.davidpoll.com", UriKind.Absolute),
                NestedValue = new NestedObject
                {
                    BoolValue = false,
                    DoubleValue = 1000001.2,
                    StringValue = "Bonjour, Monde!",
                    IntValue = 7847009,
                    UriValue = new Uri("/Woohoo!", UriKind.Relative),
                },
            };
            obj.ListValue.Add(1);
            obj.ListValue.Add(2);
            obj.ListValue.Add(1);
            obj.ListValue.Add(4);
            obj.DictValue["1234"] = "Hello \n !";
            string result = new XamlSerializer().Serialize(obj);
            var output = XamlReader.Load(result);
            Assert.IsInstanceOfType(output, typeof(NestedObject));
            NestedObject afterObj = output as NestedObject;
            Assert.AreEqual(obj.BoolValue, afterObj.BoolValue);
            Assert.AreEqual(obj.DoubleValue, afterObj.DoubleValue, 0.0001);
            Assert.AreEqual(obj.IntValue, afterObj.IntValue);
            Assert.AreEqual(obj.StringValue, afterObj.StringValue);
            Assert.AreEqual(obj.UriValue, afterObj.UriValue);
            obj = obj.NestedValue;
            afterObj = afterObj.NestedValue;
            Assert.AreEqual(obj.BoolValue, afterObj.BoolValue);
            Assert.AreEqual(obj.DoubleValue, afterObj.DoubleValue, 0.1);
            Assert.AreEqual(obj.IntValue, afterObj.IntValue);
            Assert.AreEqual(obj.StringValue, afterObj.StringValue);
            Assert.AreEqual(obj.UriValue, afterObj.UriValue);
        }

        [TestMethod]
        public void TestAttached()
        {
            NestedObject obj = new NestedObject
            {
                BoolValue = true,
                DoubleValue = 1.9493419349,
                StringValue = "Hello, world!",
                IntValue = 1337,
                UriValue = new Uri("http://www.davidpoll.com", UriKind.Absolute),
                NestedValue = new NestedObject
                {
                    BoolValue = false,
                    DoubleValue = 1000001.2,
                    StringValue = "Bonjour, Monde!",
                    IntValue = 7847009,
                    UriValue = new Uri("/Woohoo!", UriKind.Relative),
                    //OtherObject = new Button
                    //{
                    //    Tag = "Hiya!",
                    //    Content = new TextBlock { Text = "Howdy!" },
                    //    Resources = new ResourceDictionary() { { "a", "b" }, { "c", new Style() } }
                    //}
                },
            };
            obj.ListValue.Add(1);
            obj.ListValue.Add(2);
            obj.ListValue.Add(1);
            obj.ListValue.Add(4);
            obj.DictValue["1234"] = "Hello \n !";
            AttachedProps.SetAttachedObject(obj, "Some attached value");
            AttachedProps.GetAttachedDictionary(obj)["test"] = "test value";
            AttachedProps.SetAttachedObject(obj.NestedValue, new BindableDictionary { { "a", "b" }, { "c", "d" } });
            XamlSerializer xs = new XamlSerializer();
            xs.DiscoverAttachedProperties(typeof(AttachedProps));
            string result = xs.Serialize(obj);
            var output = XamlReader.Load(result);
            Assert.IsInstanceOfType(output, typeof(NestedObject));
            NestedObject afterObj = output as NestedObject;
            Assert.AreEqual(obj.BoolValue, afterObj.BoolValue);
            Assert.AreEqual(obj.DoubleValue, afterObj.DoubleValue, 0.0001);
            Assert.AreEqual(obj.IntValue, afterObj.IntValue);
            Assert.AreEqual(obj.StringValue, afterObj.StringValue);
            Assert.AreEqual(obj.UriValue, afterObj.UriValue);
            Assert.AreEqual(AttachedProps.GetAttachedObject(obj), AttachedProps.GetAttachedObject(afterObj));
            Assert.AreEqual(AttachedProps.GetAttachedDictionary(obj)["test"], AttachedProps.GetAttachedDictionary(afterObj)["test"]);
            obj = obj.NestedValue;
            afterObj = afterObj.NestedValue;
            Assert.AreEqual(obj.BoolValue, afterObj.BoolValue);
            Assert.AreEqual(obj.DoubleValue, afterObj.DoubleValue, 0.1);
            Assert.AreEqual(obj.IntValue, afterObj.IntValue);
            Assert.AreEqual(obj.StringValue, afterObj.StringValue);
            Assert.AreEqual(obj.UriValue, afterObj.UriValue);
            Assert.IsInstanceOfType(AttachedProps.GetAttachedObject(afterObj), typeof(BindableDictionary));
        }

        [TestMethod]
        public void TestUiSerializer()
        {
            Button b = new Button();
            TextBox tb = new TextBox();
            Grid g = new Grid();
            g.Tag = "asdf";
            g.Children.Add(b);
            g.Children.Add(tb);
            Grid.SetRow(tb, 1);
            tb.Tag = "Testing";
            UiXamlSerializer uxs = new UiXamlSerializer();
            string result = uxs.Serialize(g);
            object output = XamlReader.Load(result);
            Assert.IsInstanceOfType(output, typeof(Grid));
            Grid hydrated = g as Grid;
            Assert.AreEqual(g.Tag, hydrated.Tag);
            Assert.IsInstanceOfType(hydrated.Children[0], typeof(Button));
            Assert.IsInstanceOfType(hydrated.Children[1], typeof(TextBox));
            Assert.AreEqual((g.Children[1] as Control).Tag, (hydrated.Children[1] as Control).Tag);
        }

        [TestMethod]
        public void TestUiSerializerBinding()
        {
            Button b = new Button();
            TextBox tb = new TextBox();
            Grid g = new Grid();
            g.Children.Add(b);
            g.Children.Add(tb);
            g.SetBinding(FrameworkElement.TagProperty, new Binding("SomeProperty.FooBar"));
            Grid.SetRow(tb, 1);
            tb.Tag = "Testing";
            b.Name = "someButton";
            UiXamlSerializer uxs = new UiXamlSerializer();
            string result = uxs.Serialize(g);
            object output = XamlReader.Load(result);
            Assert.IsInstanceOfType(output, typeof(Grid));
            Grid hydrated = g as Grid;
            Assert.AreEqual(g.Tag, hydrated.Tag);
            Assert.IsInstanceOfType(hydrated.Children[0], typeof(Button));
            Assert.IsInstanceOfType(hydrated.Children[1], typeof(TextBox));
            Assert.AreEqual((g.Children[1] as Control).Tag, (hydrated.Children[1] as Control).Tag);
        }

        [TestMethod]
        public void TestFullUi()
        {
            TestUserControl tuc = new TestUserControl();
            UiXamlSerializer uxs = new UiXamlSerializer();
            uxs.DiscoverAttachedProperties(typeof(AttachedProps));
            string result = uxs.Serialize(tuc.Content);
            object output = XamlReader.Load(result);
            Assert.IsInstanceOfType(output, typeof(Grid));
        }

        [TestMethod]
        public void TestFullUiRoundTrip()
        {
            TestUserControl tuc = new TestUserControl();
            UiXamlSerializer uxs = new UiXamlSerializer();
            uxs.DiscoverAttachedProperties(typeof(AttachedProps));
            string result = uxs.Serialize(tuc.Content);
            object output = XamlReader.Load(result);
            string secondResult = uxs.Serialize(output);
            object secondOutput = XamlReader.Load(secondResult);
            Assert.IsInstanceOfType(output, typeof(Grid));
            Assert.AreEqual(result, secondResult);
        }

        [TestMethod]
        public void TestUiSerializerStaticResource()
        {
            Button b = new Button();
            TextBox tb = new TextBox();
            Grid g = new Grid();
            g.Resources.Add("someValue", new Person { FirstName = "David" });
            b.Content = g.Resources["someValue"];
            g.Children.Add(b);
            g.Children.Add(tb);
            g.SetBinding(FrameworkElement.TagProperty, new Binding("SomeProperty.FooBar"));
            Grid.SetRow(tb, 1);
            tb.Tag = "Testing";
            b.Name = "someButton";
            UiXamlSerializer uxs = new UiXamlSerializer();
            string result = uxs.Serialize(g);
            object output = XamlReader.Load(result);
            Assert.IsInstanceOfType(output, typeof(Grid));
            Grid hydrated = g as Grid;
            Assert.AreEqual(g.Tag, hydrated.Tag);
            Assert.IsInstanceOfType(hydrated.Children[0], typeof(Button));
            Assert.IsInstanceOfType(((Button)hydrated.Children[0]).Content, typeof(Person));
            Assert.IsInstanceOfType(hydrated.Children[1], typeof(TextBox));
            Assert.AreEqual((g.Children[1] as Control).Tag, (hydrated.Children[1] as Control).Tag);
        }
    }
}
