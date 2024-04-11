package cat.iesesteveterradas;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;

public class PR32QueryMain {
    private static final Logger logger = Logger.getLogger("MyLog");  
    static FileHandler fh;  
    public static void main(String[] args) {
        try {
            fh = new FileHandler("data/logs/PR32CreateMain.java.log", true);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
            
            logger.info("Establishing connection to MongoDB...");

            logger.info("Extracting data from MongoDB...");

            double avg = getAvgViewCount();

            if (avg <= 0) {
                logger.log(Level.SEVERE, "The average is not greater than 0");
                return;
            }

            FindIterable<Document> resultGrtAvg = mongoQueryAvg(avg);

            saveResultPDF("informe1.pdf",resultGrtAvg);

            FindIterable<Document> resultRegexTitle = mongoQueryRegex("Title", ".*\\b(pug|wig|yak|nap|jig|mug|zap|gag|oaf|elf)\\b.*");
            
            saveResultPDF("informe2.pdf", resultRegexTitle);

            logger.info("Ending mongoDB operations");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "das");
        }  

    }

    public static MongoCollection<Document> getAllMongoCollection() {
        try  {
            MongoClient mongoClient = MongoClients.create("mongodb://root:example@localhost:27017");
            MongoDatabase database = mongoClient.getDatabase("StackExchange");
            MongoCollection<Document> collection = database.getCollection("questionsCollection");

            return collection;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred", e);
        }

        return null;
    }

    public static FindIterable<Document> mongoQueryAvg(double avg) {
        try  {
        
            MongoCollection<Document> collection = getAllMongoCollection();
            
            FindIterable<Document> result = collection.find(Filters.gt("ViewCount", avg));

            return result;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred", e);
        }

        return null;
    }

    public static double getAvgViewCount() {
        MongoCollection<Document> allEntries = getAllMongoCollection();

        logger.info("The number of documents in the collection: "+allEntries.countDocuments());

        AggregateIterable<org.bson.Document> aggregate = allEntries.aggregate(Arrays.asList(Aggregates.group("_id", new BsonField("averageView", new BsonDocument("$avg", new BsonString("$ViewCount"))))));

        Document result = aggregate.first();
        double avg = result.getDouble("averageView");
        System.out.println(avg);
        return avg;
    }

    public static FindIterable<Document> mongoQueryRegex(String field, String regex) {
        try  {
        
            MongoCollection<Document> collection = getAllMongoCollection();

            // Utilitzar una expressió regular per buscar 'with' en el títol
            Bson regexQuery = new Document(field, new Document("$regex", regex).append("$options", "i"));

            // Realitzar la consulta
            FindIterable<Document> result = collection.find(regexQuery);

            return result;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred", e);
        }

        return null;
    }

    public static void saveResultPDF(String pdfTitle, FindIterable<Document> resultDocuments) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream contentStream = null;

            try {
                contentStream = new PDPageContentStream(document, page);
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(25, 750);

                int lines = 0;
                for (Document doc : resultDocuments) {
                    if (lines > 50) {
                        contentStream.endText();
                        contentStream.close();
                        
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        contentStream.setLeading(14.5f);
                        contentStream.newLineAtOffset(25, 750);
                        lines = 0;
                    }

                    lines++;
                    String line = (String) doc.get("Title");
                    contentStream.showText(line);
                    contentStream.newLine();
                }
                
                contentStream.endText();
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            File outFolder = new File(System.getProperty("user.dir") + "/data/out/");

            if (!outFolder.exists()) {
                outFolder.mkdir();
            }

            String outputPath = System.getProperty("user.dir") + "/data/out/" + pdfTitle;

            
            document.save(outputPath);
            logger.info("PDF creat a: " + outputPath);
        } catch (IOException e) {
            logger.log(Level.SEVERE,"Error al crear el PDF: " + e.getMessage() );
        }
    }
}
