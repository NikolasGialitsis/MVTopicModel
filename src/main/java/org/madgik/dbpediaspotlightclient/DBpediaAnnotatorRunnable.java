/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.madgik.dbpediaspotlightclient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.madgik.dbpediaspotlightclient.DBpediaAnnotator.AnnotatorType;
import org.madgik.io.TMDataSource;
import org.madgik.io.modality.Text;

/**
 * Simple web service-based annotation client for DBpedia Spotlight.
 *
 * @author pablomendes, Joachim Daiber
 */
public class DBpediaAnnotatorRunnable implements Runnable {

    public static final String RESOURCE_POISON = "$poison$";

    public static Logger logger = Logger.getLogger(DBpediaAnnotator.class.getName());
    int startDoc, numDocs;
    final BlockingQueue<Text> pubsQueue;
    final BlockingQueue<String> resourcesQueue;
    AnnotatorType annotator;
    int threadId = 0;
    String spotlightService;
    double confidence;
    TMDataSource outputWriter;
    Map<String, List<DBpediaResource>> allEntities;
    Set<DBpediaResource> allResources;


    //private HttpClient client = new HttpClient();
    private final HttpClient httpClient;

    public DBpediaAnnotatorRunnable(
            TMDataSource outputWriter, AnnotatorType annotator,
            BlockingQueue<Text> pubs, int threadId, HttpClient httpClient,
            BlockingQueue<String> resources, String spotlightService, double confidence) {
        this.outputWriter = outputWriter;
        this.pubsQueue = pubs;
        this.annotator = annotator;
        this.threadId = threadId;
        this.httpClient = httpClient;
        this.resourcesQueue = resources;
        this.spotlightService = spotlightService;
        this.confidence = confidence;
        allEntities = new HashMap<>();
        allResources = new HashSet<>();
    }

    public String getMeSHLabelById(String meshId) {

        String response = "";
        String label = "";

        try {
            String searchUrl = "https://id.nlm.nih.gov/mesh/" + meshId + ".json";
            //+ "query=" + URLEncoder.encode(query, "utf-8")
            //+ "&format=json";

            GetMethod getMethod = new GetMethod(searchUrl);

            //getMethod.addRequestHeader(new Header("Accept", "application/json"));
            response = request(getMethod);

        } catch (Exception e) {
            logger.error("Invalid response from MeSH API:" + e);
            //throw new Exception("Received invalid response from DBpedia Spotlight API.");

        }

        assert response != null;

        JSONObject resultJSON = null;
        

        try {
            resultJSON = new JSONObject(response);
            label = resultJSON.getJSONArray("@graph").getJSONObject(0).getJSONObject("label").getString("@value");

        } catch (JSONException e) {
            logger.error("Invalid JSON response from dbpedia API:" + e);

        }

        return label;
    }

