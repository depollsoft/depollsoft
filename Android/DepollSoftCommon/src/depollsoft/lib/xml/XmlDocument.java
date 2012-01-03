package depollsoft.lib.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Xml;

import depollsoft.lib.binding.TrackableField;

public class XmlDocument {
  private static class XmlHandler extends DefaultHandler {
    private Stack<XmlElement> elementStack;
    private List<XmlElement> elements;

    public XmlHandler() {
      this.elementStack = new Stack<XmlElement>();
      this.elements = new ArrayList<XmlElement>();
    }

    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {
      super.characters(ch, start, length);
      String newValue = (this.elementStack.peek().getValue() != null ? this.elementStack
          .peek().getValue() : "")
          + new String(ch, start, length);
      if (newValue.length() == 0 && this.elementStack.peek().getValue() == null)
        return;
      this.elementStack.peek().setValue(newValue);
    }

    @Override
    public void endDocument() throws SAXException {
      // TODO Auto-generated method stub
      super.endDocument();
    }

    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
      // TODO Auto-generated method stub
      super.endElement(uri, localName, qName);
      XmlElement cur = this.elementStack.pop();
      if (this.elementStack.isEmpty())
        this.elements.add(cur);
      else
        this.elementStack.peek().getElements().add(cur);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
      // TODO Auto-generated method stub
      super.endPrefixMapping(prefix);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      // TODO Auto-generated method stub
      super.error(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      // TODO Auto-generated method stub
      super.fatalError(e);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException {
      // TODO Auto-generated method stub
      super.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void notationDecl(String name, String publicId, String systemId)
        throws SAXException {
      // TODO Auto-generated method stub
      super.notationDecl(name, publicId, systemId);
    }

    @Override
    public void processingInstruction(String target, String data)
        throws SAXException {
      // TODO Auto-generated method stub
      super.processingInstruction(target, data);
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId)
        throws IOException, SAXException {
      // TODO Auto-generated method stub
      return super.resolveEntity(publicId, systemId);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
      // TODO Auto-generated method stub
      super.setDocumentLocator(locator);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
      // TODO Auto-generated method stub
      super.skippedEntity(name);
    }

    @Override
    public void startDocument() throws SAXException {
      // TODO Auto-generated method stub
      super.startDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException {
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
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
      // TODO Auto-generated method stub
      super.startPrefixMapping(prefix, uri);
    }

    @Override
    public void unparsedEntityDecl(String name, String publicId,
        String systemId, String notationName) throws SAXException {
      // TODO Auto-generated method stub
      super.unparsedEntityDecl(name, publicId, systemId, notationName);
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
      // TODO Auto-generated method stub
      super.warning(e);
    }
  }

  public static XmlDocument parse(InputStream input) {
    XmlHandler handler = new XmlHandler();
    try {
      Xml.parse(new InputStreamReader(input), handler);
      XmlDocument result = new XmlDocument();
      result.setElements(handler.elements);
      return result;
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (SAXException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static XmlDocument parse(String input) {
    return XmlDocument.parse(new ByteArrayInputStream(input.getBytes()));
  }

  private TrackableField<List<XmlElement>> elements = new TrackableField<List<XmlElement>>();

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
    return this.elements.getValue();
  }

  private void setElements(List<XmlElement> value) {
    this.elements.setValue(value);
  }
}
