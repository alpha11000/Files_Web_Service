
package fileWebService;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FilesClient {
    
    private static final String endpoint = "http://localhost:8888/filesws";
    
    public static void main(String[ ] args) {
      
      Scanner scanner = new Scanner(System.in);
      
      while(true){
          System.out.println("\nO que deseja fazer?:"+
                            "\n1 - Criar Arquivo;"+
                            "\n2 - Listar Arquivos;"+
                            "\n3 - Escrever em Arquivo;"+
                            "\n4 - Deletar Arquivo;"+
                            "\n5 - Visualizar Arquivo;"+
                            "\n0 - Sair.\n");
          
          int choice = scanner.nextInt();
          String name;
          
          switch(choice){
              case 0:
                  System.exit(0);
              case 1:
                  System.out.println("Digite o nome do arquivo:");
                  name = scanner.next();
                  Map keys_values = new HashMap<String, String>();
                  keys_values.put("name", name);
                  
                  new FilesClient().sendPostRequest(keys_values);
                  break;
              case 2:
                  System.out.println("# Lista de arquivos:");
                  new FilesClient().sendGetRequest(null);
                  break;
              case 3:
                  System.out.println("Digite o nome do arquivo que deseja alterar:");
                  name = scanner.next();
                  System.out.println("Digite o conteudo para o arquivo:");
                  scanner = new Scanner(System.in);
                  String content = scanner.nextLine();
                  scanner = new Scanner(System.in);
                  
                  Map keys_values_1 = new HashMap<String, String>();
                  keys_values_1.put("name", name);
                  keys_values_1.put("content", content);
                  
                  new FilesClient().sendPostRequest(keys_values_1);
                  break;
              case 4:
                  System.out.println("Digite o nome do arquivo que deseja deletar:");
                  name = scanner.next();
                  Map keys_values_2 = new HashMap<String, String>();
                  keys_values_2.put("name", name);
                  System.out.println("deletando");
                  new FilesClient().sendDeleteRequest(keys_values_2);
                  break;
              case 5:
                  System.out.println("Digite o nome do arquivo que deseja visualizar:");
                  name = scanner.next();
                  System.out.println("# Conteudo do arquivo:");
                  new FilesClient().sendGetRequest("name=" + name);
                  break;
              default:
                  System.out.println("escolha invalida.");
                  break;
          }
          
      } 
    }
    
    private void sendPostRequest(Map<String, String> key_value) {
	try {
            //POST requests
            HttpURLConnection conn = get_connection(endpoint, "POST");
            conn.setDoOutput(true);
            
            for(Map.Entry<String, String> entry : key_value.entrySet()){
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            
            print_and_parse(conn, true);
	}
        catch(NullPointerException e) { System.err.println(e); }
    }
    
    private void sendDeleteRequest(Map<String, String> key_value) {

        try {
            //DELETE request
            HttpURLConnection conn = get_connection(endpoint, "DELETE");
            conn.setDoOutput(true);
            
            for(Map.Entry<String, String> entry : key_value.entrySet()){
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            conn.connect();
            
            print_and_parse(conn, true);
        } catch (IOException ex) {
            Logger.getLogger(FilesClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void sendGetRequest(String keys_values) {
	try {
            // GET requests
            HttpURLConnection conn = get_connection(endpoint + ((keys_values == null) ?  "" :"?" + keys_values), "GET");
            conn.connect();
            print_and_parse(conn, true);
	}
        catch(IOException e) { System.err.println(e); }
        catch(NullPointerException e) { System.err.println(e); }
    }
    
    
    
    private HttpURLConnection get_connection(String url_string,String verb) {

        HttpURLConnection conn = null;
        try {
               URL url = new URL(url_string);
               conn = (HttpURLConnection) url.openConnection();
               conn.setRequestMethod(verb);
        }
        catch(MalformedURLException e) { System.err.println(e); }
        catch(IOException e) { System.err.println(e); }
        
        return conn;
    }
    
    private void print_and_parse(HttpURLConnection conn, boolean parse) {
	try {
		String xml = "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                
		String next = null;
		while ((next = reader.readLine()) != null)
		xml += next;
		//System.out.println("The raw XML:\n" + xml);
		if (parse) {
                    SaxParserHandler saxParserHandler = new SaxParserHandler();
                    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                       
                    parser.parse(new ByteArrayInputStream(xml.getBytes()), saxParserHandler);
                    
                    for(String s : saxParserHandler.getResults()){
                        System.out.println(s);
                    }
                    
		}
	}
	catch(IOException e) { System.err.println(e); }
        
	catch(ParserConfigurationException e) { System.err.println(e); }
        
	catch(SAXException e) { System.err.println(e); }
    
    }
    
    static class SaxParserHandler extends DefaultHandler {
        
        ArrayList<String> results = new ArrayList();
        private StringBuilder elementValue = new StringBuilder();
        
	public void startElement(String uri, String lname, String qname, Attributes attributes) {
            if(qname.toLowerCase().compareTo("string") == 0)
                elementValue = new StringBuilder();            
	}
        
	public void characters(char[ ] data, int start, int length) {
            if (elementValue == null) {
                elementValue = new StringBuilder();
            } else {
                elementValue.append(data, start, length);
            }
	}
        
	public void endElement(String uri, String lname, String qname) {
            if(qname.toLowerCase().compareTo("string") == 0){
                if(elementValue.length() != 0 && elementValue.charAt(0) == '[')
                    elementValue.deleteCharAt(0).deleteCharAt(elementValue.length()-1);
                results.add(elementValue.toString());
            }
	}
        
        public String[] getResults(){
            return results.toArray(new String[0]);
        }
    }  
}