    public void getAndUpdateDetails(String resourceURI) {

        String response = "";

        String query = "prefix dbpedia-owl: <http://dbpedia.org/ontology/>\n" +
"				prefix dcterms: <http://purl.org/dc/terms/>                              \n" +
"                \n" +
"                                 SELECT  ?label ?subject ?subjectLabel ?redirect ?redirectLabel ?disambiguates ?disambiguatesLabel \n" +
"					 ?abstract ?type ?typeLabel ?meshId ?icd10\n" +
"                                 WHERE {\n" +
"                                     ?uri dbpedia-owl:abstract ?abstract .\n" +
"				OPTIONAL{\n" +
"                                     ?uri rdfs:label ?label .\n" +
"}\n" +
"				OPTIONAL{\n" +
"                                     ?uri rdf:type ?type .\n" +
"                                     ?type rdfs:label ?typeLabel .\n" +
"}\n" +
"				OPTIONAL{\n" +
"                                     ?uri dbo:meshId ?meshId .\n" +
"  \n" +
"				}\n" +
"				OPTIONAL{\n" +
"                                     ?uri dbo:icd10 ?icd10 .\n" +
"  				}\n" +
"				OPTIONAL{\n" +
"                                     ?uri dcterms:subject ?subject.\n" +
"                                     ?subject rdfs:label ?subjectLabel .\n" +
"				}\n" +
"				OPTIONAL{\n" +
"                                     ?disambiguates  dbpedia-owl:wikiPageDisambiguates  ?uri .\n" +
"                                     ?disambiguates rdfs:label ?disambiguatesLabel.\n" +
"				}\n" +
"				OPTIONAL{\n" +
"                                     ?redirect dbpedia-owl:wikiPageRedirects ?uri .                    \n" +
"                                     ?redirect rdfs:label ?redirectLabel .\n" +
"				}\n" +
"                                     FILTER (?uri = <" + resourceURI + "> \n" +
"                                     && langMatches(lang(?label),\"en\")  \n" +
"                                     && langMatches(lang(?abstract),\"en\") \n" +
"                                     && (lang(?disambiguatesLabel) = \"\" || langMatches(lang(?disambiguatesLabel),\"en\"))\n" +
"                                     && (lang(?redirectLabel) = \"\" || langMatches(lang(?redirectLabel),\"en\"))\n" +
"                                     && (lang(?subjectLabel) = \"\" || langMatches(lang(?subjectLabel),\"en\"))\n" +
"                                     && (lang(?typeLabel) = \"\" || langMatches(lang(?typeLabel),\"en\"))\n" +
"                                     && (lang(?type) = \"\" || strstarts(str(?type), \"http://dbpedia.org/ontology/\"))\n" +
")  \n" +
"                                 }";
        
        
             
        try {
            String searchUrl = "http://dbpedia.org/sparql?"
                    + "query=" + URLEncoder.encode(query, "utf-8")
                    + "&format=json";

            GetMethod getMethod = new GetMethod(searchUrl);

            //getMethod.addRequestHeader(new Header("Accept", "application/json"));
            response = request(getMethod);

        } catch (Exception e) {
            logger.error("Invalid response from dbpedia API:" + e);
            //throw new Exception("Received invalid response from DBpedia Spotlight API.");

        }

        assert response != null;

        JSONObject resultJSON = null;
        JSONArray entities = null;

        try {
            resultJSON = new JSONObject(response);
            entities = resultJSON.getJSONObject("results").getJSONArray("bindings");
        } catch (JSONException e) {
            logger.error("Invalid JSON response from dbpedia API:" + e);

        }

        if (entities != null) {
            Set<DBpediaLink> categories = new HashSet<DBpediaLink>();
            Set<DBpediaLink> abreviations = new HashSet<DBpediaLink>();
            Set<DBpediaLink> types = new HashSet<DBpediaLink>();
            String resourceAbstract = "";
            String label = "";
            String icd10 = "";
            String meshId = "";
            String mesh = "";

            for (int i = 0; i < entities.length(); i++) {
                try {
                    JSONObject entity = entities.getJSONObject(i);

                    try {
                        types.add(new DBpediaLink(entity.getJSONObject("type").getString("value"), entity.getJSONObject("typeLabel").getString("value")));
                    } catch (JSONException e) {
                        logger.debug("JSON parsing types not found:" + e);

                    }

                    try {
                        categories.add(new DBpediaLink(entity.getJSONObject("subject").getString("value"), entity.getJSONObject("subjectLabel").getString("value")));
                    } catch (JSONException e) {
                        logger.debug("JSON parsing categories not found:" + e);

                    }

                    try {
                        String redirectLabel = entity.getJSONObject("redirectLabel").getString("value");

                        if (redirectLabel.toUpperCase().equals(redirectLabel)) {
                            abreviations.add(new DBpediaLink(entity.getJSONObject("redirect").getString("value"), redirectLabel));
                        }
                    } catch (JSONException e) {
                        logger.debug("JSON parsing redirectLabel not found:" + e);

                    }
                    try {
                        String disambiguatesLabel = entity.getJSONObject("disambiguatesLabel").getString("value").replace("(disambiguation)", "").trim();
                        if (disambiguatesLabel.toUpperCase().equals(disambiguatesLabel)) {
                            abreviations.add(new DBpediaLink(entity.getJSONObject("disambiguates").getString("value"), disambiguatesLabel));
                        }
                    } catch (JSONException e) {
                        logger.debug("JSON parsing disambiguatesLabel not found:" + e);

                    }

                    if (i == 0) {
                        resourceAbstract = entity.getJSONObject("abstract").getString("value");
                        label = entity.getJSONObject("label").getString("value");

                        try {
                            icd10 = entity.getJSONObject("icd10").getString("value");
                        } catch (JSONException e) {
                            logger.debug("JSON parsing icd10not found:" + e);
                        }

                        try {
                            meshId = entity.getJSONObject("meshId").getString("value");
                            if (!meshId.isEmpty()) {
                                mesh = getMeSHLabelById(meshId);
                            }
                            //TODO: Get MeshLabel (Heading) via https://id.nlm.nih.gov/mesh/D020246.json 
                        } catch (JSONException e) {
                            logger.debug("JSON parsing meshId found:" + e);
                        }
                    }

                } catch (JSONException e) {
                    logger.error("JSON parsing exception from dbpedia API:" + e);

                }

            }

            //public DBpediaResource(DBpediaResourceType type, String URI, String title, int support,  double Similarity, double confidence, String mention, List<String> categories, String wikiAbstract, String wikiId) {
            saveResourceDetails(new DBpediaResource(DBpediaResourceType.Entity, resourceURI, label, 0, 1,
                    1, "", categories, resourceAbstract, "", abreviations, types, meshId, mesh, icd10));
        }
    }

