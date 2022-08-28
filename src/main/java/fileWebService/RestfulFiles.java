
package fileWebService;

import java.beans.XMLEncoder;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.xml.ws.Provider;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.http.HTTPException;

@WebServiceProvider

@ServiceMode(value = javax.xml.ws.Service.Mode.MESSAGE)
@BindingType(value = HTTPBinding.HTTP_BINDING)
public class RestfulFiles implements Provider<Source>{
 
    @Resource
    private WebServiceContext ws_ctx;
    
    private final Map<String, File> files_map; 
    private final String filesDirectory = "files\\";
    private final File directory = new File(filesDirectory);

    
    public RestfulFiles(){
        files_map = new HashMap();
        
        if(!directory.exists())
            directory.mkdir();
        
        File[] files = directory.listFiles();
        for(File file : files){
            files_map.put(file.getName(), file);
        }
    }
    
    @Override
    public Source invoke(Source request) {
        if (ws_ctx == null) throw new RuntimeException("DI failed on ws_ctx.");

        
        MessageContext msg_ctx = ws_ctx.getMessageContext();
        
        String http_verb = (String)      
        msg_ctx.get(MessageContext.HTTP_REQUEST_METHOD);
        
        http_verb = http_verb.trim().toUpperCase();
        
        
        if (http_verb.equals("GET")) return doGet(msg_ctx);
        if (http_verb.equals("POST")) return doPost(msg_ctx);
        if (http_verb.equals("DELETE")) return doDelete(msg_ctx);
        else throw new HTTPException(405); 
    }

    private Source doGet(MessageContext msg_ctx) {
        
        String query_string = (String) msg_ctx.get(MessageContext.QUERY_STRING);
        ByteArrayInputStream stream = null;
        
        if (query_string == null){
            System.out.println("Enviando lista de arquivos...");
            String[] names = getFileNames();
            stream = encode_to_stream(names);
        } else {
           String name = get_value_from_qs("name", query_string);
           System.out.println("Enviando conteúdo do arquivo " + name);
           File file = files_map.get(name);  
           if (file == null) throw new HTTPException(404); 
           
           try {
                byte[] encoded = Files.readAllBytes(Paths.get(file.getPath()));
                String out = new String(encoded, Charset.defaultCharset());
                stream = encode_to_stream(out);
           } catch (FileNotFoundException ex) {
               Logger.getLogger(RestfulFiles.class.getName()).log(Level.SEVERE, null, ex);
               throw new HTTPException(404);
           } catch (IOException ex) {
               Logger.getLogger(RestfulFiles.class.getName()).log(Level.SEVERE, null, ex);
               throw new HTTPException(500);
            }
        }
        
        return new StreamSource(stream);
    }
    
    private Source doPost(MessageContext msg_ctx) {
        Map headers = (Map)msg_ctx.get(MessageContext.HTTP_REQUEST_HEADERS);
        ByteArrayInputStream stream;
        
        if(headers == null) throw new HTTPException(400);
        
        String name = headers.get("Name").toString();
        name = (name.length() == 2) ? "" : name.substring(1, name.length()-1);
        
        File file = null;
        
        if(files_map.containsKey(name)){
            System.out.println("Atualizando conteudo do arquivo " + name);
            file = files_map.get(name);
            stream = encode_to_stream(HttpURLConnection.HTTP_OK);
        }else{
            System.out.println("Criando arquivo " + name);
            files_map.put(name, new File(filesDirectory + name));
            writeOnFile(files_map.get(name), "");
            stream = encode_to_stream(HttpURLConnection.HTTP_CREATED);
        }
        
        if(headers.containsKey("Content")){
            String content = headers.get("Content").toString();
            writeOnFile(file, content);
         }
        
        return new StreamSource(stream);
    }
    
    private Source doDelete(MessageContext msg_ctx) {
        Map headers = (Map)msg_ctx.get(MessageContext.HTTP_REQUEST_HEADERS);
        ByteArrayInputStream stream = null;
        
        if(headers == null) throw new HTTPException(400);
        
        String name = headers.get("Name").toString();
        name = (name.length() == 2) ? "" : name.substring(1, name.length()-1);
        
        if(files_map.containsKey(name)){
            System.out.println("Deletando o arquivo " + name);
            files_map.get(name).delete();
            files_map.remove(name);
            stream = encode_to_stream(HttpURLConnection.HTTP_OK);
        }else{
            throw new HTTPException(404);
        }
        
        return new StreamSource(stream);
    }
    

    private String get_value_from_qs(String key, String query_string) {
        
        String[] parts = query_string.split("=");
        if (!parts[0].equalsIgnoreCase(key))
            throw new HTTPException(400);
        
        return parts[1].trim();
    }

    private ByteArrayInputStream encode_to_stream(Object obj) {
       ByteArrayOutputStream stream = new ByteArrayOutputStream();
       XMLEncoder enc = new XMLEncoder(stream);
  
       enc.writeObject(obj);
       enc.close();
       return new ByteArrayInputStream(stream.toByteArray());
    }
    
    private String[] getFileNames(){
        String[] fileNames = new String[files_map.size()];
        int i = 0;
        
        for(String name : files_map.keySet()){
            fileNames[i++] = name;
        }
        
        return fileNames;
    }
    
    private void writeOnFile(File file, String content){
       if(file == null){
           System.out.println("NULL");
           return;
       }
       
        OutputStream out;
        try {
            out = new FileOutputStream(file);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, "utf-8"));
            writer.write(content);
            writer.close();
            out.close();
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RestfulFiles.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RestfulFiles.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RestfulFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
            
}
