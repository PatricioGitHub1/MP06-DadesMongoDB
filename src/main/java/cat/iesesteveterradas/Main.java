package cat.iesesteveterradas;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.List;

import org.basex.api.client.ClientSession;
import org.basex.core.*;
import org.basex.core.cmd.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import edu.stanford.nlp.pipeline.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);    

    public static void main(String[] args) throws IOException {
         // Initialize connection details
        String host = "127.0.0.1";
        int port = 1984;
        String username = "admin"; // Default username
        String password = "admin"; // Default password

        // Establish a connection to the BaseX server
        try (ClientSession session = new ClientSession(host, port, username, password)) {
            logger.info("Connected to BaseX server.");
            session.execute(new Open("coffee.stackexchange"));

            // exercici 2 
            exercici2(session);

            // exercici 3
            exercici3(session);
            
        } catch (BaseXException e) {
            logger.error("Error connecting or executing the query: " + e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }        
    }

    public static void exercici2(ClientSession session) throws FileNotFoundException, IOException {
        Path inputPath = Paths.get(System.getProperty("user.dir"),"data","input");
        Path outputPath = Paths.get(System.getProperty("user.dir"),"data","output");
        
        File inputDir = inputPath.toFile();
        File outputDir = outputPath.toFile();

        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        File[] files = inputDir.listFiles();

        for (File xqueryFile : files) {
            logger.info("Executing file "+xqueryFile.getName()+"...");
            
            BufferedReader br = new BufferedReader(new FileReader(xqueryFile));
            String query = "";

            try {
                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    query += sCurrentLine;
                }
                
                // guardamos el resultado y escribimos el resultado en un archivo
                String result = session.execute(new XQuery(query));
                
                // esto me lo dio el amigo Chatgpt
                File outputFile = new File(outputDir, xqueryFile.getName().substring(0, xqueryFile.getName().lastIndexOf('.')) + ".xml"); 

                outputFile.createNewFile();

                PrintWriter out = new PrintWriter(outputFile);
                out.println(result);

                out.close();
                logger.info("Query result saved on file "+outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            br.close();
        }
    }

    public static void exercici3(ClientSession session) throws IOException{
        // query para sacar titulos y bodies de las 5 preguntas mas vistas
        String xquery = "declare option output:method 'text';"
              + "declare option output:indent 'no';"
              + "let $sortedPosts :="
              + "  for $p in /posts/row[@PostTypeId = '1']"
              + "  let $views := xs:integer($p/@ViewCount)"
              + "  order by $views descending"
              + "  return string-join((data($p/@Title), data($p/@Body)))"
              + "return subsequence($sortedPosts, 1, 5)";


        String text = session.execute(new XQuery(xquery));
        //String text = "John Doe, a software engineer at Google, recently visited New York City. He said, \"It's an amazing place!\" The trip made him feel very happy.";
        
        File entitiesOutputFile = new File(System.getProperty("user.dir") + "/data/noms_propis.txt");
        String resultingString = "";

        String basePath = System.getProperty("user.dir") + "/data/models/";

        // Updated paths for the provided model files
        InputStream modelInSentence = new FileInputStream(basePath + "opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin");
        InputStream modelInToken = new FileInputStream(basePath + "opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin");
        InputStream modelInPOS = new FileInputStream(basePath + "en-pos-maxent.bin");
        InputStream modelInPerson = new FileInputStream(basePath + "en-ner-person.bin");

        // Sentence detection
        SentenceModel modelSentence = new SentenceModel(modelInSentence);
        SentenceDetectorME sentenceDetector = new SentenceDetectorME(modelSentence);
        String[] sentences = sentenceDetector.sentDetect(text);
        logger.info("Sentence Detection:");
        Arrays.stream(sentences).forEach(sentence -> logger.info(sentence));

        // Tokenization
        TokenizerModel modelToken = new TokenizerModel(modelInToken);
        TokenizerME tokenizer = new TokenizerME(modelToken);
        logger.info("\nTokenization and POS Tagging:");
        for (String sentence : sentences) {
            try{
                String[] tokens = tokenizer.tokenize(sentence);
            
                // POS Tagging
                POSModel modelPOS = new POSModel(modelInPOS);
                POSTaggerME posTagger = new POSTaggerME(modelPOS);
                String[] tags = posTagger.tag(tokens);
                
                for (int i = 0; i < tokens.length; i++) {
                    logger.info(tokens[i] + " (" + tags[i] + ")");
                }
            } catch (Exception e){
                logger.error(e.getMessage());
            }
        }

        // Named Entity Recognition
        TokenNameFinderModel modelPerson = new TokenNameFinderModel(modelInPerson);
        NameFinderME nameFinder = new NameFinderME(modelPerson);
        logger.info("\nNamed Entity Recognition:");
        for (String sentence : sentences) {
            String[] tokens = tokenizer.tokenize(sentence);
            opennlp.tools.util.Span[] nameSpans = nameFinder.find(tokens);
            for (opennlp.tools.util.Span s : nameSpans) {
                logger.info("Entity: " + tokens[s.getStart()]);
            }
        }

        // Clean up IO resources
        modelInSentence.close();
        modelInToken.close();
        modelInPOS.close();
        modelInPerson.close();


        // Inicialitza Stanford CoreNLP
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, sentiment");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // Crea un document amb el text
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        // Obté les frases del document
        List<CoreMap> sentencesList2 = document.get(SentencesAnnotation.class);

        for (CoreMap sentence : sentencesList2) {
            // Mostra tokens i etiquetes POS de cada frase
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                String pos = token.get(PartOfSpeechAnnotation.class);
                logger.info(word + " (" + pos + ")");
            }
            
            // Mostra el reconeixement d'entitats anomenades
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                String ne = token.get(NamedEntityTagAnnotation.class);
                logger.info("Entity: " + word + " (" + ne + ")");
            }
            
            String currentEntityType = "";
            String fullEntityWord = "";

            // Reconeixement de Named Entity Recognition (NER)
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.getString(TextAnnotation.class);
                String ner = token.getString(NamedEntityTagAnnotation.class);
                
                // Comprova si el token és una entitat anomenada (NER)

                // CHATGPT ayudo a debuggar esta parte
                if (!"O".equals(ner)) { 
                    logger.info("Entity Detected: " + word + " - Entity Type: " + ner);

                    if (currentEntityType.equals(ner)) {
                        fullEntityWord += " " + word; 
                    } else {

                        if (!fullEntityWord.isEmpty()) {
                            resultingString += fullEntityWord + " - Entity Type: " + currentEntityType +"\n";
                        }
                        currentEntityType = ner;
                        fullEntityWord = "Entity Detected: " + word; 
                    }

                }
            }

            if (!fullEntityWord.isEmpty()) {
                resultingString += fullEntityWord + " - Entity Type: " + currentEntityType +"\n"; 
            }

            // Guardamos resultado en un archivo
            if (!entitiesOutputFile.exists()) {
                entitiesOutputFile.createNewFile();
            }

            PrintWriter out = new PrintWriter(entitiesOutputFile);
            out.println(resultingString);
            out.close();
            
            logger.info("NLP result saved on file /data/noms_propis.txt!");
            
            // Anàlisi de sentiments
            String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
            logger.info("Sentiment: " + sentiment);
        }    
        
    }
}
