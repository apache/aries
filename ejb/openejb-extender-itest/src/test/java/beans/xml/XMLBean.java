package beans.xml;

public class XMLBean implements LocalIface, RemoteIface {

  public String getRemoteString() {
    return "A Remote Call";
  }

  public String getLocalString() {
    
    return "A Local Call";
  }

}
