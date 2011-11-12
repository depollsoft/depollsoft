package depollsoft.tagmaster.barbershop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

import android.net.Uri;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.json.JsonSerializer;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.Task;
import depollsoft.lib.xml.XmlDocument;
import depollsoft.lib.xml.XmlElement;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

public class Tag
{
   public static final int CURRENT_APP_VERSION = 2;
   private static final Map<Integer, SoftReference<Tag>> TagCache = new HashMap<Integer, SoftReference<Tag>>();
   private static final Object CacheWriteLock = new Object();
   private static final String API_URI_STRING = "http://www.barbershoptags.com/api.php?client=TagMaster&";
   private static final String RATING_URI_STRING = "http://www.barbershoptags.com/api.php?client=TagMaster&action=rate&id=%d&rating=%d";

   public static void clearCache()
   {
      File directory = new File(RichApplication.getAppContext().getFilesDir(),
            "TagCache");
      for (File f : directory.listFiles())
         f.delete();
      Tag.TagCache.clear();
   }

   public static long getCurrentCacheSize()
   {
      File directory = new File(RichApplication.getAppContext().getFilesDir(),
            "TagCache");
      long total = 0;
      for (File f : directory.listFiles())
         total += f.length();
      return total;
   }

   public static Task<Tag> loadTagById(int id)
   {
      return Tag.loadTagById(id, false);
   }

   public static Task<Tag> loadTagById(final int id, boolean refresh)
   {
      final Task.TaskSource<Tag> taskSource = new Task.TaskSource<Tag>();
      File directory = new File(RichApplication.getAppContext().getFilesDir(),
            "TagCache");
      File file = new File(directory, "" + id);
      boolean loadedFromCache = false;
      if (!refresh && Tag.TagCache.containsKey(id))
      {
         Tag cachedTag = Tag.TagCache.get(id).get();
         if (cachedTag != null)
         {
            taskSource.setResult(cachedTag);
            loadedFromCache = true;
         }
      }
      if (!loadedFromCache && !refresh && file.exists())
      {
         try
         {
            InputStream is = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            is.read(buffer);
            is.close();
            String cachedTag = new String(buffer);
            buffer = null;
            JSONObject jsonObj = new JSONObject(cachedTag);
            Tag tag = (Tag) JsonSerializer.deserialize(jsonObj);
            if (tag == null || tag.getAppVersion() != Tag.CURRENT_APP_VERSION)
               throw new Exception();
            Tag.TagCache.put(id, new SoftReference<Tag>(tag));
            taskSource.setResult(tag);
         }
         catch (Exception e)
         {
            Tag.loadTagById(id, true).continueWith(new Action<Tag>()
            {

               public void invoke(Tag parameter)
               {
                  taskSource.setResult(parameter);
               }
            }, new Action<Exception>()
            {

               public void invoke(Exception parameter)
               {
                  taskSource.setError(parameter);
               }
            });
         }
      }
      else
      {
         Tag.queryById(id).continueWith(new Action<Tag>()
         {

            public void invoke(Tag parameter)
            {
               parameter.cache();
               Tag.TagCache.put(id, new SoftReference<Tag>(parameter));
               taskSource.setResult(parameter);
            }
         }, new Action<Exception>()
         {

            public void invoke(Exception parameter)
            {
               taskSource.setError(parameter);
            }
         });
      }
      return new Task<Tag>(taskSource);
   }

