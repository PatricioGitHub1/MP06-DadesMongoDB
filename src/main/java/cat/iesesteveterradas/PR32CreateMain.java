package cat.iesesteveterradas;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.text.StringEscapeUtils;
import org.basex.api.client.ClientSession;
import org.basex.core.BaseXException;
import org.basex.core.cmd.Open;
import org.basex.core.cmd.XQuery;

import java.util.logging.FileHandler;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class PR32CreateMain {
    private static final Logger logger = Logger.getLogger("MyLog");  
    static FileHandler fh;  
    private static final String XML_DB_NAME = "coffee.stackexchange";

    public static void main(String[] args) {
        try {
            fh = new FileHandler("data/logs/PR32CreateMain.java.log", true);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  

            logger.info("Starting xquery to extract Data");  

            String rawXqueryResult = getXqueryResult();

            // funcion para sacar los datos del xquery y introducirlos en MongoDB
            getDataFromResult(rawXqueryResult);
        } catch (BaseXException e) {
            logger.info("Error connecting or executing the query: " + e.getMessage());
        } catch (IOException e) {
            logger.info(e.getMessage());
        }  
    }

    public static String getXqueryResult() throws BaseXException, IOException{
        // Initialize connection details
        String host = "127.0.0.1";
        int port = 1984;
        String username = "admin"; // Default username
        String password = "admin"; // Default password

        // Establish a connection to the BaseX server
        ClientSession session = new ClientSession(host, port, username, password);
        logger.info("Connected to BaseX server.");
        session.execute(new Open(XML_DB_NAME));

        String xquery = "declare option output:method \"xml\";" +
        "declare option output:indent \"yes\";" +
        "<posts>{" +
        "  let $sorted-posts := " +
        "    for $p in /posts/row[@PostTypeId = \"1\"]" +
        "    let $views := xs:integer($p/@ViewCount)" +
        "    order by $views descending" +
        "    return $p" +
        "  return subsequence($sorted-posts, 1, 10000)" +
        "}</posts>";

        String result = session.execute(new XQuery(xquery));
        logger.info("Query executed, closing session");

        session.close();
        
        return result;
            
    }

    public static void getDataFromResult(String xmlString) {
        try (var mongoClient = MongoClients.create("mongodb://root:example@localhost:27017")){
            // Conexion a MongoDB
            MongoDatabase database = mongoClient.getDatabase("StackExchange");
            MongoCollection<org.bson.Document> collection = database.getCollection("questionsCollection");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlString)));

            NodeList nodeList = document.getElementsByTagName("row");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Element row = (Element) nodeList.item(i);
                String id = row.getAttribute("Id");
                String postTypeId = row.getAttribute("PostTypeId");
                String acceptedAnswerId = row.getAttribute("AcceptedAnswerId");
                String creationDate = row.getAttribute("CreationDate");
                String score = row.getAttribute("ViewCount");
                String viewCount = row.getAttribute("Id");   
                String body = StringEscapeUtils.unescapeHtml4(row.getAttribute("Body"));
                String ownerUserId = row.getAttribute("OwnerUserId");
                String lastActivityDate = row.getAttribute("LastActivityDate");
                String title = StringEscapeUtils.unescapeHtml4(row.getAttribute("Title"));
                String tags = row.getAttribute("Tags");
                String answerCount = row.getAttribute("AnswerCount");
                String commentCount = row.getAttribute("CommentCount");
                String contentLicense = row.getAttribute("ContentLicense");

                org.bson.Document question = new org.bson.Document()
                                .append("Id", id)
                                .append("PostTypeId", postTypeId)
                                .append("AcceptedAnswerId", acceptedAnswerId)
                                .append("CreationDate", creationDate)
                                .append("Score", score)
                                .append("ViewCount", Integer.parseInt(viewCount))
                                .append("Body", body)
                                .append("OwnerUserId", ownerUserId)
                                .append("LastActivityDate", lastActivityDate)
                                .append("Title", title)
                                .append("Tags", tags)
                                .append("AnswerCount", answerCount)
                                .append("CommentCount", commentCount)
                                .append("ContentLicense", contentLicense);

                // Inserir el document a la col·lecció
                collection.insertOne(question);
            }
            logger.info("All documents where inserted succesfully");
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("An exception took place while inserting data into MongoDB Collection");
        }

    }
}