    public List<DBpediaResource> extractFromSpotlight(String input) throws Exception {

        //private final static String API_URL = "http://localhost:2222/rest/candidates";
        //final String API_URL = "http://localhost:2222/rest/annotate";
        final String API_URL = spotlightService.trim(); //"http://model.dbpedia-spotlight.org/en/annotate";
        //http://model.dbpedia-spotlight.org/en/annotate
        //private final static String API_URL = "http://www.dbpedia-spotlight.com/en/annotate";
        //private final static String API_URL = "http://spotlight.sztaki.hu:2222/rest/annotate";
        //final double CONFIDENCE = 0.4;
        final int SUPPORT = 0;

        String spotlightResponse = "";
        LinkedList<DBpediaResource> resources = new LinkedList<DBpediaResource>();
        try {
            GetMethod getMethod = new GetMethod(API_URL + "/?"
                    + "confidence=" + confidence
                    + "&support=" + SUPPORT
                    + "&text=" //President%20Obama%20called%20Wednesday%20on%20Congress%20to%20extend%20a%20tax%20break%20for%20students%20included%20in%20last%20year%27s%20economic%20stimulus%20package,%20arguing%20that%20the%20policy%20provides%20more%20generous%20assistance"
                    //+ URLEncoder.encode("President Obama called Wednesday on Congress to extend a tax break for students included in last year's economic stimulus package, arguing that the policy provides more generous assistance", "utf-8")
                    + URLEncoder.encode(input, "utf-8")
            );

            getMethod.addRequestHeader(new Header("Accept", "application/json"));

            spotlightResponse = request(getMethod);
            if (spotlightResponse.equals("404")) throw new Exception("Spotlight returned response: 404");

        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        } catch (Exception e){
            logger.error(e.getMessage());
        }

        assert spotlightResponse != null;

        JSONObject resultJSON = null;
        JSONArray entities = null;

        try {
            resultJSON = new JSONObject(spotlightResponse);
            entities = resultJSON.getJSONArray("Resources");
        } catch (JSONException e) {
            //FIXME this is pretty common when no resources were found, not an error though. Log level changed from error to debug. We should check spotlightResponse details and show an appropriate error then.
            logger.debug(String.format("Invalid response -no resources- from DBpedia Spotlight API for input %s: %s", input, e));
            return resources;

        }

        JSONObject entity = null;
        for (int i = 0; i < entities.length(); i++) {
            try {
                entity = entities.getJSONObject(i);
                //public DBpediaResource(DBpediaResourceType type, String URI, String title, int support,  double Similarity, double confidence, String mention, List<String> categories, String wikiAbstract, String wikiId) {
                resources.add(
                        new DBpediaResource(DBpediaResourceType.Entity, entity.getString("@URI"), "", Integer.parseInt(entity.getString("@support")), Double.parseDouble(entity.getString("@similarityScore")),
                                1, entity.getString("@surfaceForm"), null, "", "", null, null, "", "", ""));

            } catch (JSONException e) {
                logger.error(String.format("Invalid response -no details- from DBpedia Spotlight API for resource %s: %s", entity.toString(), e));

            }

        }

        return resources;

    }

