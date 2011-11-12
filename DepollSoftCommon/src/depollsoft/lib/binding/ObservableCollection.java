package depollsoft.lib.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

public class ObservableCollection<T> extends Trackable implements List<T>
{
   private List<T> backingStore;
   private List<Long> ids;
   private long curId;
   private Stack<Long> returnedIds;
   private Trackable trackable = this;

   public ObservableCollection()
   {
      this(new ArrayList<T>());
   }

   public ObservableCollection(List<T> backingStore)
   {
      this.backingStore = backingStore;
      this.ids = new ArrayList<Long>();
      this.returnedIds = new Stack<Long>();
      for (int x = 0; x < backingStore.size(); x++)
         this.ids.add(this.getNewId());
   }

   public void add(int location, T object)
   {
      this.backingStore.add(location, object);
      this.ids.add(location, this.getNewId());
      this.trackable.updateTrackers();
   }

   public boolean add(T object)
   {
      boolean result = this.backingStore.add(object);
      this.ids.add(this.getNewId());
      if (result)
         this.trackable.updateTrackers();
      return result;
   }

   public boolean addAll(Collection<? extends T> arg0)
   {
      boolean result = this.backingStore.addAll(arg0);
      for (@SuppressWarnings("unused")
      Object item : arg0)
         this.ids.add(this.getNewId());
      if (result)
         this.trackable.updateTrackers();
      return result;
   }

   public boolean addAll(int arg0, Collection<? extends T> arg1)
   {
      boolean result = this.backingStore.addAll(arg0, arg1);
      for (@SuppressWarnings("unused")
      Object item : arg1)
         this.ids.add(this.getNewId());
      if (result)
         this.trackable.updateTrackers();
      return result;
   }

   public void clear()
   {
      this.backingStore.clear();
      this.ids.clear();
      this.returnedIds.clear();
      this.curId = 0;
      this.trackable.updateTrackers();
   }

   public boolean contains(Object object)
   {
      this.trackable.track();
      return this.backingStore.contains(object);
   }

   public boolean containsAll(Collection<?> arg0)
   {
      this.trackable.track();
      return this.backingStore.containsAll(arg0);
   }

   public T get(int location)
   {
      this.trackable.track();
      return this.backingStore.get(location);
   }

   public long getId(int index)
   {
      return this.ids.get(index);
   }

   private long getNewId()
   {
      if (this.returnedIds.isEmpty())
         return this.curId++;
      return this.returnedIds.pop();
   }

   public int indexOf(Object object)
   {
      this.trackable.track();
      return this.backingStore.indexOf(object);
   }

   public boolean isEmpty()
   {
      this.trackable.track();
      return this.backingStore.isEmpty();
   }

   public Iterator<T> iterator()
   {
      this.trackable.track();
      return this.backingStore.iterator();
   }

   public int lastIndexOf(Object object)
   {
      this.trackable.track();
      return this.backingStore.lastIndexOf(object);
   }

   public ListIterator<T> listIterator()
   {
      this.trackable.track();
      return this.backingStore.listIterator();
   }

   public ListIterator<T> listIterator(int location)
   {
      this.trackable.track();
      return this.backingStore.listIterator(location);
   }

   public T remove(int location)
   {
      T result = this.backingStore.remove(location);
      this.returnId(this.ids.remove(location));
      this.trackable.updateTrackers();
      return result;
   }

   public boolean remove(Object object)
   {
      int index = this.backingStore.indexOf(object);
      boolean result = index >= 0;
      if (result)
         this.remove(index);
      return result;
   }

   public boolean removeAll(Collection<?> arg0)
   {
      HashSet<?> items = new HashSet<Object>(arg0);
      ArrayList<Long> toRemove = new ArrayList<Long>();
      for (int x = 0; x < this.backingStore.size(); x++)
         if (items.contains(this.backingStore.get(x)))
         {
            toRemove.add(this.ids.get(x));
            this.returnId(this.ids.get(x));
         }
      boolean result = this.backingStore.removeAll(arg0);
      this.ids.removeAll(toRemove);
      if (result)
         this.trackable.updateTrackers();
      return result;
   }

   public boolean retainAll(Collection<?> arg0)
   {
      HashSet<?> items = new HashSet<Object>(arg0);
      ArrayList<Long> toRemove = new ArrayList<Long>();
      for (int x = 0; x < this.backingStore.size(); x++)
         if (!items.contains(this.backingStore.get(x)))
         {
            toRemove.add(this.ids.get(x));
            this.returnId(this.ids.get(x));
         }
      boolean result = this.backingStore.retainAll(arg0);
      this.ids.removeAll(toRemove);
      if (result)
         this.trackable.updateTrackers();
      return result;
   }

   private void returnId(long id)
   {
      // this.returnedIds.push(id);
   }

   public T set(int location, T object)
   {
      T result = this.backingStore.set(location, object);
      this.trackable.updateTrackers();
      return result;
   }

   public int size()
   {
      this.trackable.track();
      return this.backingStore.size();
   }

   public List<T> subList(int start, int end)
   {
      this.trackable.track();
      return new ObservableCollection<T>(this.backingStore.subList(start, end));
   }

   public Object[] toArray()
   {
      this.trackable.track();
      return this.backingStore.toArray();
   }

   public <T1> T1[] toArray(T1[] array)
   {
      this.trackable.track();
      return this.backingStore.<T1> toArray(array);
   }

}