   public static Task<TagQueryResult> query(String query)
   {
      return Tag.query(query, 10);
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults)
   {
      return Tag.query(query, numberOfResults, 0);
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults,
         int start)
   {
      return Tag.query(query, numberOfResults, start, null);
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults,
         int start, Integer parts)
   {
      return Tag.query(query, numberOfResults, start, parts, null);
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults,
         int start, Integer parts, Boolean learning)
   {
      return Tag.query(query, numberOfResults, start, parts, learning, null);
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults,
         int start, Integer parts, Boolean learning, Boolean sheetMusic)
   {
      return Tag.query(query, numberOfResults, start, parts, learning,
            sheetMusic, null);
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults,
         int start, Integer parts, Boolean learning, Boolean sheetMusic,
         TagCollection collection)
   {
      return Tag.query(query, numberOfResults, start, parts, learning,
            sheetMusic, collection, null);
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults,
         int start, Integer parts, Boolean learning, Boolean sheetMusic,
         TagCollection collection, TagSortOptions sortBy)
   {
      return Tag.query(query, numberOfResults, start, parts, learning,
            sheetMusic, collection, sortBy, null);
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults,
         int start, Integer parts, Boolean learning, Boolean sheetMusic,
         TagCollection collection, TagSortOptions sortBy, Double minimumRating)
   {
      return Tag.query(query, numberOfResults, start, parts, learning,
            sheetMusic, collection, sortBy, minimumRating, null);
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults,
         final int start, Integer parts, Boolean learning, Boolean sheetMusic,
         TagCollection collection, TagSortOptions sortBy, Double minimumRating,
         Integer minimumDownloads)
   {
      return Tag.query(query, numberOfResults, start, parts, learning,
            sheetMusic, collection, sortBy, minimumRating, minimumDownloads,
            false);
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults,
         final int start, Integer parts, Boolean learning, Boolean sheetMusic,
         TagCollection collection, TagSortOptions sortBy, Double minimumRating,
         Integer minimumDownloads, final boolean cache)
   {
      return Tag
            .query(
                  query,
                  numberOfResults,
                  start,
                  parts,
                  learning,
                  sheetMusic,
                  collection,
                  sortBy,
                  minimumRating,
                  minimumDownloads,
                  cache,
                  "id,Title,AltTitle,Rating,Posted,Downloaded,SheetMusic,Bass,Bari,Lead,Tenor,Other1,Other2,Other3,Other4");
   }

   public static Task<TagQueryResult> query(String query, int numberOfResults,
         final int start, Integer parts, Boolean learning, Boolean sheetMusic,
         TagCollection collection, TagSortOptions sortBy, Double minimumRating,
         Integer minimumDownloads, final boolean cache, String fieldList)
   {
      StringBuffer sb = new StringBuffer();
      sb.append("n=" + numberOfResults);
      if (fieldList != null)
         sb.append("&fldlist=" + fieldList);
      sb.append("&start=" + (start + 1));
      if (!(query == null || query.trim().equals("")))
         sb.append("&q=" + Uri.encode(query));
      if (parts != null)
         sb.append("&Parts=" + parts);
      if (learning != null)
         sb.append("&Learning=" + (learning.booleanValue() ? "Yes" : "No"));
      if (sheetMusic != null)
         sb.append("&SheetMusic=" + (sheetMusic.booleanValue() ? "Yes" : "No"));
      if (sortBy != null)
      {
         sb.append("&Sortby=");
         switch (sortBy)
         {
         case Classic:
            sb.append("Classic");
            collection = TagCollection.ClassicTags;
            break;
         case Downloaded:
            sb.append("Downloaded");
            break;
         case Posted:
            sb.append("Posted");
            break;
         case Rating:
            sb.append("Rating");
            break;
         case Title:
            sb.append("Title");
            break;
         }
      }
      if (collection != null)
      {
         sb.append("&Collection=");
         switch (collection)
         {
         case ClassicTags:
            sb.append("classic");
            break;
         case EasyTags:
            sb.append("easy");
            break;
         }
      }
      if (minimumRating != null)
         sb.append("&MinRating=" + minimumRating);
      if (minimumDownloads != null)
         sb.append("&MinDownloaded=" + minimumDownloads);
      final Task.TaskSource<TagQueryResult> source = new Task.TaskSource<TagQueryResult>();
      try
      {
         final URL url = new URL(Tag.API_URI_STRING + sb.toString());
         Thread t = new Thread()
         {
            @Override
            public void run()
            {
               try
               {
                  TagQueryResult result = new TagQueryResult();
                  InputStream is = url.openStream();
                  XmlDocument doc = XmlDocument.parse(is);
                  XmlElement tags = doc.elements("tags").get(0);
                  result.setAvailable(Integer.parseInt(tags.attribute(
                        "available").getValue()));
                  result.setCount(Integer.parseInt(tags.attribute("count")
                        .getValue()));
                  result.setStart(start);
                  ArrayList<Tag> resultTags = new ArrayList<Tag>();
                  for (XmlElement tagXml : tags.getElements())
                  {
                     Tag t = new Tag();
                     t.parseFromXml(tagXml);
                     resultTags.add(t);
                  }
                  result.setTags(resultTags);
                  if (cache)
                     for (Tag t : resultTags)
                        t.cache();
                  source.setResult(result);
               }
               catch (Exception e)
               {
                  source.setError(e);
               }
            }
         };
         t.start();
      }
      catch (MalformedURLException e)
      {
         e.printStackTrace();
      }
      return new Task<TagQueryResult>(source);
   }