    public List<DBpediaResource> extractFromTagMe(String input) throws Exception {

        //https://tagme.d4science.org/tagme/tag?lang=en&&include_abstract=true&include_categories=true&gcube-token=27edab24-27a7-4e51-a335-1d5356342cab-843339462&text=latent%20dirichlet%20allocation
        //private final static String API_URL = "http://localhost:2222/rest/candidates";
        final String API_URL = "http://tagme.d4science.org/tagme/tag?lang=en&&include_abstract=true&include_categories=true&gcube-token=27edab24-27a7-4e51-a335-1d5356342cab-843339462&text=";
        //&long_text=30
        String response = "";
        LinkedList<DBpediaResource> resources = new LinkedList<DBpediaResource>();
        try {
            GetMethod getMethod = new GetMethod(API_URL
                    + URLEncoder.encode(input, "utf-8")
            );

            //getMethod.addRequestHeader(new Header("Accept", "application/json"));
            response = request(getMethod);

        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException calling TagMe API:" + e);
        }

        assert response != null;

        JSONObject resultJSON = null;
        JSONArray entities = null;

        /*
        {"timestamp":"2016-12-02T11:36:49","time":0,"test":"5","api":"tag",
        "annotations":[{"abstract":"In natural language processing, Latent Dirichlet allocation (LDA) is a generative statistical model that allows sets of observations to be explained by unobserved groups that explain why some parts of the data are similar. For example, if observations are words collected into documents, it posits that each document is a mixture of a small number of topics and that each word's creation is attributable to one of the document's topics. LDA is an example of a topic model and was first presented as a graphical model for topic discovery by David Blei, Andrew Ng, and Michael I. Jordan in 2003.",
        "id":4605351,
        "title":"Latent Dirichlet allocation",
        "dbpedia_categories":["Statistical natural language processing","Latent variable models","Probabilistic models"],
        "start":0,
        "link_probability":1,
        "rho":0.5,"end":27,"spot":"latent dirichlet allocation"}],"lang":"en"}
         */
        try {
            resultJSON = new JSONObject(response);
            entities = resultJSON.getJSONArray("annotations");
        } catch (JSONException e) {
            logger.error("Invalid response from DBpedia TagMe API:" + e);
            return resources;
            //throw new Exception("Received invalid response from DBpedia Spotlight API. \n");
        }

        for (int i = 0; i < entities.length(); i++) {
            try {
                JSONObject entity = entities.getJSONObject(i);
                Set<DBpediaLink> categories = new HashSet<DBpediaLink>();

                try {
                    JSONArray JSONcategories = entity.getJSONArray("dbpedia_categories");

                    for (int j = 0; j < JSONcategories.length(); j++) {
                        categories.add(new DBpediaLink(JSONcategories.getString(j), ""));
                    }
                } catch (JSONException e) {
                    logger.error("Invalid JSON response from TagMe API:" + e);
                    //LOG.error("JSON exception "+e);
                }
                //public DBpediaResource(DBpediaResourceType type, String URI, String title, int support,  double Similarity, double confidence, String mention, List<String> categories, String wikiAbstract, String wikiId) {
                DBpediaResource newResource = new DBpediaResource(DBpediaResourceType.Entity, "", entity.getString("title"), 0, entity.getDouble("link_probability"),
                        entity.getDouble("rho"), entity.getString("spot"), categories, entity.getString("abstract"), String.valueOf(entity.getInt("id")), null, null, null, null, null);

                resources.add(newResource);

            } catch (JSONException e) {
                logger.error("Invalid JSON response from TagMe API:" + e);

            }

        }

        return resources;

    }

