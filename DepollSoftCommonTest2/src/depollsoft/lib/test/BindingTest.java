package depollsoft.lib.test;

import depollsoft.lib.binding.Binding;
import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.Function;
import depollsoft.lib.util.Property;
import depollsoft.lib.util.ReflectedProperty;
import junit.framework.Assert;
import junit.framework.TestCase;

public class BindingTest extends TestCase
{
   public void testNestedTwoWayBinding()
   {
      final Nestable n1 = new Nestable();
      n1.setValue("Hello!");
      final Nestable n2 = new Nestable();
      n2.setValue("Bonjour!");
      n2.setChild(new Nestable());
      n2.getChild().setValue("Yo!");

      Binding b = new Binding(new ReflectedProperty(n1, "Value"),
            new ReflectedProperty(n2, "Child.Value"), BindingMode.TwoWay);

      b.bind(n1);
      Assert.assertEquals(n1.getValue(), "Yo!");
      Assert.assertEquals(n2.getValue(), "Bonjour!");
      Assert.assertEquals(n2.getChild().getValue(), "Yo!");
      n1.setValue("Shalom!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getChild().getValue(), "Shalom!");
      n2.setValue("Hola!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getChild().getValue(), "Shalom!");
      n2.getChild().setValue("Sup?");
      Assert.assertEquals(n1.getValue(), "Sup?");
      Assert.assertEquals(n2.getChild().getValue(), "Sup?");
      n2.setChild(null);
      Assert.assertEquals(n1.getValue(), "Sup?");
      n2.setChild(new Nestable());
      Assert.assertEquals(n1.getValue(), null);
      n2.getChild().setValue("Woohoo!");
      Assert.assertEquals(n1.getValue(), "Woohoo!");
      Assert.assertEquals(n2.getChild().getValue(), "Woohoo!");
      Nestable newChild = new Nestable();
      newChild.setValue("Hah!");
      n2.setChild(newChild);
      Assert.assertEquals(n1.getValue(), "Hah!");
      Assert.assertEquals(n2.getChild().getValue(), "Hah!");
      n1.setValue("Shalom!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getChild().getValue(), "Shalom!");
   }

   public void testOneWayBinding()
   {
      final Nestable n1 = new Nestable();
      n1.setValue("Hello!");
      final Nestable n2 = new Nestable();
      n2.setValue("Bonjour!");

      Binding b = new Binding(new Property<String>(new Function<String>()
      {
         @Override
         public String evaluate()
         {
            // TODO Auto-generated method stub
            return n1.getValue();
         }
      }, new Action<String>()
      {

         @Override
         public void invoke(String parameter)
         {
            n1.setValue(parameter);
         }
      }, String.class), new Property<String>(new Function<String>()
      {

         @Override
         public String evaluate()
         {
            return n2.getValue();
         }
      }, new Action<String>()
      {

         @Override
         public void invoke(String parameter)
         {
            n2.setValue(parameter);
         }
      }, String.class), BindingMode.OneWay);

      b.bind(n1);
      Assert.assertEquals(n1.getValue(), "Bonjour!");
      Assert.assertEquals(n2.getValue(), "Bonjour!");
      n1.setValue("Shalom!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getValue(), "Bonjour!");
      n2.setValue("Hola!");
      Assert.assertEquals(n1.getValue(), "Hola!");
      Assert.assertEquals(n2.getValue(), "Hola!");
   }

   public void testOneWayBindingReflected()
   {
      final Nestable n1 = new Nestable();
      n1.setValue("Hello!");
      final Nestable n2 = new Nestable();
      n2.setValue("Bonjour!");

      Binding b = new Binding(new ReflectedProperty(n1, "Value"),
            new ReflectedProperty(n2, "Value"), BindingMode.OneWay);

      b.bind(n1);
      Assert.assertEquals(n1.getValue(), "Bonjour!");
      Assert.assertEquals(n2.getValue(), "Bonjour!");
      n1.setValue("Shalom!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getValue(), "Bonjour!");
      n2.setValue("Hola!");
      Assert.assertEquals(n1.getValue(), "Hola!");
      Assert.assertEquals(n2.getValue(), "Hola!");
   }

   public void testOneWayToSourceBinding()
   {
      final Nestable n1 = new Nestable();
      n1.setValue("Hello!");
      final Nestable n2 = new Nestable();
      n2.setValue("Bonjour!");

      Binding b = new Binding(new Property<String>(new Function<String>()
      {
         @Override
         public String evaluate()
         {
            // TODO Auto-generated method stub
            return n1.getValue();
         }
      }, new Action<String>()
      {

         @Override
         public void invoke(String parameter)
         {
            n1.setValue(parameter);
         }
      }, String.class), new Property<String>(new Function<String>()
      {

         @Override
         public String evaluate()
         {
            return n2.getValue();
         }
      }, new Action<String>()
      {

         @Override
         public void invoke(String parameter)
         {
            n2.setValue(parameter);
         }
      }, String.class), BindingMode.OneWayToSource);

      b.bind(n1);
      Assert.assertEquals(n1.getValue(), "Hello!");
      Assert.assertEquals(n2.getValue(), "Hello!");
      n1.setValue("Shalom!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getValue(), "Shalom!");
      n2.setValue("Hola!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getValue(), "Hola!");
   }

   public void testOneWayToSourceBindingReflected()
   {
      final Nestable n1 = new Nestable();
      n1.setValue("Hello!");
      final Nestable n2 = new Nestable();
      n2.setValue("Bonjour!");

      Binding b = new Binding(new ReflectedProperty(n1, "Value"),
            new ReflectedProperty(n2, "Value"), BindingMode.OneWayToSource);

      b.bind(n1);
      Assert.assertEquals(n1.getValue(), "Hello!");
      Assert.assertEquals(n2.getValue(), "Hello!");
      n1.setValue("Shalom!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getValue(), "Shalom!");
      n2.setValue("Hola!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getValue(), "Hola!");
   }

   public void testTwoWayBinding()
   {
      final Nestable n1 = new Nestable();
      n1.setValue("Hello!");
      final Nestable n2 = new Nestable();
      n2.setValue("Bonjour!");

      Binding b = new Binding(new Property<String>(new Function<String>()
      {
         @Override
         public String evaluate()
         {
            // TODO Auto-generated method stub
            return n1.getValue();
         }
      }, new Action<String>()
      {

         @Override
         public void invoke(String parameter)
         {
            n1.setValue(parameter);
         }
      }, String.class), new Property<String>(new Function<String>()
      {

         @Override
         public String evaluate()
         {
            return n2.getValue();
         }
      }, new Action<String>()
      {

         @Override
         public void invoke(String parameter)
         {
            n2.setValue(parameter);
         }
      }, String.class), BindingMode.TwoWay);

      b.bind(n1);
      Assert.assertEquals(n1.getValue(), "Bonjour!");
      Assert.assertEquals(n2.getValue(), "Bonjour!");
      n1.setValue("Shalom!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getValue(), "Shalom!");
      n2.setValue("Hola!");
      Assert.assertEquals(n1.getValue(), "Hola!");
      Assert.assertEquals(n2.getValue(), "Hola!");
   }

   public void testTwoWayBindingReflected()
   {
      final Nestable n1 = new Nestable();
      n1.setValue("Hello!");
      final Nestable n2 = new Nestable();
      n2.setValue("Bonjour!");

      Binding b = new Binding(new ReflectedProperty(n1, "Value"),
            new ReflectedProperty(n2, "Value"), BindingMode.TwoWay);

      b.bind(n1);
      Assert.assertEquals(n1.getValue(), "Bonjour!");
      Assert.assertEquals(n2.getValue(), "Bonjour!");
      n1.setValue("Shalom!");
      Assert.assertEquals(n1.getValue(), "Shalom!");
      Assert.assertEquals(n2.getValue(), "Shalom!");
      n2.setValue("Hola!");
      Assert.assertEquals(n1.getValue(), "Hola!");
      Assert.assertEquals(n2.getValue(), "Hola!");
   }
}
