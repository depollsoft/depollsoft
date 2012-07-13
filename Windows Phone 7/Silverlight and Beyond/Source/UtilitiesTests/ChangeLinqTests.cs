using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using Microsoft.Silverlight.Testing;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SLaB.Utilities.ChangeLinq;

namespace UtilitiesTests
{
    [TestClass]
    public class ChangeLinqTests : SilverlightTest
    {
        private ObservableCollection<Person> _People;

        [TestInitialize]
        public void Initialize()
        {
            this._People = new ObservableCollection<Person>
                {
                new Person{FirstName="David", LastName="Poll", Age=22},
                new Person{FirstName="Joe", LastName="Schmoe", Age=53},
                new Person{FirstName="Jack", LastName="Poll", Age=17},
                new Person{FirstName="George", LastName="Washington", Age=72},
                new Person{FirstName="John", LastName="Adams", Age=46},
                new Person{FirstName="Thomas", LastName="Jefferson", Age=39},
                new Person{FirstName="James", LastName="Madison", Age=12},
                new Person{FirstName="James", LastName="Monroe", Age=29},
                new Person{FirstName="John Q.", LastName="Adams", Age=51},
                new Person{FirstName="John", LastName="Doe", Age=99}
            };
        }

        [TestMethod]
        public void TestOneOne()
        {
            List<Person> changes = new List<Person>();
            var result = this._People.AsChangeLinq();
            Assert.AreEqual("David", result.ElementAt(0).FirstName);
            Assert.AreEqual("Joe", result.ElementAt(1).FirstName);
            Assert.AreEqual("Jack", result.ElementAt(2).FirstName);
            Assert.AreEqual("George", result.ElementAt(3).FirstName);
            Assert.AreEqual("John", result.ElementAt(4).FirstName);
            Assert.AreEqual("Thomas", result.ElementAt(5).FirstName);
            Assert.AreEqual("James", result.ElementAt(6).FirstName);
            Assert.AreEqual("James", result.ElementAt(7).FirstName);
            Assert.AreEqual("John Q.", result.ElementAt(8).FirstName);
            Assert.AreEqual("John", result.ElementAt(9).FirstName);
            result.CollectionChanged += (sender, args) =>
            {
                if (args.NewItems != null)
                    changes.AddRange(args.NewItems.Cast<Person>());
                if (args.OldItems != null)
                    changes.AddRange(args.OldItems.Cast<Person>());
            };
            this._People.RemoveAt(5);
            this._People.RemoveAt(3);
            this._People.Add(new Person { FirstName = "Jane", LastName = "Doe", Age = 17 });
            this._People[1] = new Person { FirstName = "Awesome", LastName = "Dude", Age = 1 };
            Assert.AreEqual("David", result.ElementAt(0).FirstName);
            Assert.AreEqual("Awesome", result.ElementAt(1).FirstName);
            Assert.AreEqual("Jack", result.ElementAt(2).FirstName);
            Assert.AreEqual("John", result.ElementAt(3).FirstName);
            Assert.AreEqual("James", result.ElementAt(4).FirstName);
            Assert.AreEqual("James", result.ElementAt(5).FirstName);
            Assert.AreEqual("John Q.", result.ElementAt(6).FirstName);
            Assert.AreEqual("John", result.ElementAt(7).FirstName);
            Assert.AreEqual("Jane", result.ElementAt(8).FirstName);
            Assert.AreEqual("Thomas", changes[0].FirstName);
            Assert.AreEqual("George", changes[1].FirstName);
            Assert.AreEqual("Jane", changes[2].FirstName);
            Assert.AreEqual("Awesome", changes[3].FirstName);
            Assert.AreEqual("Joe", changes[4].FirstName);
        }