   public static Task<Tag> queryById(int id)
   {
      final Task.TaskSource<Tag> source = new Task.TaskSource<Tag>();
      try
      {
         final URL url = new URL(Tag.API_URI_STRING + "id=" + id);
         Thread t = new Thread()
         {
            @Override
            public void run()
            {
               try
               {
                  InputStream is = url.openStream();
                  XmlDocument doc = XmlDocument.parse(is);
                  XmlElement tags = doc.elements("tags").get(0);
                  Tag t = new Tag();
                  t.parseFromXml(tags.elements("tag").get(0));
                  source.setResult(t);
               }
               catch (Exception e)
               {
                  source.setError(e);
               }
            }
         };
         t.start();
      }
      catch (MalformedURLException e)
      {
         e.printStackTrace();
      }
      return new Task<Tag>(source);
   }

   private TrackableField<Integer> appVersion = new TrackableField<Integer>(0);

   private TrackableField<Integer> id = new TrackableField<Integer>(0);

   private TrackableField<String> title = new TrackableField<String>();

   private TrackableField<Date> lastRefreshed = new TrackableField<Date>();

   private TrackableField<String> alternativeTitle = new TrackableField<String>();

   private TrackableField<String> version = new TrackableField<String>();

   private TrackableField<String> writtenKey = new TrackableField<String>();

   private TrackableField<Integer> parts = new TrackableField<Integer>();

   private TrackableField<String> tagType = new TrackableField<String>();

   private TrackableField<String> recordingMethod = new TrackableField<String>();

   private TrackableField<String> teachingVideo = new TrackableField<String>();

   private TrackableField<String> notes = new TrackableField<String>();

   private TrackableField<String> arranger = new TrackableField<String>();

   private TrackableField<String> arrangerWebsite = new TrackableField<String>();

   private TrackableField<Integer> yearArranged = new TrackableField<Integer>();

   private TrackableField<String> sungBy = new TrackableField<String>();

   private TrackableField<String> sungByWebsite = new TrackableField<String>();

   private TrackableField<Integer> sungYear = new TrackableField<Integer>();

   private TrackableField<String> learningTrackQuartet = new TrackableField<String>();

   private TrackableField<String> learningTrackQuartetWebsite = new TrackableField<String>();

   private TrackableField<String> teacher = new TrackableField<String>();

   private TrackableField<String> teacherWebsite = new TrackableField<String>();

   private TrackableField<String> provider = new TrackableField<String>();

   private TrackableField<String> providerWebsite = new TrackableField<String>();

   private TrackableField<Date> posted = new TrackableField<Date>();

   private TrackableField<Integer> classicTagNumber = new TrackableField<Integer>();

   private TrackableField<Double> rating = new TrackableField<Double>();

   private TrackableField<Integer> downloadCount = new TrackableField<Integer>(
         0);

   private TrackableField<RemoteLocation> sheetMusicUri = new TrackableField<RemoteLocation>();

   private TrackableField<RemoteLocation> notationUri = new TrackableField<RemoteLocation>();

   private TrackableField<RemoteLocation> allPartsTrackUri = new TrackableField<RemoteLocation>();

   private TrackableField<RemoteLocation> bassTrackUri = new TrackableField<RemoteLocation>();

   private TrackableField<RemoteLocation> baritoneTrackUri = new TrackableField<RemoteLocation>();

   private TrackableField<RemoteLocation> leadTrackUri = new TrackableField<RemoteLocation>();

   private TrackableField<RemoteLocation> tenorTrackUri = new TrackableField<RemoteLocation>();

   private TrackableField<RemoteLocation> other1TrackUri = new TrackableField<RemoteLocation>();

   private TrackableField<RemoteLocation> other2TrackUri = new TrackableField<RemoteLocation>();

   private TrackableField<RemoteLocation> other3TrackUri = new TrackableField<RemoteLocation>();

