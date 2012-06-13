package depollsoft.bbsservices.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ChapterRefreshServlet extends HttpServlet {

  private static final long serialVersionUID = -6929584363239695112L;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    doPost(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String params = "cmbMiles=25&cmbDistrict=400007&txtCity=&cmbState=&txtZipCode=&txtChapterName=+&txtChorusName=&cmdSearchByName=Search+by+Name";
    URL chapterListUrl = new URL(
        "http://ebiz.barbershop.org/ebusiness/Public/ChapterProximitySearch2.aspx");
    HttpURLConnection connection = (HttpURLConnection) chapterListUrl.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    connection.setDoOutput(true);
    OutputStream os = connection.getOutputStream();
    os.write(params.getBytes());
    os.close();
    InputStream is = connection.getInputStream();
    byte[] buffer = new byte[connection.getContentLength()];
    is.read(buffer);
    String asAString = new String(buffer);
    log(asAString);
    OutputStream output = resp.getOutputStream();
    output.write(buffer);
  }

}