        [TestMethod]
        public void TestConcat()
        {
            List<Person> changes = new List<Person>();
            var result = this._People.AsChangeLinq().Concat(this._People.AsChangeLinq());
            Assert.AreEqual("David", result.ElementAt(0).FirstName);
            Assert.AreEqual("Joe", result.ElementAt(1).FirstName);
            Assert.AreEqual("Jack", result.ElementAt(2).FirstName);
            Assert.AreEqual("George", result.ElementAt(3).FirstName);
            Assert.AreEqual("John", result.ElementAt(4).FirstName);
            Assert.AreEqual("Thomas", result.ElementAt(5).FirstName);
            Assert.AreEqual("James", result.ElementAt(6).FirstName);
            Assert.AreEqual("James", result.ElementAt(7).FirstName);
            Assert.AreEqual("John Q.", result.ElementAt(8).FirstName);
            Assert.AreEqual("John", result.ElementAt(9).FirstName);
            Assert.AreEqual("David", result.ElementAt(10).FirstName);
            Assert.AreEqual("Joe", result.ElementAt(11).FirstName);
            Assert.AreEqual("Jack", result.ElementAt(12).FirstName);
            Assert.AreEqual("George", result.ElementAt(13).FirstName);
            Assert.AreEqual("John", result.ElementAt(14).FirstName);
            Assert.AreEqual("Thomas", result.ElementAt(15).FirstName);
            Assert.AreEqual("James", result.ElementAt(16).FirstName);
            Assert.AreEqual("James", result.ElementAt(17).FirstName);
            Assert.AreEqual("John Q.", result.ElementAt(18).FirstName);
            Assert.AreEqual("John", result.ElementAt(19).FirstName);
            result.CollectionChanged += (sender, args) =>
            {
                if (args.NewItems != null)
                    changes.AddRange(args.NewItems.Cast<Person>());
                if (args.OldItems != null)
                    changes.AddRange(args.OldItems.Cast<Person>());
            };
            this._People.RemoveAt(5);
            this._People.RemoveAt(3);
            this._People.Add(new Person { FirstName = "Jane", LastName = "Doe", Age = 17 });
            this._People[1] = new Person { FirstName = "Awesome", LastName = "Dude", Age = 1 };
            Assert.AreEqual("David", result.ElementAt(0).FirstName);
            Assert.AreEqual("Awesome", result.ElementAt(1).FirstName);
            Assert.AreEqual("Jack", result.ElementAt(2).FirstName);
            Assert.AreEqual("John", result.ElementAt(3).FirstName);
            Assert.AreEqual("James", result.ElementAt(4).FirstName);
            Assert.AreEqual("James", result.ElementAt(5).FirstName);
            Assert.AreEqual("John Q.", result.ElementAt(6).FirstName);
            Assert.AreEqual("John", result.ElementAt(7).FirstName);
            Assert.AreEqual("Jane", result.ElementAt(8).FirstName);
            Assert.AreEqual("David", result.ElementAt(9).FirstName);
            Assert.AreEqual("Awesome", result.ElementAt(10).FirstName);
            Assert.AreEqual("Jack", result.ElementAt(11).FirstName);
            Assert.AreEqual("John", result.ElementAt(12).FirstName);
            Assert.AreEqual("James", result.ElementAt(13).FirstName);
            Assert.AreEqual("James", result.ElementAt(14).FirstName);
            Assert.AreEqual("John Q.", result.ElementAt(15).FirstName);
            Assert.AreEqual("John", result.ElementAt(16).FirstName);
            Assert.AreEqual("Jane", result.ElementAt(17).FirstName);
            Assert.AreEqual("Thomas", changes[0].FirstName);
            Assert.AreEqual("Thomas", changes[1].FirstName);
            Assert.AreEqual("George", changes[2].FirstName);
            Assert.AreEqual("George", changes[3].FirstName);
            Assert.AreEqual("Jane", changes[4].FirstName);
            Assert.AreEqual("Jane", changes[5].FirstName);
            Assert.AreEqual("Awesome", changes[6].FirstName);
            Assert.AreEqual("Joe", changes[7].FirstName);
            Assert.AreEqual("Awesome", changes[8].FirstName);
            Assert.AreEqual("Joe", changes[9].FirstName);
        }