   private TrackableField<RemoteLocation> other4TrackUri = new TrackableField<RemoteLocation>();

   private TrackableField<List<Video>> videos = new TrackableField<List<Video>>();

   private TrackableField<List<Track>> tracks = new TrackableField<List<Track>>();

   private TrackableField<String> lyrics = new TrackableField<String>();

   public Tag()
   {
      this.setAppVersion(Tag.CURRENT_APP_VERSION);
      this.setVideos(new ArrayList<Video>());
   }

   public void cache()
   {
      this.cache(true);
   }

   public void cache(final boolean overwrite)
   {
      if (overwrite)
         Tag.TagCache.put(this.getId(), new SoftReference<Tag>(this));
      Thread t = new Thread()
      {
         @Override
         public void run()
         {
            synchronized (Tag.CacheWriteLock)
            {
               try
               {
                  File directory = new File(RichApplication.getAppContext()
                        .getFilesDir(), "TagCache");
                  directory.mkdir();
                  File cacheFile = new File(directory, "" + Tag.this.getId());
                  if (cacheFile.exists())
                  {
                     if (!overwrite)
                        return;
                     cacheFile.delete();
                  }
                  FileOutputStream fos = new FileOutputStream(cacheFile);
                  JSONObject serialized = JsonSerializer.serialize(Tag.this);
                  PrintWriter pw = new PrintWriter(fos);
                  pw.println(serialized);
                  pw.close();
               }
               catch (FileNotFoundException e)
               {
                  e.printStackTrace();
               }
            }
         }
      };
      t.start();
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
         return false;
      return ((Tag) obj).getId() == this.getId();
   }

   public RemoteLocation getAllPartsTrackUri()
   {
      return this.allPartsTrackUri.getValue();
   }

   public String getAlternativeTitle()
   {
      return this.alternativeTitle.getValue();
   }

   public int getAppVersion()
   {
      return this.appVersion.getValue();
   }

   public String getArranger()
   {
      return this.arranger.getValue();
   }

   public String getArrangerWebsite()
   {
      return this.arrangerWebsite.getValue();
   }

   public RemoteLocation getBaritoneTrackUri()
   {
      return this.baritoneTrackUri.getValue();
   }

   public RemoteLocation getBassTrackUri()
   {
      return this.bassTrackUri.getValue();
   }

   public Integer getClassicTagNumber()
   {
      return this.classicTagNumber.getValue();
   }

   public int getDownloadCount()
   {
      return this.downloadCount.getValue();
   }

   public int getId()
   {
      return this.id.getValue();
   }

   public Note getKeyNote()
   {
      if (this.getWrittenKey() == null)
         return null;
      String noteName = this.getWrittenKey().toUpperCase(Locale.ENGLISH)
            .replace("MAJOR", "").replace("MINOR", "").replace(":", "").trim();
      Accidental acc = Accidental.Natural;
      if (noteName.length() > 1)
         acc = noteName.charAt(1) == '#' ? Accidental.Sharp : Accidental.Flat;
      return Note.findNote("" + noteName.charAt(0), acc, 4);
   }

   public Date getLastRefreshed()
   {
      return this.lastRefreshed.getValue();
   }

   public RemoteLocation getLeadTrackUri()
   {
      return this.leadTrackUri.getValue();
   }

   public String getLearningTrackQuartet()
   {
      return this.learningTrackQuartet.getValue();
   }

   public String getLearningTrackQuartetWebsite()
   {
      return this.learningTrackQuartetWebsite.getValue();
   }

   public String getLyrics()
   {
      return this.lyrics.getValue();
   }

   public RemoteLocation getNotationUri()
   {
      return this.notationUri.getValue();
   }

   public String getNotes()
   {
      return this.notes.getValue();
   }

   public RemoteLocation getOther1TrackUri()
   {
      return this.other1TrackUri.getValue();
   }

   public RemoteLocation getOther2TrackUri()
   {
      return this.other2TrackUri.getValue();
   }

   public RemoteLocation getOther3TrackUri()
   {
      return this.other3TrackUri.getValue();
   }

   public RemoteLocation getOther4TrackUri()
   {
      return this.other4TrackUri.getValue();
   }

   public Integer getParts()
   {
      return this.parts.getValue();
   }

