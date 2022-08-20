package fileWebService;

import javax.xml.ws.Endpoint;

public class RestfulFilesPublisher {
    public static void main(String[ ] args) {
        int port = 8888;
        String url = "http://localhost:" + port + "/filesws";
        System.out.println("Publishing files on port " + port);
        Endpoint.publish(url, new RestfulFiles());
    }
}