        [TestMethod]
        public void TestWhereAcceptAll()
        {
            List<Person> changes = new List<Person>();
            var result = this._People.AsChangeLinq().Where(item => true);
            Assert.AreEqual("David", result.ElementAt(0).FirstName);
            Assert.AreEqual("Joe", result.ElementAt(1).FirstName);
            Assert.AreEqual("Jack", result.ElementAt(2).FirstName);
            Assert.AreEqual("George", result.ElementAt(3).FirstName);
            Assert.AreEqual("John", result.ElementAt(4).FirstName);
            Assert.AreEqual("Thomas", result.ElementAt(5).FirstName);
            Assert.AreEqual("James", result.ElementAt(6).FirstName);
            Assert.AreEqual("James", result.ElementAt(7).FirstName);
            Assert.AreEqual("John Q.", result.ElementAt(8).FirstName);
            Assert.AreEqual("John", result.ElementAt(9).FirstName);
            result.CollectionChanged += (sender, args) =>
            {
                if (args.NewItems != null)
                    changes.AddRange(args.NewItems.Cast<Person>());
                if (args.OldItems != null)
                    changes.AddRange(args.OldItems.Cast<Person>());
            };
            this._People.RemoveAt(5);
            this._People.RemoveAt(3);
            this._People.Add(new Person { FirstName = "Jane", LastName = "Doe", Age = 17 });
            this._People[1] = new Person { FirstName = "Awesome", LastName = "Dude", Age = 1 };
            Assert.AreEqual("David", result.ElementAt(0).FirstName);
            Assert.AreEqual("Awesome", result.ElementAt(1).FirstName);
            Assert.AreEqual("Jack", result.ElementAt(2).FirstName);
            Assert.AreEqual("John", result.ElementAt(3).FirstName);
            Assert.AreEqual("James", result.ElementAt(4).FirstName);
            Assert.AreEqual("James", result.ElementAt(5).FirstName);
            Assert.AreEqual("John Q.", result.ElementAt(6).FirstName);
            Assert.AreEqual("John", result.ElementAt(7).FirstName);
            Assert.AreEqual("Jane", result.ElementAt(8).FirstName);
            Assert.AreEqual("Thomas", changes[0].FirstName);
            Assert.AreEqual("George", changes[1].FirstName);
            Assert.AreEqual("Jane", changes[2].FirstName);
            Assert.AreEqual("Awesome", changes[3].FirstName);
            Assert.AreEqual("Joe", changes[4].FirstName);
        }

        [TestMethod]
        public void TestSelectOperator()
        {
            List<string> changes = new List<string>();
            var result = from person in this._People.AsChangeLinq()
                         select person.FirstName;
            Assert.AreEqual("David", result.ElementAt(0));
            Assert.AreEqual("Joe", result.ElementAt(1));
            Assert.AreEqual("Jack", result.ElementAt(2));
            Assert.AreEqual("George", result.ElementAt(3));
            Assert.AreEqual("John", result.ElementAt(4));
            Assert.AreEqual("Thomas", result.ElementAt(5));
            Assert.AreEqual("James", result.ElementAt(6));
            Assert.AreEqual("James", result.ElementAt(7));
            Assert.AreEqual("John Q.", result.ElementAt(8));
            Assert.AreEqual("John", result.ElementAt(9));
            result.CollectionChanged += (sender, args) =>
            {
                if (args.NewItems != null)
                    changes.AddRange(args.NewItems.Cast<string>());
                if (args.OldItems != null)
                    changes.AddRange(args.OldItems.Cast<string>());
            };
            this._People.RemoveAt(5);
            this._People.RemoveAt(3);
            this._People.Add(new Person { FirstName = "Jane", LastName = "Doe", Age = 17 });
            this._People[1] = new Person { FirstName = "Awesome", LastName = "Dude", Age = 1 };
            Assert.AreEqual("David", result.ElementAt(0));
            Assert.AreEqual("Awesome", result.ElementAt(1));
            Assert.AreEqual("Jack", result.ElementAt(2));
            Assert.AreEqual("John", result.ElementAt(3));
            Assert.AreEqual("James", result.ElementAt(4));
            Assert.AreEqual("James", result.ElementAt(5));
            Assert.AreEqual("John Q.", result.ElementAt(6));
            Assert.AreEqual("John", result.ElementAt(7));
            Assert.AreEqual("Jane", result.ElementAt(8));
            Assert.AreEqual("Thomas", changes[0]);
            Assert.AreEqual("George", changes[1]);
            Assert.AreEqual("Jane", changes[2]);
            Assert.AreEqual("Awesome", changes[3]);
            Assert.AreEqual("Joe", changes[4]);
        }