   public Date getPosted()
   {
      return this.posted.getValue();
   }

   public String getProvider()
   {
      return this.provider.getValue();
   }

   public String getProviderWebsite()
   {
      return this.providerWebsite.getValue();
   }

   public Double getRating()
   {
      return this.rating.getValue();
   }

   public String getRecordingMethod()
   {
      return this.recordingMethod.getValue();
   }

   public boolean getSheetMusicSupportedFormat()
   {
      // TODO: Implement
      return true;
   }

   public RemoteLocation getSheetMusicUri()
   {
      return this.sheetMusicUri.getValue();
   }

   public String getSungBy()
   {
      return this.sungBy.getValue();
   }

   public String getSungByWebsite()
   {
      return this.sungByWebsite.getValue();
   }

   public Integer getSungYear()
   {
      return this.sungYear.getValue();
   }

   public String getTagType()
   {
      return this.tagType.getValue();
   }

   public String getTagUri()
   {
      return String
            .format(
                  "http://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=%s",
                  this.getId());
   }

   public String getTeacher()
   {
      return this.teacher.getValue();
   }

   public String getTeacherWebsite()
   {
      return this.teacherWebsite.getValue();
   }

   public String getTeachingVideo()
   {
      return this.teachingVideo.getValue();
   }

   public RemoteLocation getTenorTrackUri()
   {
      return this.tenorTrackUri.getValue();
   }

   public String getTitle()
   {
      return this.title.getValue();
   }

   public List<Track> getTracks()
   {
      if (this.tracks.getValue() == null)
      {
         ObservableCollection<Track> tracks = new ObservableCollection<Track>();
         if (this.getAllPartsTrackUri() != null)
            tracks.add(new Track("All Parts", this.getAllPartsTrackUri()));
         if (this.getTenorTrackUri() != null)
            tracks.add(new Track("Tenor", this.getTenorTrackUri()));
         if (this.getLeadTrackUri() != null)
            tracks.add(new Track("Lead", this.getLeadTrackUri()));
         if (this.getBaritoneTrackUri() != null)
            tracks.add(new Track("Baritone", this.getBaritoneTrackUri()));
         if (this.getBassTrackUri() != null)
            tracks.add(new Track("Bass", this.getBassTrackUri()));
         if (this.getOther1TrackUri() != null)
            tracks.add(new Track("Other1", this.getOther1TrackUri()));
         if (this.getOther2TrackUri() != null)
            tracks.add(new Track("Other2", this.getOther2TrackUri()));
         if (this.getOther3TrackUri() != null)
            tracks.add(new Track("Other3", this.getOther3TrackUri()));
         if (this.getOther4TrackUri() != null)
            tracks.add(new Track("Other4", this.getOther4TrackUri()));
         this.tracks.setValue(tracks);
      }
      return this.tracks.getValue();
   }

   public String getVersion()
   {
      return this.version.getValue();
   }

   public List<Video> getVideos()
   {
      return this.videos.getValue();
   }

   public String getWrittenKey()
   {
      return this.writtenKey.getValue();
   }

   public Integer getYearArranged()
   {
      return this.yearArranged.getValue();
   }

   @Override
   public int hashCode()
   {
      return new Integer(this.getId()).hashCode();
   }