    public void run() {

        final int logBatchSize = 1000;
        if (pubsQueue != null) {

            int counter = 0;
            long batchEntitiesRetrievalExecutionTime = 0;
            long batchEntitiesSavingExecutionTime = 0;

            Text currentPubText;

            try {
                while (!((currentPubText = pubsQueue.take()) instanceof PubTextPoison)) {
                    counter++;
                    long entitiesRetrievalStartTime = System.currentTimeMillis();
                    List<DBpediaResource> entities = getDBpediaEntities(currentPubText.getContent(), annotator);
                    long inbetweenTime = System.currentTimeMillis();
                    batchEntitiesRetrievalExecutionTime += inbetweenTime - entitiesRetrievalStartTime;
                    if (entities.size() > 0) {
                        saveDBpediaEntities(entities, currentPubText.getId(), annotator);
                    }
                    batchEntitiesSavingExecutionTime += System.currentTimeMillis() - inbetweenTime;

                    if (counter % logBatchSize == 0) {
                        logger.info(String.format("[%s]: time taken to retrieve dbpedia entities for a batch of %s publications: %s secs",
                                threadId, logBatchSize, batchEntitiesRetrievalExecutionTime / 1000));
                        logger.info(String.format("[%s]: time taken to store dbpedia entities for a batch of %s publications: %s secs",
                                threadId, logBatchSize, batchEntitiesSavingExecutionTime / 1000));
                        batchEntitiesRetrievalExecutionTime = 0;
                        batchEntitiesSavingExecutionTime = 0;
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("got interrupted exception", e);
            } finally {
                logger.info("annotation thread has finished");
            }
        } else if (resourcesQueue != null) {

            int counter = 0;
            long batchUpdateResourceExecutionTime = 0;

            String currentResource;

            try {
                while (!RESOURCE_POISON.equals((currentResource = resourcesQueue.take()))) {
                    counter++;
                    final long startDocTime = System.currentTimeMillis();
                    getAndUpdateDetails(currentResource);
                    final long endDocTime = System.currentTimeMillis();
                    batchUpdateResourceExecutionTime += endDocTime - startDocTime;

                    if (counter % logBatchSize == 0) {
                        logger.info(String.format("[%s]: Time taken to update resources details for a batch of %s resources: %s secs",
                                threadId, logBatchSize, batchUpdateResourceExecutionTime / 1000));
                        batchUpdateResourceExecutionTime = 0;
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("[%s]: Extraction time for %s resource: %s ms  \n", threadId, currentResource, (endDocTime - startDocTime)));
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("got interrupted exception", e);
            } finally {
                logger.info("resource update thread has finished");
            }

        }
    }

    protected static String readFileAsString(String filePath) throws java.io.IOException {
        return readFileAsString(new File(filePath));
    }

    protected static String readFileAsString(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        BufferedInputStream f = new BufferedInputStream(new FileInputStream(file));
        f.read(buffer);
        return new String(buffer);

    }

    static abstract class LineParser {

        public abstract String parse(String s) throws ParseException;

        static class ManualDatasetLineParser extends LineParser {

            public String parse(String s) throws ParseException {
                return s.trim();
            }
        }

        static class OccTSVLineParser extends LineParser {

            public String parse(String s) throws ParseException {
                String result = s;
                try {
                    result = s.trim().split("\t")[3];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new ParseException(e.getMessage(), 3);
                }
                return result;
            }
        }
    }

    public void saveDBpediaEntities(List<DBpediaResource> entities, String pubId, AnnotatorType annotator) {
        outputWriter.saveSemanticAugmentationSingleOutput(entities, pubId, annotator);
        allEntities.put(pubId, entities);
    }

    public void saveResourceDetails(DBpediaResource resource) {
        outputWriter.saveSemanticOutputResourceDetails(resource);
        allResources.add(resource);

    }

    public List<DBpediaResource> getDBpediaEntities(String text, AnnotatorType annotator) {

        LineParser parser = new LineParser.ManualDatasetLineParser();
        List<DBpediaResource> entities = new ArrayList<DBpediaResource>();
        int correct = 0;
        int error = 0;
        int sum = 0;
        int i = 0;

        String txt2Annotate = "";
        int txt2AnnotatNum = 0;
        final long startDocTime = System.currentTimeMillis();
        //String[] txts = text.split("\n");
        String[] txts = text.split("\\.");
        for (String snippet : txts) {
            String s = "";
            try {
                s = parser.parse(snippet);
            } catch (Exception e) {

                logger.error(e.toString());
            }
            if (s != null && !s.equals("")) {
                i++;
                txt2Annotate += (" " + s);

                if ((i % 10) == 0 || i == txts.length - 1) {
                    txt2AnnotatNum++;

                    try {
                        final long startTime = System.currentTimeMillis();
                        if (annotator == AnnotatorType.spotlight) {
                            entities.addAll(extractFromSpotlight(txt2Annotate.replaceAll("\\s+", " ")));
                        } else if (annotator == AnnotatorType.tagMe) {
                            entities.addAll(extractFromTagMe(txt2Annotate.replaceAll("\\s+", " ")));
                        }

                        final long endTime = System.currentTimeMillis();
                        sum += (endTime - startTime);

                        correct++;
                    } catch (Exception e) {
                        error++;
                        logger.error(e.toString());

                        e.printStackTrace();
                    }

                    txt2Annotate = "";
                }

            }

        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s]: Extracted %s entities from %s text items, with %s successes and %s errors. \n", threadId, entities.size(), txt2AnnotatNum, correct, error));
            double avg = (new Double(sum) / txt2AnnotatNum);
            final long endDocTime = System.currentTimeMillis();
            logger.debug(String.format("[%s]: Extraction time for pub: Total:%s ms AvgPerRequest:%s ms \n", threadId, (endDocTime - startDocTime), avg));
        }
        return entities;
    }

    public void saveExtractedEntitiesSet(File inputFile, File outputFile, LineParser parser, int restartFrom, AnnotatorType annotator) throws Exception {
        PrintWriter out = new PrintWriter(outputFile);
        //LOG.info("Opening input file "+inputFile.getAbsolutePath());
        String text = readFileAsString(inputFile);
        int i = 0;
        int correct = 0;
        int error = 0;
        int sum = 0;
        for (String snippet : text.split("\n")) {
            String s = parser.parse(snippet);
            if (s != null && !s.equals("")) {
                i++;

                if (i < restartFrom) {
                    continue;
                }

                List<DBpediaResource> entities = new ArrayList<DBpediaResource>();
                try {
                    final long startTime = System.nanoTime();
                    if (annotator == AnnotatorType.spotlight) {
                        entities.addAll(extractFromSpotlight(snippet.replaceAll("\\s+", " ")));
                    } else if (annotator == AnnotatorType.tagMe) {
                        entities.addAll(extractFromTagMe(snippet.replaceAll("\\s+", " ")));
                    }
                    final long endTime = System.nanoTime();
                    sum += endTime - startTime;
                    logger.info(String.format("(%s) Extraction ran in %s ns. \n", i, endTime - startTime));

                    correct++;
                } catch (Exception e) {
                    error++;
                    logger.error(e);

                }
                for (DBpediaResource e : entities) {
                    out.println(e.getLink().uri);
                }
                out.println();
                out.flush();
            }
        }
        out.close();
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Extracted entities from %s text items, with %s successes and %s errors. \n", i, correct, error));
            logger.debug("Results saved to: " + outputFile.getAbsolutePath());
            double avg = (new Double(sum) / i);
            logger.debug(String.format("Average extraction time: %s ms \n", avg * 1000000));
        }
    }

    public void evaluate(File inputFile, File outputFile) throws Exception {
        evaluateManual(inputFile, outputFile, 0);
    }

    public void evaluateManual(File inputFile, File outputFile, int restartFrom) throws Exception {
        saveExtractedEntitiesSet(inputFile, outputFile, new LineParser.ManualDatasetLineParser(), restartFrom, AnnotatorType.spotlight);
    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            logger.error(e);

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }

        return sb.toString();

    }

    public String request(HttpMethod method) throws Exception {

        String spotlightResponse = null;

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));

        try {
            // Execute the method.
            int statusCode = httpClient.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                return String.valueOf(statusCode);
            }

            // Read the response body.
            InputStream responseBody = method.getResponseBodyAsStream(); // .getResponseBody(); //TODO Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended.

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            spotlightResponse = getStringFromInputStream(responseBody);

        } catch (HttpException e) {

            throw new Exception("Protocol error executing HTTP request.", e);
        } catch (IOException e) {

            throw new Exception("Transport error executing HTTP request.", e);
        } finally {
            // Release the connection.
            method.releaseConnection();
        }

        return spotlightResponse;

    }

}