        [TestMethod]
        public void TestWhereOperator()
        {
            List<Person> changes = new List<Person>();
            var result = this._People.AsChangeLinq().Where(item => item.FirstName.StartsWith("J"));
            Assert.AreEqual("Joe", result.ElementAt(0).FirstName);
            Assert.AreEqual("Jack", result.ElementAt(1).FirstName);
            Assert.AreEqual("John", result.ElementAt(2).FirstName);
            Assert.AreEqual("James", result.ElementAt(3).FirstName);
            Assert.AreEqual("James", result.ElementAt(4).FirstName);
            Assert.AreEqual("John Q.", result.ElementAt(5).FirstName);
            Assert.AreEqual("John", result.ElementAt(6).FirstName);
            result.CollectionChanged += (sender, args) =>
            {
                if (args.NewItems != null)
                    changes.AddRange(args.NewItems.Cast<Person>());
                if (args.OldItems != null)
                    changes.AddRange(args.OldItems.Cast<Person>());
            };
            this._People.RemoveAt(5);
            this._People.RemoveAt(3);
            this._People.Add(new Person { FirstName = "Jane", LastName = "Doe", Age = 17 });
            this._People[1] = new Person { FirstName = "Awesome", LastName = "Dude", Age = 1 };
            Assert.AreEqual("Jack", result.ElementAt(0).FirstName);
            Assert.AreEqual("John", result.ElementAt(1).FirstName);
            Assert.AreEqual("James", result.ElementAt(2).FirstName);
            Assert.AreEqual("James", result.ElementAt(3).FirstName);
            Assert.AreEqual("John Q.", result.ElementAt(4).FirstName);
            Assert.AreEqual("John", result.ElementAt(5).FirstName);
            Assert.AreEqual("Jane", result.ElementAt(6).FirstName);
            Assert.AreEqual("Jane", changes[0].FirstName);
            Assert.AreEqual("Joe", changes[1].FirstName);
        }

        [TestMethod]
        public void TestSelectWhere()
        {
            List<string> changes = new List<string>();
            var result = from person in this._People.AsChangeLinq()
                         where person.FirstName.StartsWith("J")
                         select person.FirstName;
            Assert.AreEqual("Joe", result.ElementAt(0));
            Assert.AreEqual("Jack", result.ElementAt(1));
            Assert.AreEqual("John", result.ElementAt(2));
            Assert.AreEqual("James", result.ElementAt(3));
            Assert.AreEqual("James", result.ElementAt(4));
            Assert.AreEqual("John Q.", result.ElementAt(5));
            Assert.AreEqual("John", result.ElementAt(6));
            result.CollectionChanged += (sender, args) =>
            {
                if (args.NewItems != null)
                    changes.AddRange(args.NewItems.Cast<string>());
                if (args.OldItems != null)
                    changes.AddRange(args.OldItems.Cast<string>());
            };
            this._People.RemoveAt(5);
            this._People.RemoveAt(3);
            this._People.Add(new Person { FirstName = "Jane", LastName = "Doe", Age = 17 });
            this._People[1] = new Person { FirstName = "Awesome", LastName = "Dude", Age = 1 };
            Assert.AreEqual("Jack", result.ElementAt(0));
            Assert.AreEqual("John", result.ElementAt(1));
            Assert.AreEqual("James", result.ElementAt(2));
            Assert.AreEqual("James", result.ElementAt(3));
            Assert.AreEqual("John Q.", result.ElementAt(4));
            Assert.AreEqual("John", result.ElementAt(5));
            Assert.AreEqual("Jane", result.ElementAt(6));
            Assert.AreEqual("Jane", changes[0]);
            Assert.AreEqual("Joe", changes[1]);
        }
    }
}