   protected void parseFromXml(XmlElement element)
   {
      this.setLastRefreshed(new Date());
      for (XmlElement property : element.getElements())
      {
         try
         {
            String propValue = property.getValue();
            if (propValue == null || propValue.trim().length() == 0)
               propValue = null;
            if (propValue != null)
               propValue = propValue.trim();
            if (property.getName().equals("id") && propValue != null)
               this.setId(Integer.parseInt(propValue));
            else if (property.getName().equals("Title"))
               this.setTitle(propValue);
            else if (property.getName().equals("AltTitle"))
               this.setAlternativeTitle(propValue);
            else if (property.getName().equals("Version"))
               this.setVersion(propValue);
            else if (property.getName().equals("WritKey"))
               this.setWrittenKey(propValue);
            else if (property.getName().equals("Parts") && propValue != null)
               this.setParts(Integer.parseInt(propValue));
            else if (property.getName().equals("Type"))
               this.setTagType(propValue);
            else if (property.getName().equals("Recording"))
               this.setRecordingMethod(propValue);
            else if (property.getName().equals("TeachVid"))
               this.setTeachingVideo(propValue);
            else if (property.getName().equals("Lyrics"))
               this.setLyrics(propValue);
            else if (property.getName().equals("Notes"))
               this.setNotes(propValue);
            else if (property.getName().equals("Arranger"))
               this.setArranger(propValue);
            else if (property.getName().equals("ArrWebsite"))
               this.setArrangerWebsite(propValue);
            else if (property.getName().equals("Arranged") && propValue != null)
               this.setYearArranged(Integer.parseInt(propValue));
            else if (property.getName().equals("SungBy"))
               this.setSungBy(propValue);
            else if (property.getName().equals("SungWebsite"))
               this.setSungByWebsite(propValue);
            else if (property.getName().equals("SungYear") && propValue != null)
               this.setSungYear(Integer.parseInt(propValue));
            else if (property.getName().equals("Quartet"))
               this.setLearningTrackQuartet(propValue);
            else if (property.getName().equals("QWebsite"))
               this.setLearningTrackQuartetWebsite(propValue);
            else if (property.getName().equals("Teacher"))
               this.setTeacher(propValue);
            else if (property.getName().equals("TWebsite"))
               this.setTeacherWebsite(propValue);
            else if (property.getName().equals("Provider"))
               this.setProvider(propValue);
            else if (property.getName().equals("ProvWebsite"))
               this.setProviderWebsite(propValue);
            else if (property.getName().equals("Posted"))
               this.setPosted(new Date(propValue));
            else if (property.getName().equals("Classic") && propValue != null)
               this.setClassicTagNumber(Integer.parseInt(propValue));
            else if (property.getName().equals("Rating") && propValue != null)
               this.setRating(Double.parseDouble(propValue));
            else if (property.getName().equals("Downloaded")
                  && propValue != null)
               this.setDownloadCount(Integer.parseInt(propValue
                     .replace(",", "")));
            else if (property.getName().equals("SheetMusic"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setSheetMusicUri(rl);
               }
            }
            else if (property.getName().equals("Notation"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setNotationUri(rl);
               }
            }
            else if (property.getName().equals("AllParts"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setAllPartsTrackUri(rl);
               }
            }
            else if (property.getName().equals("Bass"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setBassTrackUri(rl);
               }
            }
            else if (property.getName().equals("Bari"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setBaritoneTrackUri(rl);
               }
            }
            else if (property.getName().equals("Lead"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setLeadTrackUri(rl);
               }
            }
            else if (property.getName().equals("Tenor"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setTenorTrackUri(rl);
               }
            }
            else if (property.getName().equals("Other1"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setOther1TrackUri(rl);
               }
            }
            else if (property.getName().equals("Other2"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setOther2TrackUri(rl);
               }
            }
            else if (property.getName().equals("Other3"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setOther3TrackUri(rl);
               }
            }
            else if (property.getName().equals("Other4"))
            {
               if (!(propValue == null || propValue.length() == 0))
               {
                  RemoteLocation rl = new RemoteLocation();
                  rl.setUri(propValue);
                  rl.setType(property.attribute("type").getValue());
                  this.setOther4TrackUri(rl);
               }
            }
            else if (property.getName().equals("videos"))
            {
               for (XmlElement elem : property.getElements())
               {
                  if (!elem.getName().equals("video"))
                     continue;
                  Video v = new Video();
                  v.parseFromXml(elem);
                  this.getVideos().add(v);
               }
            }
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }

   public Task<Boolean> rate(int rating)
   {
      final Task.TaskSource<Boolean> source = new Task.TaskSource<Boolean>();
      try
      {
         final URL url = new URL(String.format(Tag.RATING_URI_STRING,
               this.getId(), rating));
         Thread t = new Thread()
         {
            @Override
            public void run()
            {
               try
               {
                  InputStream is = url.openStream();
                  BufferedReader br = new BufferedReader(new InputStreamReader(
                        is));
                  String value = br.readLine();
                  boolean result = value.trim().toLowerCase().equals("ok");
                  source.setResult(result);
               }
               catch (Exception e)
               {
                  source.setError(e);
               }
            }
         };
         t.start();
      }
      catch (MalformedURLException e)
      {
         e.printStackTrace();
      }
      return new Task<Boolean>(source);
   }

   public void setAllPartsTrackUri(RemoteLocation value)
   {
      this.allPartsTrackUri.setValue(value);
   }

   public void setAlternativeTitle(String value)
   {
      this.alternativeTitle.setValue(value);
   }

   public void setAppVersion(int value)
   {
      this.appVersion.setValue(value);
   }

   public void setArranger(String value)
   {
      this.arranger.setValue(value);
   }

   public void setArrangerWebsite(String value)
   {
      this.arrangerWebsite.setValue(value);
   }

   public void setBaritoneTrackUri(RemoteLocation value)
   {
      this.baritoneTrackUri.setValue(value);
   }

   public void setBassTrackUri(RemoteLocation value)
   {
      this.bassTrackUri.setValue(value);
   }

   public void setClassicTagNumber(Integer value)
   {
      this.classicTagNumber.setValue(value);
   }

   public void setDownloadCount(int value)
   {
      this.downloadCount.setValue(value);
   }

   public void setId(int value)
   {
      this.id.setValue(value);
   }

   public void setLastRefreshed(Date value)
   {
      this.lastRefreshed.setValue(value);
   }

   public void setLeadTrackUri(RemoteLocation value)
   {
      this.leadTrackUri.setValue(value);
   }

   public void setLearningTrackQuartet(String value)
   {
      this.learningTrackQuartet.setValue(value);
   }

   public void setLearningTrackQuartetWebsite(String value)
   {
      this.learningTrackQuartetWebsite.setValue(value);
   }

   public void setLyrics(String value)
   {
      this.lyrics.setValue(value);
   }

   public void setNotationUri(RemoteLocation value)
   {
      this.notationUri.setValue(value);
   }

   public void setNotes(String value)
   {
      this.notes.setValue(value);
   }

   public void setOther1TrackUri(RemoteLocation value)
   {
      this.other1TrackUri.setValue(value);
   }

   public void setOther2TrackUri(RemoteLocation value)
   {
      this.other2TrackUri.setValue(value);
   }

   public void setOther3TrackUri(RemoteLocation value)
   {
      this.other3TrackUri.setValue(value);
   }

   public void setOther4TrackUri(RemoteLocation value)
   {
      this.other4TrackUri.setValue(value);
   }

   public void setParts(Integer value)
   {
      this.parts.setValue(value);
   }

   public void setPosted(Date value)
   {
      this.posted.setValue(value);
   }

   public void setProvider(String value)
   {
      this.provider.setValue(value);
   }

   public void setProviderWebsite(String value)
   {
      this.providerWebsite.setValue(value);
   }

   public void setRating(Double value)
   {
      this.rating.setValue(value);
   }

   public void setRecordingMethod(String value)
   {
      this.recordingMethod.setValue(value);
   }

   public void setSheetMusicUri(RemoteLocation value)
   {
      this.sheetMusicUri.setValue(value);
   }

   public void setSungBy(String value)
   {
      this.sungBy.setValue(value);
   }

   public void setSungByWebsite(String value)
   {
      this.sungByWebsite.setValue(value);
   }

   public void setSungYear(Integer value)
   {
      this.sungYear.setValue(value);
   }

   public void setTagType(String value)
   {
      this.tagType.setValue(value);
   }

   public void setTeacher(String value)
   {
      this.teacher.setValue(value);
   }

   public void setTeacherWebsite(String value)
   {
      this.teacherWebsite.setValue(value);
   }

   public void setTeachingVideo(String value)
   {
      this.teachingVideo.setValue(value);
   }

   public void setTenorTrackUri(RemoteLocation value)
   {
      this.tenorTrackUri.setValue(value);
   }

   public void setTitle(String value)
   {
      this.title.setValue(value);
   }

   public void setVersion(String value)
   {
      this.version.setValue(value);
   }

   public void setVideos(List<Video> value)
   {
      this.videos.setValue(value);
   }

   public void setWrittenKey(String value)
   {
      this.writtenKey.setValue(value);
   }

   public void setYearArranged(Integer value)
   {
      this.yearArranged.setValue(value);
   }

   @Override
   public String toString()
   {
      return String.format("id=%s, title=%s", this.getId(), this.getTitle());
   }
}
