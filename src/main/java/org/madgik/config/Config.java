package org.madgik.config;

import org.apache.log4j.Logger;
import org.madgik.MVTopicModel.SciTopicFlow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Config extends Properties {

    public enum ExperimentType { ACM, PubMed }
    public enum SimilarityType { cos, Jen_Sha_Div, symKL}
    public enum Net2BoWType { OneWay, TwoWay, PPR}


    int numTopics;

    public int getNumTopWords() {
        return numTopWords;
    }

    int numTopWords;

    public byte getNumModalities() {
        return (byte) modalities.size();
    }

    public List<String> getModalities() {
        return modalities;
    }

    public void setModalities(List<String> modalities) {
        this.modalities = modalities;
    }

    List<String> modalities;

    public int getNumIterations() {
        return numIterations;
    }

    int numIterations;

    public int getShowTopicsInterval() {
        return showTopicsInterval;
    }

    int showTopicsInterval;
    int numOfThreads;
    int numChars;
    int burnIn;
    int optimizeInterval;
    double pruneCntPerc;
    double pruneLblCntPerc;
    double pruneMaxPerc;

    public boolean doComputeNewSemanticAugmentations() {
        return computeNewSemanticAugmentations;
    }

    boolean computeNewSemanticAugmentations;

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    String experimentId;

    public String getExperimentDetails() {
        return experimentDetails;
    }

    String
            experimentDetails;

    public String getInitModelFile() {
        return initModelFile;
    }

    public String getTagger() {
        return tagger;
    }

    String tagger;
    String initModelFile;

    public int getNumOfThreads() {
        return numOfThreads;
    }

    public int getNumChars() {
        return numChars;
    }

    public int getBurnIn() {
        return burnIn;
    }

    public int getOptimizeInterval() {
        return optimizeInterval;
    }

    public double getPruneCntPerc() {
        return pruneCntPerc;
    }

    public double getPruneLblCntPerc() {
        return pruneLblCntPerc;
    }

    public double getPruneMaxPerc() {
        return pruneMaxPerc;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public int getLimitDocs() {
        return limitDocs;
    }

    int limitDocs;


    String dictDir = "";

    public int getSeed() {
        return seed;
    }

    int seed;


    boolean ACMAuthorSimilarity;
    boolean calcTopicDistributionsAndTrends;
    boolean calcEntitySimilarities;
    boolean calcTopicSimilarities;
    boolean calcPPRSimilarities;

    public boolean isIgnoreText() {
        return ignoreText;
    }

    boolean ignoreText;
    boolean findKeyPhrases;

    String ModellingOutputDataSourceType;
    String ModellingOutputDataSourceParams;
    String ModellingInputDataSourceType;
    String ModellingInputDataSourceParams;
    String InferenceDataSourceType;
    String InferenceDataSourceParams;
    String InferenceOutputDataSourceType;
    String InferenceOutputDataSourceParams;
    String InferenceModelDataSourceType;
    String InferenceModelDataSourceParams;

    public DataIOConfig getSemanticAugmentationInput() {
        return SemanticAugmentationInput;
    }

    String SemanticAnnotatorType;
    public String getSemanticAnnotatorType(){
        return SemanticAnnotatorType;
    }


    public DataIOConfig getSemanticAugmentationOutput() {
        return SemanticAugmentationOutput;
    }

    DataIOConfig SemanticAugmentationInput = new DataIOConfig("semantic_augmentation_input");
    DataIOConfig SemanticAugmentationOutput = new DataIOConfig("semantic_augmentation_output");

    public String getInferenceModelDataSourceType() {
        return InferenceModelDataSourceType;
    }

    public String getInferenceModelDataSourceParams() {
        return InferenceModelDataSourceParams;
    }


    public String getInferenceOutputDataSourceParams(){ return InferenceOutputDataSourceParams;}
    public String getInferenceOutputDataSourceType(){ return InferenceOutputDataSourceType;}
    public String getInferenceDataSourceParams(){ return InferenceDataSourceParams;}
    public String getInferenceDataSourceType(){ return InferenceDataSourceType;}

    public String getModellingOutputDataSourceType() {
        return ModellingOutputDataSourceType;
    }

    public String getModellingOutputDataSourceParams() {
        return ModellingOutputDataSourceParams;
    }

    public String getModellingInputDataSourceType() {
        return ModellingInputDataSourceType;
    }

    public String getModellingInputDataSourceParams() {
        return ModellingInputDataSourceParams;
    }






    ExperimentType experimentType = ExperimentType.PubMed;
    Net2BoWType PPRenabled = Net2BoWType.OneWay;

    SimilarityType similarityType = SimilarityType.cos; //Cosine 1 jensenShannonDivergence 2 symmetric KLP

    public Config() {
    }

    public Config(String configPath) {
        this(configPath, null);
    }


    public Config(String configPath, Map<String, String> runtimeProp) {
        InputStream inputStream = null;
        try {
            Properties prop = new Properties();
            inputStream = getClass().getClassLoader().getResourceAsStream(configPath);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + configPath + "' not found in the classpath");
            }

            if (runtimeProp != null) {
                prop.putAll(runtimeProp);
            }

            // get the property value and print it out
            numTopics = Integer.parseInt(prop.getProperty("TopicsNumber", "400"));
            numTopWords = Integer.parseInt(prop.getProperty("TopWords", "20"));
            modalities = Arrays.asList(prop.getProperty("modalities", "text").split(","));
            numIterations = Integer.parseInt(prop.getProperty("Iterations", "800"));
            showTopicsInterval = Integer.parseInt(prop.getProperty("ShowTopicsInterval", "50"));
            numOfThreads = Integer.parseInt(prop.getProperty("NumOfThreads", "4"));
            numChars = Integer.parseInt(prop.getProperty("NumOfChars", "4000"));
            burnIn = Integer.parseInt(prop.getProperty("BurnIn", "50"));
            optimizeInterval = Integer.parseInt(prop.getProperty("OptimizeInterval", "50"));
            pruneCntPerc = Double.parseDouble(prop.getProperty("PruneCntPerc", "0.002"));
            pruneLblCntPerc = Double.parseDouble(prop.getProperty("PruneLblCntPerc", "0.002"));
            pruneMaxPerc = Double.parseDouble(prop.getProperty("PruneMaxPerc", "10"));


            try {
                seed = Integer.parseInt(prop.getProperty("seed"));
            }catch (NumberFormatException e){
                seed = 1337;
                Logger.getLogger(SciTopicFlow.LOGGERNAME).warn("No random seed supplied. Using " + seed);
            }
            computeNewSemanticAugmentations = Boolean.parseBoolean(prop.getProperty("computeSemanticAugmentations", "false"));

            ModellingInputDataSourceType = prop.getProperty("ModellingInputDataSourceType").trim();
            ModellingInputDataSourceParams = prop.getProperty("ModellingInputDataSourceParams").trim();
            ModellingOutputDataSourceType = prop.getProperty("ModellingOutputDataSourceType").trim();
            ModellingOutputDataSourceParams = prop.getProperty("ModellingOutputDataSourceParams").trim();

            InferenceDataSourceParams = prop.getProperty("InferenceDataSourceParams").trim();
            InferenceDataSourceType = prop.getProperty("InferenceDataSourceType").trim();
            InferenceOutputDataSourceParams = prop.getProperty("InferenceOutputDataSourceParams").trim();
            InferenceOutputDataSourceType = prop.getProperty("InferenceOutputDataSourceType").trim();
            InferenceModelDataSourceParams = prop.getProperty("InferenceModelDataSourceParams").trim();
            InferenceModelDataSourceType = prop.getProperty("InferenceModelDataSourceType").trim();

            // semantic augmentation parameters
            this.SemanticAugmentationInput.readParams(prop);
            this.SemanticAugmentationOutput.readParams(prop);
            SemanticAnnotatorType = prop.getProperty("SemanticAnnotatorType", "spotlight").trim();

            initModelFile = prop.getProperty("initModelFile", "");
            tagger = prop.getProperty("tagger", "openNLP");

            experimentId = prop.getProperty("ExperimentId", "");
            limitDocs = Integer.parseInt(prop.getProperty("limitDocs", "0"));

            findKeyPhrases = Boolean.parseBoolean(prop.getProperty("findKeyPhrases", "false"));
            ACMAuthorSimilarity = Boolean.parseBoolean(prop.getProperty("ACMAuthorSimilarity", "true"));
            ignoreText = Boolean.parseBoolean(prop.getProperty("ignoreText", "false"));

        } catch (Exception e) {
            Logger.getLogger("SciTopic").error("Exception in reading properties: " + e);
            System.exit(1);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                System.exit(-1);
            }
        }

    }

    public boolean isCalcTopicDistributionsAndTrends() {
        return calcTopicDistributionsAndTrends;
    }

    public boolean isCalcEntitySimilarities() {
        return calcEntitySimilarities;
    }

    public boolean isCalcTopicSimilarities() {
        return calcTopicSimilarities;
    }

    public boolean isCalcPPRSimilarities() {
        return calcPPRSimilarities;
    }

    public boolean isFindKeyPhrases() {
        return findKeyPhrases;
    }

    public ExperimentType getExperimentType() {
        return experimentType;
    }

    public Net2BoWType getPPRenabled() {
        return PPRenabled;
    }

    public SimilarityType getSimilarityType() {
        return similarityType;
    }

    public String getDictDir() {
        return dictDir;
    }

    public int getNumTopics() {
        return numTopics;
    }

    public boolean isACMAuthorSimilarity() {
        return ACMAuthorSimilarity;
    }

    /**
     * Build an experiment identifier string from the given parameters
     * @return
     */
    public String makeExperimentString (){

        if (! this.experimentId.isEmpty()) return experimentId;
        return experimentType.toString() + "_" + numTopics + "T_"
                + numIterations + "IT_" + numChars + "CHRs_" + modalities.size() + "M_" + ((limitDocs > 0) ? ("Lmt_" + limitDocs) : "") + PPRenabled.name();
    }
    public void makeExperimentDetails(){
        experimentDetails = String.format("Multi View Topic Modeling Analysis \n pruneMaxPerc:%.1f  pruneCntPerc:%.4f" +
                                " pruneLblCntPerc:%.4f burnIn:%d numOfThreads:%d similarityType:%s",
                this.pruneMaxPerc, pruneCntPerc, pruneLblCntPerc, burnIn, numOfThreads, similarityType.toString());
    }
    // returns an indentifier pertaining to the input
    public String getInputId(){
        return experimentType.toString() + "_" + numChars + "CHRs_" + modalities.size() + "M_" + ((limitDocs > 0) ? ("lim" + limitDocs) : "") + ".inputs";
    }

    public String getInferenceId(){
        return experimentType.toString() + "_" + numChars + "CHRs_" + modalities.size() + "M_" + ((limitDocs > 0) ? ("lim" + limitDocs) : "") + ".inferences";
    }

    public static void main(String[] args) {
        boolean t = true;
        for (byte m = t? (byte) 0: (byte) 1; m<4; m++) System.out.println(m);
    }
}