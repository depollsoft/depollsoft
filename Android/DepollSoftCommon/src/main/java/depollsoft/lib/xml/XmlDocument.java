package depollsoft.lib.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;


public class XmlDocument {
  private static class XmlHandler extends DefaultHandler {
    private Stack<XmlElement> elementStack;
    private List<XmlElement> elements;

    public XmlHandler() {
      this.elementStack = new Stack<XmlElement>();
      this.elements = new ArrayList<XmlElement>();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      super.characters(ch, start, length);
      String newValue = (this.elementStack.peek().getValue() != null ? this.elementStack.peek()
          .getValue() : "") + new String(ch, start, length);
      if (newValue.length() == 0 && this.elementStack.peek().getValue() == null)
        return;
      this.elementStack.peek().setValue(newValue);
    }

    @Override
    public void endDocument() throws SAXException {
      super.endDocument();
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      super.endElement(uri, localName, qName);
      XmlElement cur = this.elementStack.pop();
      if (this.elementStack.isEmpty())
        this.elements.add(cur);
      else
        this.elementStack.peek().getElements().add(cur);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
      super.endPrefixMapping(prefix);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      super.error(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      super.fatalError(e);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
      super.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void notationDecl(String name, String publicId, String systemId) throws SAXException {
      super.notationDecl(name, publicId, systemId);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
      super.processingInstruction(target, data);
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws IOException,
        SAXException {
      return super.resolveEntity(publicId, systemId);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
      super.setDocumentLocator(locator);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
      super.skippedEntity(name);
    }

    @Override
    public void startDocument() throws SAXException {
      super.startDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      super.startElement(uri, localName, qName, attributes);
      XmlElement elem = new XmlElement();
      elem.setName(localName);
      for (int x = 0; x < attributes.getLength(); x++) {
        XmlAttribute attr = new XmlAttribute();
        attr.setName(attributes.getLocalName(x));
        attr.setValue(attributes.getValue(x));
        elem.getAttributes().add(attr);
      }
      this.elementStack.push(elem);
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
      super.startPrefixMapping(prefix, uri);
    }

    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId,
        String notationName) throws SAXException {
      super.unparsedEntityDecl(name, publicId, systemId, notationName);
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
      super.warning(e);
    }
  }

  public static XmlDocument parse(InputStream input) {
    try {
      byte[] bytes = readAllBytes(input);
      return parse(bytes);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static XmlDocument parse(String input) {
    try {
      return XmlDocument.parse(input.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private static XmlDocument parse(byte[] data) {
    XmlHandler handler = new XmlHandler();
    try {
      Xml.parse(new InputStreamReader(new ByteArrayInputStream(data)), handler);
      return fromElements(handler.elements);
    } catch (LinkageError e) {
      // The SAX parser may fail to load in certain environments (like unit tests)
      // where native/Expat classes are not available. Fallback to XmlPullParser.
      return parseWithPullParser(data);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static XmlDocument parseWithPullParser(byte[] data) {
    XmlPullParser parser = Xml.newPullParser();
    try {
      parser.setInput(new InputStreamReader(new ByteArrayInputStream(data)));
      Stack<XmlElement> elementStack = new Stack<XmlElement>();
      ArrayList<XmlElement> roots = new ArrayList<XmlElement>();
      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        switch (eventType) {
          case XmlPullParser.START_TAG:
            XmlElement elem = new XmlElement();
            elem.setName(parser.getName());
            for (int i = 0; i < parser.getAttributeCount(); i++) {
              XmlAttribute attr = new XmlAttribute();
              attr.setName(parser.getAttributeName(i));
              attr.setValue(parser.getAttributeValue(i));
              elem.getAttributes().add(attr);
            }
            elementStack.push(elem);
            break;
          case XmlPullParser.TEXT:
            if (!elementStack.isEmpty()) {
              XmlElement current = elementStack.peek();
              String existing = current.getValue();
              current.setValue((existing == null ? "" : existing) + parser.getText());
            }
            break;
          case XmlPullParser.END_TAG:
            if (!elementStack.isEmpty()) {
              XmlElement finished = elementStack.pop();
              if (elementStack.isEmpty())
                roots.add(finished);
              else
                elementStack.peek().getElements().add(finished);
            }
            break;
          default:
            break;
        }
        eventType = parser.next();
      }
      return fromElements(roots);
    } catch (XmlPullParserException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static XmlDocument fromElements(List<XmlElement> elements) {
    XmlDocument result = new XmlDocument();
    result.setElements(elements);
    return result;
  }

  private static byte[] readAllBytes(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[1024];
    int read;
    while ((read = input.read(chunk)) != -1) {
      buffer.write(chunk, 0, read);
    }
    return buffer.toByteArray();
  }

  private List<XmlElement> elements;

  private XmlDocument() {
  }

  public List<XmlElement> elements(String name) {
    ArrayList<XmlElement> results = new ArrayList<XmlElement>();
    for (XmlElement elem : this.getElements())
      if (elem.getName().equals(name))
        results.add(elem);
    return results;
  }

  public List<XmlElement> getElements() {
    return this.elements;
  }

  private void setElements(List<XmlElement> value) {
    this.elements = value;
  }
}
