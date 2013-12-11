/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vse.fis.keg.entityclassifier.core;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import cz.vse.fis.keg.entityclassifier.core.conf.PropertiesLoader;
import cz.vse.fis.keg.entityclassifier.core.conf.Settings;
import cz.vse.fis.keg.entityclassifier.core.mongodb.MongoDBClient;
import cz.vse.fis.keg.entityclassifier.core.ontologymapper.DBpediaMapping;
import cz.vse.fis.keg.entityclassifier.core.ontologymapper.DBpediaOntologyManager;
import cz.vse.fis.keg.entityclassifier.core.ontologymapper.OntoRecord;
import cz.vse.fis.keg.entityclassifier.core.ontologymapper.YagoOntologyManager;
import cz.vse.fis.keg.entityclassifier.core.vao.Article;
import cz.vse.fis.keg.entityclassifier.core.vao.Entity;
import cz.vse.fis.keg.entityclassifier.core.vao.Hypernym;
import gate.Gate;
import gate.Document;
import gate.creole.ExecutionException;
import gate.util.GateException;
import gate.Factory;
import gate.Annotation;
import gate.AnnotationSet;
import gate.FeatureMap;
import gate.Node;
import gate.Corpus;
import gate.ProcessingResource;
//import gate.creole.SerialAnalyserController;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialController;
import gate.creole.SerialAnalyserController;
import gate.CreoleRegister;
import gate.util.InvalidOffsetException;
import java.util.Iterator;
import java.io.File;
import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Milan Dojčinovski 
 * <dojcinovski.milan (at) gmail.com> 
 * Twitter: @m1ci 
 * www: http://dojchinovski.mk
 * 
 */
public class THDInstance {
    
    private static String gateHome = null;
    private static String docAcquisitionPlugin = null;
    private static String pluginHome; 
    
    private String usedWikiCreoleName;// = "wikifetch.WikipediaPR";
    private String usedQueryHighlightCreoleName;
    private static final String QueryHighlightPRname = "QueryHighlightPR";
    private String JapeGrammarPath;
    
    private static String enEntityExtractionGrammar;
    private static String nlEntityExtractionGrammar;
    private static String deEntityExtractionGrammar;
    
    private static String enHypernymExtractionGrammar = null;
    private static String deHypernymExtractionGrammar = null;
    private static String nlHypernymExtractionGrammar = null;

    private static String nlTaggerBinary;
    private static String deTaggerBinary;
   
    private ProcessingResource JapePR;
    private SerialAnalyserController corpusAnnotationPipeline = null;
    private SerialController corpusAcquisitionPipeline = null;
    
    private static SerialAnalyserController enEntityExtractionPipeline = null;
    private static SerialAnalyserController nlEntityExtractionPipeline = null;
    private static SerialAnalyserController deEntityExtractionPipeline = null;    
    
    private static SerialAnalyserController hypernymExtractionPipelineEN = null;
    private static SerialAnalyserController hypernymExtractionPipelineDE = null;
    private static SerialAnalyserController hypernymExtractionPipelineNL = null;
        
    public static THDInstance thdInstance = null;
    private BasicDBObject queryObj;
        
    public static THDInstance getInstance(){
        
        if(thdInstance == null){
            thdInstance = new THDInstance();
            init();
            initGate();
            assamblePipelines();
                     
        }
        return thdInstance;
    }
    
    private static void assamblePipelines(){
        assambleEntityExtractionPipelineEN();
        assambleEntityExtractionPipelineDE();
        assambleEntityExtractionPipelineNL();
        assambleHypernymExtractionPipelineEN();
        assambleHypernymExtractionPipelineDE();
        assambleHypernymExtractionPipelineNL();
    }
    
    private DBObject resObj;
    
    public ArrayList<Hypernym> extractEntityHypernyms(String entity, String lang, String kb, String[] provenance) throws UnknownHostException{
        
        ArrayList<Hypernym> hypernymsList = new ArrayList<Hypernym>();
        ArrayList<Hypernym> thdHypernyms = new ArrayList<Hypernym>();
        ArrayList<Hypernym> dbpediaHypernyms = new ArrayList<Hypernym>();
        ArrayList<Hypernym> yagoHypernyms = new ArrayList<Hypernym>();
        
        switch (lang) {
            case "en":
                for(String prov : provenance) {
                    
                    if(prov.equals("thd")) {
                        
                        if(kb.equals("linkedHypernymsDataset")){
                            thdHypernyms = extractEntityHypernymsLHD(entity, lang, provenance);
                        }else{
                            thdHypernyms = extractEntityHypernymsEN(entity, kb);
                        }
                        
                    } else if(prov.equals("dbpedia")){
                        System.out.println("fetching dbpedia");
                        dbpediaHypernyms = getDBpediaHypernyms(entity, lang);
                        System.out.println(dbpediaHypernyms.size());
                    } else if(prov.equals("yago")){
                        System.out.println("fetching yago");
                        yagoHypernyms = getYAGOHypernyms(entity, lang);
                    }            
                }
                if(dbpediaHypernyms != null){
                    hypernymsList.addAll(dbpediaHypernyms);
                }

                if(yagoHypernyms != null){
                    hypernymsList.addAll(yagoHypernyms);
                }

                if(thdHypernyms != null) {
                    hypernymsList.addAll(thdHypernyms);
                }
                return hypernymsList;
            case "de":
                for(String prov : provenance){
                    if(prov.equals("thd")){
                        System.out.println("fetching thd");
                        thdHypernyms = extractEntityHypernymsDE(entity, kb);
                    } else if(prov.equals("dbpedia")){
                        System.out.println("fetching dbpedia");
                        dbpediaHypernyms = getDBpediaHypernyms(entity, lang);
                        System.out.println(dbpediaHypernyms.size());
                    } else if(prov.equals("yago")){
                        System.out.println("fetching yago");
                        yagoHypernyms = getYAGOHypernyms(entity, lang);
                    }            
                }
                if(dbpediaHypernyms != null){
                    hypernymsList.addAll(dbpediaHypernyms);
                }

                if(yagoHypernyms != null){
                    hypernymsList.addAll(yagoHypernyms);
                }

                if(thdHypernyms != null) {
                    hypernymsList.addAll(thdHypernyms);
                }
                return hypernymsList;
            case "nl":                
                for(String prov : provenance){
                    if(prov.equals("thd")){
                        System.out.println("fetching thd");
                        thdHypernyms = extractEntityHypernymsNL(entity, kb);
                    } else if(prov.equals("dbpedia")){
                        System.out.println("fetching dbpedia");
                        dbpediaHypernyms = getDBpediaHypernyms(entity, lang);
                        System.out.println(dbpediaHypernyms.size());
                    } else if(prov.equals("yago")){
                        System.out.println("fetching yago");
                        yagoHypernyms = getYAGOHypernyms(entity, lang);
                    }            
                }
                if(dbpediaHypernyms != null){
                    hypernymsList.addAll(dbpediaHypernyms);
                }

                if(yagoHypernyms != null){
                    hypernymsList.addAll(yagoHypernyms);
                }

                if(thdHypernyms != null) {
                    hypernymsList.addAll(thdHypernyms);
                }
                return hypernymsList;

        }
        return null;
    }
    
    public ArrayList<Hypernym> getDBpediaHypernyms(String entityTitle, String lang) throws UnknownHostException{
        
        ArrayList<Hypernym> hypernymsList = new ArrayList<Hypernym>();
        queryObj = new BasicDBObject();
        //System.out.println("Searching hypernym for entity: " + entityTitle);
        queryObj.append("label", entityTitle);
        queryObj.append("types", new BasicDBObject().append("$elemMatch", new BasicDBObject().append("origin", "dbpedia")));
        DBObject resObj;
        BasicDBList dbTypesList;
        switch (lang) {
            case "en":
                
                resObj = MongoDBClient.getDBInstance().getCollection("en_entities_dbpedia").findOne(queryObj);
                
                if(resObj != null){
                    
                    dbTypesList = (BasicDBList)resObj.get("types");

                    for(int i=0; i < dbTypesList.size(); i++){
                        DBObject obj = (DBObject)dbTypesList.get(i);

                        Hypernym hypernym = new Hypernym();
                        hypernym.setEntity(entityTitle);
                        hypernym.setEntityURL(resObj.get("uri").toString());
                        hypernym.setType(obj.get("label").toString());
                        hypernym.setTypeURL(obj.get("uri").toString());
                        hypernym.setOrigin("dbpedia");
                        //System.out.println("TEST: " + obj.get("uri").toString());
                        if(obj.get("uri").toString().contains("http://dbpedia.org/ontology/")){
                            hypernymsList.add(hypernym);
                            //System.out.println("DBpedia ontology");
                        }
                    }
                }
                return hypernymsList;
                
            case "de":
                resObj = MongoDBClient.getDBInstance().getCollection("de_entities_dbpedia").findOne(queryObj);
                
                if(resObj != null){
                    dbTypesList = (BasicDBList)resObj.get("types");

                    for(int i=0; i < dbTypesList.size(); i++){

                        DBObject obj = (DBObject)dbTypesList.get(i);

                        Hypernym hypernym = new Hypernym();
                        hypernym.setEntity(entityTitle);
                        hypernym.setEntityURL(resObj.get("uri").toString());
                        hypernym.setType(obj.get("label").toString());
                        hypernym.setTypeURL(obj.get("uri").toString());
                        hypernym.setOrigin("dbpedia");
                        //System.out.println("TEST: " + obj.get("uri").toString());
                        if(obj.get("uri").toString().contains("http://dbpedia.org/ontology/")){
                            hypernymsList.add(hypernym);
                            //System.out.println("DBpedia ontology");
                        }
                    }
                }
                
                return hypernymsList;                

            case "nl":
                resObj = MongoDBClient.getDBInstance().getCollection("nl_entities_dbpedia").findOne(queryObj);
                
                if(resObj != null){
                    dbTypesList = (BasicDBList)resObj.get("types");

                    for(int i=0; i < dbTypesList.size(); i++){

                        DBObject obj = (DBObject)dbTypesList.get(i);

                        Hypernym hypernym = new Hypernym();
                        hypernym.setEntity(entityTitle);
                        hypernym.setEntityURL(resObj.get("uri").toString());
                        hypernym.setType(obj.get("label").toString());
                        hypernym.setTypeURL(obj.get("uri").toString());
                        hypernym.setOrigin("dbpedia");
                        //System.out.println("TEST: " + obj.get("uri").toString());
                        
                        if(obj.get("uri").toString().contains("http://dbpedia.org/ontology/")){
                            hypernymsList.add(hypernym);
                            //System.out.println("DBpedia ontology");
                        }
                    }
                }
                
                return hypernymsList;
                
            default:
                //System.out.println("Not supported language!");
                return hypernymsList;
        }
    }
    
    public ArrayList<Hypernym> getYAGOHypernyms(String entityTitle, String lang) throws UnknownHostException{
        
        ArrayList<Hypernym> hypernymsList = new ArrayList<Hypernym>();
        
        switch(lang){

            case "en":

                queryObj = new BasicDBObject();
                queryObj.append("labels.label", entityTitle).append("labels.lang", "en");
                
                resObj = MongoDBClient.getDBInstance().getCollection("entities_yago").findOne(queryObj);
                
                if(resObj != null) {
                    
                    //System.out.println("before " + hypernymsList.size());
                    hypernymsList.addAll(YagoOntologyManager.getInstance().getYagoHypernyms(entityTitle, "http://yago-knowledge.org/resource/"+entityTitle.replaceAll(" ", "_"), "en", "yago"));
                    //System.out.println("after " + hypernymsList.size());
                }
                
                break;
                
            case "de":
                
                queryObj = new BasicDBObject();
                queryObj.append("labels.label", entityTitle).append("labels.lang", "de");
//                queryObj.append("uri", "http://yago-knowledge.org/resource/"+entityTitle.replaceAll(" ", "_"));
                
                resObj = MongoDBClient.getDBInstance().getCollection("entities_yago").findOne(queryObj);
                
                if(resObj != null){
                    
                    //System.out.println("before " + hypernymsList.size());
                    hypernymsList.addAll(YagoOntologyManager.getInstance().getYagoHypernyms(entityTitle, resObj.get("uri").toString(), "de", "yago"));
                    //System.out.println("after " + hypernymsList.size());
                }
                
                break;
                
            case "nl":
                
                break;
        }
        return hypernymsList;
    }
    
    public ArrayList<Hypernym> extractEntityHypernymsLHD(String entityTitle, String lang, String[] provs) throws UnknownHostException{
        
        ArrayList<Hypernym> hypernymsList = new ArrayList<Hypernym>();
        ArrayList<Hypernym> thdHypernyms = new ArrayList<Hypernym>();
        ArrayList<Hypernym> dbpediaHypernyms = new ArrayList<Hypernym>();
        ArrayList<Hypernym> yagoHypernyms = new ArrayList<Hypernym>();
        
        boolean thd_check = false;
        boolean yago_check = false;
        boolean dbpedia_check = false;
        
        for(String prov : provs){
            if(prov.equals("thd") && thd_check != true){
                thdHypernyms = getTHDHypernymsLHD(entityTitle, lang);
                
//                if(thdHypernyms == null) {
//                    if(!lang.equals("en")){
//                        System.out.println("Trying to find in english");
//                        thdHypernyms = getTHDHypernyms(entityTitle, "en");                    
//                    }
//                } else if((thdHypernyms.size() == 0) && (!lang.equals("en"))){
//                        System.out.println("Trying to find in english");
//                        thdHypernyms = getTHDHypernyms(entityTitle, "en");                                    
//                }
                thd_check = true;
            } else if(prov.equals("dbpedia") && dbpedia_check != true){
                dbpediaHypernyms = getDBpediaHypernyms(entityTitle, lang);
                dbpedia_check = true;
            }
            else if(prov.equals("yago") && yago_check != true){
                yagoHypernyms = getYAGOHypernyms(entityTitle, lang);
                yago_check = true;
            }            
        }
        
        if(dbpediaHypernyms != null){
            hypernymsList.addAll(dbpediaHypernyms);
            //System.out.println("DBpedia found: " + dbpediaHypernyms.size());
        }
        
        if(yagoHypernyms != null){
            hypernymsList.addAll(yagoHypernyms);
            //System.out.println("YAGO found: " + yagoHypernyms.size());
        }
        
        if(thdHypernyms != null) {
            hypernymsList.addAll(thdHypernyms);
            //System.out.println("THD found: " + thdHypernyms.size());
        }
        
        return hypernymsList;
    }
    
    public ArrayList<Hypernym> getTHDHypernymsLHD(String entityTitle, String lang) throws UnknownHostException{
        
        ArrayList<Hypernym> hypernymsList = new ArrayList<Hypernym>();
        BasicDBObject queryObj = new BasicDBObject();
        //System.out.println("Searching hypernym for entity: " + entityTitle);
        queryObj.append("label", entityTitle);
        queryObj.append("types", new BasicDBObject().append("$elemMatch", new BasicDBObject().append("origin", "thd")));
        
        switch (lang) {
            case "en":
                
                DBObject resObj = MongoDBClient.getDBInstance().getCollection("en_entities_thd").findOne(queryObj);                
                        //System.out.println(resObj);ß
                
                if(resObj != null){
                    
                    // deriving YAGO types
                    //System.out.println("before " + hypernymsList.size());
                    //hypernymsList.addAll(YagoOntologyManager.getInstance().getYagoHypernyms(entityTitle, "http://yago-knowledge.org/resource/"+entityTitle.replaceAll(" ", "_"), "en", "thd-derived"));
                    //System.out.println("after " + hypernymsList.size());
                    
                    //System.out.println(resObj);
                    BasicDBList e = (BasicDBList)resObj.get("types");
                    DBObject type = (DBObject)e.get(0);
                    //System.out.println("HYPENYM: " + type.get("label"));

                    Hypernym hypernym = new Hypernym();
                    hypernym.setEntity(entityTitle);
                    hypernym.setEntityURL(resObj.get("uri").toString());
                    hypernym.setType(type.get("label").toString());
                    hypernym.setTypeURL(type.get("uri").toString());
                    hypernym.setOrigin(type.get("origin").toString());
                    hypernym.setAccuracy(type.get("accuracy").toString());
                    hypernym.setBounds(type.get("bounds").toString());
                    hypernymsList.add(hypernym);
                    
                    BasicDBList typesList = (BasicDBList)resObj.get("types");
                    DBObject firstType = (DBObject)typesList.get(0);
                    
                    // hypernym is mapped to dbpedia ontology
                    // creating hierarchy from the dbpedia ontology
                    if(firstType.get("mapping").equals("dbOnto")){
                        
                        //System.out.println("dbOnto");

                        OntoRecord initRecord = new OntoRecord();
                        initRecord.setUri(firstType.get("uri").toString());
                        
                        while(initRecord != null){
                            
                            initRecord = DBpediaOntologyManager.getInstance().getSubclass(initRecord.getUri(), lang);
                            
                            if(initRecord != null){
                                
                                //System.out.println("YES: " + initRecord.getUri());
                                Hypernym hypernymDerived = new Hypernym();
                                hypernymDerived.setEntity(entityTitle);
                                hypernymDerived.setEntityURL(resObj.get("uri").toString());
                                hypernymDerived.setType(initRecord.getLabel());
                                hypernymDerived.setTypeURL(initRecord.getUri());
                                hypernymDerived.setOrigin("thd-derived");
                                hypernymDerived.setAccuracy(type.get("accuracy").toString());
                                hypernymDerived.setBounds(type.get("bounds").toString());
                                hypernymsList.add(hypernymDerived);
                            }
                        }
                    }
                    // hypernym is not mapped to dbpedia ontology
                    // searching for superclass mapped to dbpedia ontology
                    // if found, for the superclass is created hierarchy
                    else if(firstType.get("mapping").equals("dbRes")){
                        
//                        System.out.println("dbRes");
                        String initialUri = firstType.get("uri").toString();
//                        System.out.println("dbRes" + initialUri);
                        
                        boolean continueSearching = true;
                        
                        while(continueSearching) {
                            // try to find dbonto for dbres
                            DBpediaMapping mapping = getSubclassConfirmed(initialUri);
                            
                            // if not found, try to find dbres for the dbres
                            if(mapping == null){
                                
                                initialUri = getSuperclass(initialUri, "en");
                                
                                if(initialUri == null){
                                    //System.out.println("No superclass found, finishing searching");
                                    continueSearching = false;
                                    
                                } else {
                                    //System.out.println("Superclass found");
                                    // superClass uri found
                                    // check if uri is dbOnto - create hierarchy
                                    // check if uri is dbRes - continue
                                    
                                    if(initialUri.contains("/resource/")){
                                        //System.out.println("Found superclass is dbres");
                                        // do nothing
                                        // continue to search
                                    } else if(initialUri.contains("/ontology/")) {
                                        //System.out.println("Found superclass is dbonto, finishing searching and creating hierarchy");
                                        // create hierarchy
                                        continueSearching = false;
                                        
                                        OntoRecord initRecord = new OntoRecord();
                                        initRecord.setUri(initialUri);

                                        while(initRecord != null){

                                            initRecord = DBpediaOntologyManager.getInstance().getSubclass(initRecord.getUri(), lang);

                                            if(initRecord != null){

                                                //System.out.println("YES: " + initRecord.getUri());
                                                Hypernym hypernymDerived = new Hypernym();
                                                hypernymDerived.setEntity(entityTitle);
                                                hypernymDerived.setEntityURL(resObj.get("uri").toString());
                                                hypernymDerived.setType(initRecord.getLabel());
                                                hypernymDerived.setTypeURL(initRecord.getUri());
                                                hypernymDerived.setOrigin("thd-derived");
                                                hypernymDerived.setAccuracy(type.get("accuracy").toString());
                                                hypernymDerived.setBounds(type.get("bounds").toString());
                                                hypernymsList.add(hypernymDerived);

                                            }
                                        }
                                    } else {
                                        // some other uri
                                        //System.out.println("Problem 1234");
                                        continueSearching = false;
                                    }
                                }
                            }
                            // if found, then create hierarchy
                            else {
                                continueSearching = false;
                                // creating hierarchy
                                OntoRecord initRecord = new OntoRecord();
                                initRecord.setUri(initialUri);

                                while(initRecord != null){

                                    initRecord = DBpediaOntologyManager.getInstance().getSubclass(initRecord.getUri(), lang);

                                    if(initRecord != null){

                                        //System.out.println("YES: " + initRecord.getUri());
                                        Hypernym hypernymDerived = new Hypernym();
                                        hypernymDerived.setEntity(entityTitle);
                                        hypernymDerived.setEntityURL(resObj.get("uri").toString());
                                        hypernymDerived.setType(initRecord.getLabel());
                                        hypernymDerived.setTypeURL(initRecord.getUri());
                                        hypernymDerived.setOrigin("thd-derived");
                                        hypernymsList.add(hypernymDerived);
                                    }
                                }
                            }
                        }
                    }
                    
                    return hypernymsList;
                    
                } else {
                    //System.out.println("hypernym not found for " + entityTitle);
                    return null;
                }
            case "de":
                
                resObj = MongoDBClient.getDBInstance().getCollection("de_entities_thd").findOne(queryObj);                
                        //System.out.println(resObj);
                
                if(resObj != null){
                    
                    //System.out.println("before " + hypernymsList.size());
                    hypernymsList.addAll(YagoOntologyManager.getInstance().getYagoHypernyms(entityTitle, "http://yago-knowledge.org/resource/"+entityTitle.replaceAll(" ", "_"), "de", "thd-derived"));
                    //System.out.println("after " + hypernymsList.size());

                    //System.out.println(resObj);
                    BasicDBList e = (BasicDBList)resObj.get("types");
                    DBObject type = (DBObject)e.get(0);
                    //System.out.println("HYPENYM: " + type.get("label"));

                    Hypernym hypernym = new Hypernym();
                    hypernym.setEntity(entityTitle);
                    hypernym.setEntityURL(resObj.get("uri").toString());
                    hypernym.setType(type.get("label").toString());
                    hypernym.setTypeURL(type.get("uri").toString());
                    hypernym.setOrigin(type.get("origin").toString());
                    hypernymsList.add(hypernym);
                    
                    BasicDBList typesList = (BasicDBList)resObj.get("types");
                    DBObject firstType = (DBObject)typesList.get(0);
                    
                    // hypernym is mapped to dbpedia ontology
                    // creating hierarchy from the dbpedia ontology
                    if(firstType.get("mapping").equals("dbOnto")){
                        
                        //System.out.println("dbOnto");

                        OntoRecord initRecord = new OntoRecord();
                        initRecord.setUri(firstType.get("uri").toString());
                        
                        while(initRecord != null){
                            
                            initRecord = DBpediaOntologyManager.getInstance().getSubclass(initRecord.getUri(), lang);
                            
                            if(initRecord != null){
                                
                                //System.out.println("YES: " + initRecord.getUri());
                                Hypernym hypernymDerived = new Hypernym();
                                hypernymDerived.setEntity(entityTitle);
                                hypernymDerived.setEntityURL(resObj.get("uri").toString());
                                hypernymDerived.setType(initRecord.getLabel());
                                hypernymDerived.setTypeURL(initRecord.getUri());
                                hypernymDerived.setOrigin("thd-derived");
                                hypernymsList.add(hypernymDerived);                                
                            }
                        }
                    }
                    // hypernym is not mapped to dbpedia ontology
                    // searching for superclass mapped to dbpedia ontology
                    // if found, for the superclass is created hierarchy
                    else if(firstType.get("mapping").equals("dbRes")){
                        
                        //System.out.println("dbRes");
                        String initialUri = firstType.get("uri").toString();
                        
                        boolean continueSearching = true;
                        
                        while(continueSearching){
                            // try to find dbonto for dbres
                            DBpediaMapping mapping = getSubclassConfirmed(initialUri);
                            
                            // if not found, try to find dbres for the dbres
                            if(mapping == null){
                                
                                initialUri = getSuperclass(initialUri, "de");
                                
                                if(initialUri == null){
                                    //System.out.println("No superclass found, finishing searching");
                                    continueSearching = false;
                                    
                                } else {
                                    //System.out.println("Superclass found");
                                    // superClass uri found
                                    // check if uri is dbOnto - create hierarchy
                                    // check if uri is dbRes - continue
                                    
                                    if(initialUri.contains("/resource/")){
                                        //System.out.println("Found superclass is dbres");
                                        // do nothing
                                        // continue to search
                                    } else if(initialUri.contains("/ontology/")) {
                                        //System.out.println("Found superclass is dbonto, finishing searching and creating hierarchy");
                                        // create hierarchy
                                        continueSearching = false;
                                        
                                        OntoRecord initRecord = new OntoRecord();
                                        initRecord.setUri(initialUri);

                                        while(initRecord != null){

                                            initRecord = DBpediaOntologyManager.getInstance().getSubclass(initRecord.getUri(), lang);

                                            if(initRecord != null){

                                                //System.out.println("YES: " + initRecord.getUri());
                                                Hypernym hypernymDerived = new Hypernym();
                                                hypernymDerived.setEntity(entityTitle);
                                                hypernymDerived.setEntityURL(resObj.get("uri").toString());
                                                hypernymDerived.setType(initRecord.getLabel());
                                                hypernymDerived.setTypeURL(initRecord.getUri());
                                                hypernymDerived.setOrigin("thd-derived");
                                                hypernymsList.add(hypernymDerived);

                                            }
                                        }
                                    } else {
                                        // some other uri
                                        //System.out.println("Problem 1234");
                                        continueSearching = false;
                                    }
                                }
                            }
                            // if found, then create hierarchy
                            else {
                                continueSearching = false;
                                // creating hierarchy
                                OntoRecord initRecord = new OntoRecord();
                                initRecord.setUri(initialUri);

                                while(initRecord != null){

                                    initRecord = DBpediaOntologyManager.getInstance().getSubclass(initRecord.getUri(), lang);

                                    if(initRecord != null){

                                        //System.out.println("YES: " + initRecord.getUri());
                                        Hypernym hypernymDerived = new Hypernym();
                                        hypernymDerived.setEntity(entityTitle);
                                        hypernymDerived.setEntityURL(resObj.get("uri").toString());
                                        hypernymDerived.setType(initRecord.getLabel());
                                        hypernymDerived.setTypeURL(initRecord.getUri());
                                        hypernymDerived.setOrigin("thd-derived");
                                        hypernymsList.add(hypernymDerived);

                                    }
                                }
                            }
                        }
                    }
                    
                    return hypernymsList;
                    
                } else {
                    //System.out.println("hypernym not found for " + entityTitle);
                    return hypernymsList;
                }
                
            case "nl":
                
                resObj = MongoDBClient.getDBInstance().getCollection("nl_entities_thd").findOne(queryObj);                
                        //System.out.println(resObj);

                if(resObj != null){
                    
                    //System.out.println("before " + hypernymsList.size());
                    hypernymsList.addAll(YagoOntologyManager.getInstance().getYagoHypernyms(entityTitle, "http://yago-knowledge.org/resource/"+entityTitle.replaceAll(" ", "_"), "en", "thd-derived"));
                    //System.out.println("after " + hypernymsList.size());

                    //System.out.println(resObj);
                    BasicDBList e = (BasicDBList)resObj.get("types");
                    DBObject type = (DBObject)e.get(0);
                    //System.out.println("HYPENYM: " + type.get("label"));

                    Hypernym hypernym = new Hypernym();
                    hypernym.setEntity(entityTitle);
                    hypernym.setEntityURL(resObj.get("uri").toString());
                    hypernym.setType(type.get("label").toString());
                    hypernym.setTypeURL(type.get("uri").toString());
                    hypernym.setOrigin(type.get("origin").toString());
                    hypernymsList.add(hypernym);
                    
                    BasicDBList typesList = (BasicDBList)resObj.get("types");
                    DBObject firstType = (DBObject)typesList.get(0);
                    
                    // hypernym is mapped to dbpedia ontology
                    // creating hierarchy from the dbpedia ontology
                    if(firstType.get("mapping").equals("dbOnto")){
                        
                        //System.out.println("dbOnto");

                        OntoRecord initRecord = new OntoRecord();
                        initRecord.setUri(firstType.get("uri").toString());
                        
                        while(initRecord != null){
                            
                            initRecord = DBpediaOntologyManager.getInstance().getSubclass(initRecord.getUri(), lang);
                            
                            if(initRecord != null){
                                
                                //System.out.println("YES: " + initRecord.getUri());
                                Hypernym hypernymDerived = new Hypernym();
                                hypernymDerived.setEntity(entityTitle);
                                hypernymDerived.setEntityURL(resObj.get("uri").toString());
                                hypernymDerived.setType(initRecord.getLabel());
                                hypernymDerived.setTypeURL(initRecord.getUri());
                                hypernymDerived.setOrigin("thd-derived");
                                hypernymsList.add(hypernymDerived);                                
                            }
                        }
                    }
                    // hypernym is not mapped to dbpedia ontology
                    // searching for superclass mapped to dbpedia ontology
                    // if found, for the superclass is created hierarchy
                    else if(firstType.get("mapping").equals("dbRes")){
                        
                        //System.out.println("dbRes");
                        String initialUri = firstType.get("uri").toString();
                        
                        boolean continueSearching = true;
                        
                        while(continueSearching){
                            // try to find dbonto for dbres
                            DBpediaMapping mapping = getSubclassConfirmed(initialUri);
                            
                            // if not found, try to find dbres for the dbres
                            if(mapping == null){
                                
                                initialUri = getSuperclass(initialUri, "nl");
                                
                                if(initialUri == null){
                                    //System.out.println("No superclass found, finishing searching");
                                    continueSearching = false;
                                    
                                } else {
                                    
                                    //System.out.println("Superclass found");
                                    // superClass uri found
                                    // check if uri is dbOnto - create hierarchy
                                    // check if uri is dbRes - continue
                                    
                                    if(initialUri.contains("/resource/")){
                                        //System.out.println("Found superclass is dbres");
                                        // do nothing
                                        // continue to search
                                    } else if(initialUri.contains("/ontology/")) {
                                        //System.out.println("Found superclass is dbonto, finishing searching and creating hierarchy");
                                        // create hierarchy
                                        continueSearching = false;
                                        
                                        OntoRecord initRecord = new OntoRecord();
                                        initRecord.setUri(initialUri);

                                        while(initRecord != null){

                                            initRecord = DBpediaOntologyManager.getInstance().getSubclass(initRecord.getUri(), lang);

                                            if(initRecord != null){

                                                //System.out.println("YES: " + initRecord.getUri());
                                                Hypernym hypernymDerived = new Hypernym();
                                                hypernymDerived.setEntity(entityTitle);
                                                hypernymDerived.setEntityURL(resObj.get("uri").toString());
                                                hypernymDerived.setType(initRecord.getLabel());
                                                hypernymDerived.setTypeURL(initRecord.getUri());
                                                hypernymDerived.setOrigin("thd-derived");
                                                hypernymsList.add(hypernymDerived);

                                            }
                                        }
                                    } else {
                                        // some other uri
                                        continueSearching = false;
                                    }
                                }
                            }
                            // if found, then create hierarchy
                            else {
                                continueSearching = false;
                                // creating hierarchy
                                OntoRecord initRecord = new OntoRecord();
                                initRecord.setUri(initialUri);

                                while(initRecord != null){

                                    initRecord = DBpediaOntologyManager.getInstance().getSubclass(initRecord.getUri(), lang);

                                    if(initRecord != null){
                                        
                                        //System.out.println("YES: " + initRecord.getUri());
                                        Hypernym hypernymDerived = new Hypernym();
                                        hypernymDerived.setEntity(entityTitle);
                                        hypernymDerived.setEntityURL(resObj.get("uri").toString());
                                        hypernymDerived.setType(initRecord.getLabel());
                                        hypernymDerived.setTypeURL(initRecord.getUri());
                                        hypernymDerived.setOrigin("thd-derived");
                                        hypernymsList.add(hypernymDerived);
                                    }
                                }
                            }
                        }
                    }
                    
                    return hypernymsList;
                    
                } else {
                    //System.out.println("hypernym not found for " + entityTitle);
                    return hypernymsList;
                }
            default:
                //System.out.println("Not supported language!");
                return null;
        }
    }
    
    public DBpediaMapping getSubclassConfirmed(String res) throws UnknownHostException{
        
        BasicDBObject queryObj2 = new BasicDBObject();
        queryObj2.append("label", res);
        DBObject resObj2 = MongoDBClient.getDBInstance().getCollection("test_en_entities_superclass").findOne(queryObj2);
        
        if(resObj2 != null){
            //System.out.println(resObj);
            BasicDBList e = (BasicDBList)resObj.get("types");
            DBObject type = (DBObject)e.get(0);
            //System.out.println("Superclass for "+ res + " is " + type.get("label"));
            
            DBpediaMapping result = new DBpediaMapping();
            result.setLabel(type.get("label").toString());
            result.setUri(type.get("uri").toString());
            
            return result;
        } else {
            return null;
        }        
    }
    
    public String getSuperclass(String uri, String lang) throws UnknownHostException{
        
        //System.out.println("Searching superclass for " + uri);
        BasicDBObject tempQueryObj = new BasicDBObject();
        tempQueryObj.append("uri", uri);
        tempQueryObj.append("types", new BasicDBObject().append("$elemMatch", new BasicDBObject().append("origin", "thd")));
        DBObject tempResObj = null;
        
        switch (lang) {
            case "en":
                tempResObj = MongoDBClient.getDBInstance().getCollection("test_en_entities").findOne(tempQueryObj);
            case "de":
                tempResObj = MongoDBClient.getDBInstance().getCollection("test_de_entities").findOne(tempQueryObj);           
        }
        
        if(tempResObj != null){
            BasicDBList typesList = (BasicDBList)tempResObj.get("types");
            DBObject tmpTypeObj = (DBObject) typesList.get(0);
            return tmpTypeObj.get("uri").toString();
        } else {
            return null;
        }
        
    }
    
    public ArrayList<Hypernym> extractEntityHypernymsEN(String entity, String kb) {
                
        ArrayList<Hypernym> hypernymsList = new ArrayList<Hypernym>();
        try {
            URL url = null;
            String path = "";
            if(kb.equals("local")){
                path = Settings.EN_WIKIPEDIA_LOCAL_EXPORT + entity.replace(" ", "_");
            }else if(kb.equals("live")){
                path = Settings.EN_WIKIPEDIA_LIVE_EXPORT + entity.replace(" ", "_");            
            }
            System.out.println(path);
            StringBuffer buffer = new StringBuffer();
            url = new URL(path);
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();
            Reader isr = new InputStreamReader(is,"UTF-8");
            Reader in = new BufferedReader(isr);
            int ch;
            
            while ((ch = in.read()) > -1) {
                buffer.append((char) ch);
            }
            in.close();
            Article article = new Article(buffer.toString(),"en");
            //System.out.println(article.getFirstSection());
            Document document = Factory.newDocument(article.getFirstSection());
            Corpus corpus = Factory.newCorpus("");
            corpus.add(document);
            hypernymExtractionPipelineEN.setCorpus(corpus);
            hypernymExtractionPipelineEN.execute();
            //System.out.println("FINITOOOOOO");
            
            Iterator corpus_iter = corpus.iterator();
            
            while (corpus_iter.hasNext()) {
                Document doc = (Document)corpus_iter.next();
                AnnotationSet as_all = doc.getAnnotations();
                AnnotationSet as_hearst = as_all.get("h");
                Iterator ann_iter = as_hearst.iterator();
                //System.out.println(as_hearst.size());
                
                while (ann_iter.hasNext()) {
                    Annotation isaAnnot = (gate.Annotation) ann_iter.next();
                    Node isaStart = isaAnnot.getStartNode();                               
                    Node isaEnd = isaAnnot.getEndNode();
                    String hypernym = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    //System.out.println("HYPERNYM " + hypernym);
                    
                    Hypernym hypObj = new Hypernym();
                    hypObj.setEntity(entity);
                    hypObj.setEntityURL("http://dbpedia.org/resource/" + entity.replace(" ", "_"));
                    hypObj.setType(hypernym.substring(0, 1).toUpperCase() + hypernym.substring(1));
                    hypObj.setTypeURL("http://dbpedia.org/resource/" + hypernym.replace(" ", "_"));
                    hypObj.setOrigin("thd");
                    hypObj.setAccuracy("0.85");
                    hypObj.setBounds("+- 2.5%");
                    hypernymsList.add(hypObj);
//                    return hypObj;
                }
            }
            
        } catch (InvalidOffsetException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ResourceInstantiationException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hypernymsList;
    }
    
    public ArrayList<Hypernym> extractEntityHypernymsDE(String entity, String kb) {
        
        ArrayList<Hypernym> hypernymsList = new ArrayList<Hypernym>();
        try {
            URL url = null;
            String path = "";
            if(kb.equals("local")){
                path = Settings.DE_WIKIPEDIA_LOCAL_EXPORT + entity.replace(" ", "_");
            }else if(kb.equals("live")){
                path = Settings.DE_WIKIPEDIA_LIVE_EXPORT+entity.replace(" ", "_");            
            }
            System.out.println(path);

            StringBuffer buffer = new StringBuffer();
            url = new URL(path);
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();
            Reader isr = new InputStreamReader(is,"UTF-8");
            Reader in = new BufferedReader(isr);
            int ch;
            
            while ((ch = in.read()) > -1) {
                buffer.append((char) ch);
            }
            
            in.close();
            Article article = new Article(buffer.toString(),"en");
            //System.out.println(article.getFirstSection());
            Document document = Factory.newDocument(article.getFirstSection());
            Corpus corpus = Factory.newCorpus("");
            corpus.add(document);
                        
            hypernymExtractionPipelineDE.setCorpus(corpus);
            hypernymExtractionPipelineDE.execute();

            //System.out.println("FINITOOOOOO");
            
            Iterator corpus_iter = corpus.iterator();
            
            while (corpus_iter.hasNext()) {
                Document doc = (Document)corpus_iter.next();
                AnnotationSet as_all = doc.getAnnotations();
                AnnotationSet as_hearst = as_all.get("h");
                Iterator ann_iter = as_hearst.iterator();
                //System.out.println(as_hearst.size());
                while (ann_iter.hasNext()) {
                    Annotation isaAnnot = (gate.Annotation) ann_iter.next();
                    Node isaStart = isaAnnot.getStartNode();                               
                    Node isaEnd = isaAnnot.getEndNode();
                    String hypernym = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    //System.out.println("HYPERNYM " + hypernym);
                    
                    Hypernym hypObj = new Hypernym();
                    hypObj.setEntity(entity);
                    hypObj.setEntityURL("http://de.dbpedia.org/resource/" + entity.replace(" ", "_"));
                    hypObj.setType(hypernym.substring(0, 1).toUpperCase() + hypernym.substring(1));
                    hypObj.setTypeURL("http://de.dbpedia.org/resource/" + hypernym.replace(" ", "_"));
                    hypObj.setOrigin("thd");
                    hypObj.setAccuracy("0.77");
                    hypObj.setBounds("+- 2.5%");
                   hypernymsList.add(hypObj);
//                    return hypObj;
                }
            }
            
        } catch (InvalidOffsetException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ResourceInstantiationException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hypernymsList;
    }

    public ArrayList<Hypernym> extractEntityHypernymsNL(String entity, String kb) {
        ArrayList<Hypernym> hypernymsList = new ArrayList<Hypernym>();
        try {
            URL url = null;
            String path = "";
            if(kb.equals("local")){
                path = Settings.NL_WIKIPEDIA_LOCAL_EXPORT + entity.replace(" ", "_");
            }else if(kb.equals("live")){
                path = Settings.NL_WIKIPEDIA_LIVE_EXPORT+entity.replace(" ", "_");            
            }
            System.out.println(path);
            StringBuffer buffer = new StringBuffer();
            url = new URL(path);
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();
            Reader isr = new InputStreamReader(is,"UTF-8");
            Reader in = new BufferedReader(isr);
            int ch;
            
            while ((ch = in.read()) > -1) {
                buffer.append((char) ch);
            }
            in.close();
            Article article = new Article(buffer.toString(),"en");
            //System.out.println(article.getFirstSection());
            Document document = Factory.newDocument(article.getFirstSection());
            Corpus corpus = Factory.newCorpus("");
            corpus.add(document);
            hypernymExtractionPipelineNL.setCorpus(corpus);
            hypernymExtractionPipelineNL.execute();
            
            Iterator corpus_iter = corpus.iterator();
            
            while (corpus_iter.hasNext()) {
                Document doc = (Document)corpus_iter.next();
                AnnotationSet as_all = doc.getAnnotations();
                AnnotationSet as_hearst = as_all.get("h");
                Iterator ann_iter = as_hearst.iterator();
                //System.out.println(as_hearst.size());
                while (ann_iter.hasNext()) {
                    Annotation isaAnnot = (gate.Annotation) ann_iter.next();
                    Node isaStart = isaAnnot.getStartNode();                               
                    Node isaEnd = isaAnnot.getEndNode();
                    String hypernym = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    //System.out.println("HYPERNYM " + hypernym);
                    
                    Hypernym hypObj = new Hypernym();
                    hypObj.setEntity(entity);
                    hypObj.setEntityURL("http://nl.dbpedia.org/resource/" + entity.replace(" ", "_"));
                    hypObj.setType(hypernym.substring(0, 1).toUpperCase() + hypernym.substring(1));
                    hypObj.setTypeURL("http://nl.dbpedia.org/resource/" + hypernym.replace(" ", "_"));
                    hypObj.setOrigin("thd");
                    hypObj.setAccuracy("0.89");
                    hypObj.setBounds("+- 2%");
                    hypernymsList.add(hypObj);
//                    return hypObj;
                }
            }
            
        } catch (InvalidOffsetException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ResourceInstantiationException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hypernymsList;
    }
    
    private static void assambleHypernymExtractionPipelineEN(){
        
        try {
            
            hypernymExtractionPipelineEN = (SerialAnalyserController) Factory.createResource("gate.creole.RealtimeCorpusController");
            
            FeatureMap resetFeatureMap = Factory.newFeatureMap();
            ProcessingResource resetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR", resetFeatureMap);
            hypernymExtractionPipelineEN.add(resetPR);
        
            FeatureMap tokenizerFeatureMap = Factory.newFeatureMap();
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", tokenizerFeatureMap);
            hypernymExtractionPipelineEN.add(tokenizerPR);
                
            FeatureMap sentenceSplitterFeatureMap = Factory.newFeatureMap();
            ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter", sentenceSplitterFeatureMap);
            //ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", sentenceSplitterFeatureMap);
            hypernymExtractionPipelineEN.add(sentenceSplitterPR);

            FeatureMap posTaggerFeatureMap = Factory.newFeatureMap();
            ProcessingResource posTaggerPR = (ProcessingResource) Factory.createResource("gate.creole.POSTagger", posTaggerFeatureMap);                
            hypernymExtractionPipelineEN.add(posTaggerPR);

            URL url = thdInstance.getClass().getResource(enHypernymExtractionGrammar);
            File japeOrigFile = new File(url.getFile());            
            java.net.URI japeURI = japeOrigFile.toURI();
            
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try {
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                // TODO
            }
            ProcessingResource japeCandidatesPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            hypernymExtractionPipelineEN.add(japeCandidatesPR);
            
        } catch (ResourceInstantiationException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void assambleHypernymExtractionPipelineDE(){
        
        try {
            
            hypernymExtractionPipelineDE = (SerialAnalyserController) Factory.createResource("gate.creole.RealtimeCorpusController");
            
            FeatureMap resetFeatureMap = Factory.newFeatureMap();
            ProcessingResource resetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR", resetFeatureMap);
            hypernymExtractionPipelineDE.add(resetPR);
        
            FeatureMap tokenizerFeatureMap = Factory.newFeatureMap();
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", tokenizerFeatureMap);
            hypernymExtractionPipelineDE.add(tokenizerPR);
                
            FeatureMap sentenceSplitterFeatureMap = Factory.newFeatureMap();
            ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter", sentenceSplitterFeatureMap);
            //ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", sentenceSplitterFeatureMap);
            hypernymExtractionPipelineDE.add(sentenceSplitterPR);

            FeatureMap taggerFeatureMap = Factory.newFeatureMap();
            taggerFeatureMap.put("debug", "false");
            taggerFeatureMap.put("encoding", "ISO-8859-1");
            taggerFeatureMap.put("failOnUnmappableCharacter", "false");
            taggerFeatureMap.put("featureMapping", "lemma=3;category=2;string=1");
            taggerFeatureMap.put("inputAnnotationType", "Token");
            taggerFeatureMap.put("inputTemplate", "${string}");
            taggerFeatureMap.put("outputAnnotationType", "Token");
            taggerFeatureMap.put("regex", "(.+)	(.+)	(.+)");
            taggerFeatureMap.put("taggerBinary", deTaggerBinary);
            taggerFeatureMap.put("updateAnnotations", false);
                    
            ProcessingResource genTag = (ProcessingResource) Factory.createResource("gate.taggerframework.GenericTagger",taggerFeatureMap);
            hypernymExtractionPipelineDE.add(genTag);

            URL url = thdInstance.getClass().getResource(deHypernymExtractionGrammar);
            File japeOrigFile = new File(url.getFile());            
            java.net.URI japeURI = japeOrigFile.toURI();
            
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try {
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                //System.out.println("Malformed URL of JAPE grammar");
                System.out.println("PROBLEM 1 " + e.toString());
            }
            
            ProcessingResource japeCandidatesPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);
            hypernymExtractionPipelineDE.add(japeCandidatesPR);
            
        } catch (ResourceInstantiationException ex) {
            System.out.println("PROBLEM 2 " + ex.toString());
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private static void assambleHypernymExtractionPipelineNL(){
        
        try {
            
            hypernymExtractionPipelineNL = (SerialAnalyserController) Factory.createResource("gate.creole.RealtimeCorpusController");
            
            FeatureMap resetFeatureMap = Factory.newFeatureMap();
            ProcessingResource resetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR", resetFeatureMap);
            hypernymExtractionPipelineNL.add(resetPR);
        
            FeatureMap tokenizerFeatureMap = Factory.newFeatureMap();
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", tokenizerFeatureMap);
            hypernymExtractionPipelineNL.add(tokenizerPR);
                
            FeatureMap sentenceSplitterFeatureMap = Factory.newFeatureMap();
            ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter", sentenceSplitterFeatureMap);
            //ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", sentenceSplitterFeatureMap);
            hypernymExtractionPipelineNL.add(sentenceSplitterPR);

            FeatureMap taggerFeatureMap = Factory.newFeatureMap();
            taggerFeatureMap.put("debug", "false");
            taggerFeatureMap.put("encoding", "ISO-8859-1");
            taggerFeatureMap.put("failOnUnmappableCharacter", "false");
            taggerFeatureMap.put("featureMapping", "lemma=3;category=2;string=1");
            taggerFeatureMap.put("inputAnnotationType", "Token");
            taggerFeatureMap.put("inputTemplate", "${string}");
            taggerFeatureMap.put("outputAnnotationType", "Token");
            taggerFeatureMap.put("regex", "(.+)	(.+)	(.+)");
            taggerFeatureMap.put("taggerBinary", nlTaggerBinary);
            taggerFeatureMap.put("updateAnnotations", false);
                    
            ProcessingResource genTag = (ProcessingResource) Factory.createResource("gate.taggerframework.GenericTagger",taggerFeatureMap);
            hypernymExtractionPipelineNL.add(genTag);            

            URL url = thdInstance.getClass().getResource(nlHypernymExtractionGrammar);
            File japeOrigFile = new File(url.getFile());            
            java.net.URI japeURI = japeOrigFile.toURI();
            
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try {
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                //System.out.println("Malformed URL of JAPE grammar");
                //System.out.println(e.toString());
            }
            ProcessingResource japeCandidatesPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            hypernymExtractionPipelineNL.add(japeCandidatesPR);
            
        } catch (ResourceInstantiationException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private static void assambleEntityExtractionPipelineEN(){
        try {
            //System.out.println("Initialization of English Entity extraction pipeline...");
            //System.out.println(thdInstance.getClass().getResource(enEntityExtractionGrammar));
            URL url = thdInstance.getClass().getResource(enEntityExtractionGrammar);
            File japeOrigFile = new File(url.getFile());
            java.net.URI japeURI = japeOrigFile.toURI();
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();

            try {
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                //System.out.println("Malformed URL of JAPE grammar");
                //System.out.println(e.toString());
            }

            FeatureMap tokenizerFeatureMap = Factory.newFeatureMap();        
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", tokenizerFeatureMap);

            FeatureMap sentenceSplitterFeatureMap = Factory.newFeatureMap();
            ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter", sentenceSplitterFeatureMap);

            FeatureMap posTaggerFeatureMap = Factory.newFeatureMap();
            ProcessingResource posTaggerPR = (ProcessingResource) Factory.createResource("gate.creole.POSTagger", posTaggerFeatureMap);

            ProcessingResource japeCandidatesPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            enEntityExtractionPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

            enEntityExtractionPipeline.add(tokenizerPR);
            enEntityExtractionPipeline.add(sentenceSplitterPR);
            enEntityExtractionPipeline.add(posTaggerPR);
            enEntityExtractionPipeline.add(japeCandidatesPR);
        } catch (ResourceInstantiationException ex) {;
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }
    
    private static void assambleEntityExtractionPipelineDE(){
        try {
                    
            //System.out.println("Initializing DE pipeline");

            FeatureMap resetFeatureMap = Factory.newFeatureMap();
            ProcessingResource resetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR", resetFeatureMap);
                    
            FeatureMap tokenizerFeatureMap = Factory.newFeatureMap();
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", tokenizerFeatureMap);
                    
            FeatureMap taggerFeatureMap = Factory.newFeatureMap();
            taggerFeatureMap.put("debug", "false");
            taggerFeatureMap.put("encoding", "ISO-8859-1");
            taggerFeatureMap.put("failOnUnmappableCharacter", "false");
            taggerFeatureMap.put("featureMapping", "lemma=3;category=2;string=1");
            taggerFeatureMap.put("inputAnnotationType", "Token");
            taggerFeatureMap.put("inputTemplate", "${string}");
            taggerFeatureMap.put("outputAnnotationType", "Token");
            taggerFeatureMap.put("regex", "(.+)	(.+)	(.+)");
            taggerFeatureMap.put("taggerBinary", deTaggerBinary);
            taggerFeatureMap.put("updateAnnotations", false);
                    
            ProcessingResource genTag = (ProcessingResource) Factory.createResource("gate.taggerframework.GenericTagger",taggerFeatureMap);
                    
            //JAPE Entity Extraction grammar
            URL url =  thdInstance.getClass().getResource(deEntityExtractionGrammar);
            
            //System.out.println(url);
            File japeOrigFile = new File(url.getFile());
            java.net.URI japeURI = japeOrigFile.toURI();
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();

            try {
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                //System.out.println("Malformed URL of JAPE grammar");
                //System.out.println(e.toString());
            }
                    
            ProcessingResource japeCandidatesPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            deEntityExtractionPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.RealtimeCorpusController");
            deEntityExtractionPipeline.add(resetPR);
            deEntityExtractionPipeline.add(tokenizerPR);
            deEntityExtractionPipeline.add(genTag);                    
            deEntityExtractionPipeline.add(japeCandidatesPR);
        } catch (ResourceInstantiationException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }
    
    private static void assambleEntityExtractionPipelineNL(){
        
        try {
        
            //System.out.println("First time running NL pipeline");
            //System.out.println(nlEntityExtractionGrammar);

            FeatureMap resetFeatureMap = Factory.newFeatureMap();
            ProcessingResource resetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR", resetFeatureMap);
                    
            FeatureMap tokenizerFeatureMap = Factory.newFeatureMap();
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", tokenizerFeatureMap);
                    
            FeatureMap taggerFeatureMap = Factory.newFeatureMap();
            taggerFeatureMap.put("debug", "false");
            taggerFeatureMap.put("encoding", "ISO-8859-1");
            taggerFeatureMap.put("failOnUnmappableCharacter", "false");
            taggerFeatureMap.put("featureMapping", "lemma=3;category=2;string=1");
            taggerFeatureMap.put("inputAnnotationType", "Token");
            taggerFeatureMap.put("inputTemplate", "${string}");
            taggerFeatureMap.put("outputAnnotationType", "Token");
            taggerFeatureMap.put("regex", "(.+)	(.+)	(.+)");
            taggerFeatureMap.put("taggerBinary", nlTaggerBinary);
            taggerFeatureMap.put("updateAnnotations", false);
                    
            ProcessingResource genTag = (ProcessingResource) Factory.createResource("gate.taggerframework.GenericTagger",taggerFeatureMap);
                    
            //JAPE Entity Extraction grammar
            URL url =  thdInstance.getClass().getResource(nlEntityExtractionGrammar);
            File japeOrigFile = new File(url.getFile());
            java.net.URI japeURI = japeOrigFile.toURI();
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();

            try {
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                //System.out.println("Malformed URL of JAPE grammar");
                //System.out.println(e.toString());
            }
                    
            ProcessingResource japeCandidatesPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            nlEntityExtractionPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.RealtimeCorpusController");
            nlEntityExtractionPipeline.add(resetPR);
            nlEntityExtractionPipeline.add(tokenizerPR);
            nlEntityExtractionPipeline.add(genTag);                    
            nlEntityExtractionPipeline.add(japeCandidatesPR);
        } catch (ResourceInstantiationException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    
    private static void init() {
        Properties prop = new PropertiesLoader().getProperties();
        readSettings(prop);
    }
         
    private static void readSettings(Properties prop){
        gateHome = prop.getProperty("gateHome"); 
        docAcquisitionPlugin = prop.getProperty("docAcquisitionPlugin"); 
        pluginHome = prop.getProperty("pluginHome");
        
        enEntityExtractionGrammar = prop.getProperty("enEntityExtractionGrammar");
        nlEntityExtractionGrammar = prop.getProperty("nlEntityExtractionGrammar");
        deEntityExtractionGrammar = prop.getProperty("deEntityExtractionGrammar");

        enHypernymExtractionGrammar = prop.getProperty("enHypernymExtractionGrammar");
        deHypernymExtractionGrammar = prop.getProperty("deHypernymExtractionGrammar");
        nlHypernymExtractionGrammar = prop.getProperty("nlHypernymExtractionGrammar");
        
        nlTaggerBinary = prop.getProperty("nlTaggerBinary");
        deTaggerBinary = prop.getProperty("deTaggerBinary");
        
        DBpediaOntologyManager.setDbpediaOntologyFileLocation(prop.getProperty("dbpediaOntologyFileLocation"));
        YagoOntologyManager.setYagoOntologyFileLocation(prop.getProperty("yagoOntologyFileLocation"));
        
    }
    
    private static void initGate(){
        
        File gateHomeFile = new File(gateHome);
        Gate.setGateHome(gateHomeFile);
        File pluginsHome = new File(pluginHome);
        Gate.setPluginsHome(pluginsHome);
        Gate.setUserConfigFile(new File(gateHome, "user-gate.xml"));    
        URL annieHome = null;
        URL taggerHome = null;
        try {
            annieHome = new File(pluginsHome, "ANNIE").toURL();
            taggerHome = new File(pluginsHome, "Tagger_Framework").toURL();
        } catch (java.net.MalformedURLException e) {
            //System.out.println(e.toString());            
        }
        try {
            Gate.init();
            CreoleRegister register = Gate.getCreoleRegister();
            register.registerDirectories(annieHome);
            register.registerDirectories(taggerHome);
        } catch (GateException ge) {
            //SimpleDebug.printDebug(ge.toString());
        }        
    }
     
    public THDInstance(){
        // THD constructor
    }
    
    public ArrayList<Entity> extractEntityCandidates(String query, String lang, String entity_type) throws ResourceInstantiationException, ExecutionException, InvalidOffsetException{
        
        ArrayList<Entity> candidates = null;
        switch (lang) {
            case "en":
                candidates = extractEntityCandidatesEN(query, lang, entity_type);
                break;
            case "de":
                candidates = extractEntityCandidatesDE(query, lang, entity_type);
                break;
            case "nl":
                candidates = extractEntityCandidatesNL(query, lang, entity_type);
                break;
        }

        return candidates;
    }
    
    public ArrayList<Entity> extractEntityCandidatesDE(String query, String lang, String entity_type){
        System.out.println("Find page for: " + query);
        try {
            Document doc = Factory.newDocument(query);
            doc.setName("Query_Document");
            Corpus corpus = Factory.newCorpus("");
            corpus.add(doc);
            
            deEntityExtractionPipeline.setCorpus(corpus);        
            deEntityExtractionPipeline.execute();
            
            Document[] docs = (Document[]) corpus.toArray(new Document[corpus.size()]);
            //System.out.println("DE Entity Extraction pipeline finished");
            ArrayList<Entity> candidates = new ArrayList<Entity>();
            
            Document d = docs[0];
            AnnotationSet as_all = d.getAnnotations();
            if(entity_type.equals("all") || entity_type.equals("ne")){
                //System.out.println("Found AS: "+ as_all.size());
                AnnotationSet as_entity = as_all.get("ne");

                Iterator anot = as_entity.iterator();
                while(anot.hasNext()){
                    try {
                        Annotation isaAnnot = (gate.Annotation) anot.next();
                        Node annStart = isaAnnot.getStartNode();
                        Node annEnd = isaAnnot.getEndNode();
                        AnnotationSet as_token = as_all.get("Token",annStart.getOffset(), annEnd.getOffset() );
                        String content = "";
                        System.out.println("size: " + as_token.size());
                        
                        if (as_token.size()>1 ) {
                            content = d.getContent().getContent(annStart.getOffset(), annEnd.getOffset()).toString();                    
    //                        System.out.println("lemma: " + tok.getFeatures().get("lemma"));
                        }else{
                            Iterator as_token_iter = as_token.iterator();
                            
                            Annotation tok = (gate.Annotation)as_token_iter.next();
                            String lemma = tok.getFeatures().get("lemma").toString();
                                
                            if(!lemma.equals("<unknown>")) {
                                content = lemma;                                
                            } else {
                                content = d.getContent().getContent(annStart.getOffset(), annEnd.getOffset()).toString();                                
                            }
                        }
                        candidates.add(new Entity(d.getContent().getContent(annStart.getOffset(), annEnd.getOffset()).toString(), annStart.getOffset(), annEnd.getOffset(), "named entity"));
                    } catch (InvalidOffsetException ex) {
                        Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if(entity_type.equals("all") || entity_type.equals("ce")){
                //System.out.println("Chosen all entities");
                AnnotationSet as_common_entity = as_all.get("e");
                Iterator anot_e = as_common_entity.iterator();
                
                while(anot_e.hasNext()){
                    try {
                        Annotation isaAnnot = (gate.Annotation) anot_e.next();
                        Node annStart = isaAnnot.getStartNode();
                        Node annEnd = isaAnnot.getEndNode();
                        ////System.out.println("Start offset"+ annStart.getOffset());
                        ////System.out.println("End offset"+ annEnd.getOffset());
                        AnnotationSet as_token = as_all.get("Token",annStart.getOffset(), annEnd.getOffset() );
                        
                        String content = "";
                        System.out.println("size: " + as_token.size());
                        
                        if (as_token.size()>1 ) {
                            content = d.getContent().getContent(annStart.getOffset(), annEnd.getOffset()).toString();                    
    //                        System.out.println("lemma: " + tok.getFeatures().get("lemma"));
                        }else{
                            Iterator as_token_iter = as_token.iterator();
                            
                            Annotation tok = (gate.Annotation)as_token_iter.next();
                            String lemma = tok.getFeatures().get("lemma").toString();
                                
                            if(!lemma.equals("<unknown>")) {
                                content = lemma;                                
                            } else {
                                content = d.getContent().getContent(annStart.getOffset(), annEnd.getOffset()).toString();                                
                            }
                        }
                        //System.out.println("Candidate: " + content);
                        candidates.add(new Entity(d.getContent().getContent(annStart.getOffset(), annEnd.getOffset()).toString(), annStart.getOffset(), annEnd.getOffset(), "common entity"));
                    } catch (InvalidOffsetException ex) {
                        //System.out.println("Problem in the loop 2");
                       Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            //System.out.println("Number of extracted candidates: "+candidates.size());
            return candidates;
            
        } catch (ExecutionException ex) {
            //System.out.println("Problem 1"+ex.getMessage());
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ResourceInstantiationException ex) {
            //System.out.println("Problem 2"+ex.getMessage());
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public ArrayList<Entity> extractEntityCandidatesNL(String query, String lang, String entity_type){
        
        try {
            Document doc = Factory.newDocument(query);
            doc.setName("Query_Document");
            Corpus corpus = Factory.newCorpus("");
            corpus.add(doc);
            
            nlEntityExtractionPipeline.setCorpus(corpus);        
            nlEntityExtractionPipeline.execute();
            
            Document[] docs = (Document[]) corpus.toArray(new Document[corpus.size()]);
            //System.out.println("NL Entity Extraction pipeline finished");
            ArrayList<Entity> candidates = new ArrayList<Entity>();
            
            Document d = docs[0];
            AnnotationSet as_all = d.getAnnotations();
            
            //System.out.println("Found AS: "+ as_all.size());
            if(entity_type.equals("all") || entity_type.equals("ne")) {
                
                AnnotationSet as_entity = as_all.get("ne");
                Iterator anot = as_entity.iterator();
                while(anot.hasNext()){
                    try {
                        Annotation isaAnnot = (gate.Annotation) anot.next();
                        Node annStart = isaAnnot.getStartNode();
                        Node annEnd = isaAnnot.getEndNode();
                        ////System.out.println("Start offset"+ annStart.getOffset());
                        ////System.out.println("End offset"+ annEnd.getOffset());
                        String content = d.getContent().getContent(annStart.getOffset(), annEnd.getOffset()).toString();
                        //System.out.println("Candidate: " + content);
                        candidates.add(new Entity(content, annStart.getOffset(), annEnd.getOffset(), "named entity"));
                    } catch (InvalidOffsetException ex) {
                        //System.out.println("Problem in the loop 1");
                        Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if(entity_type.equals("all") || entity_type.equals("ce")){
                AnnotationSet as_common_entity = as_all.get("e");
                Iterator anot_e = as_common_entity.iterator();
                while(anot_e.hasNext()){
                    try {
                        Annotation isaAnnot = (gate.Annotation) anot_e.next();
                        Node annStart = isaAnnot.getStartNode();
                        Node annEnd = isaAnnot.getEndNode();
                        ////System.out.println("Start offset"+ annStart.getOffset());
                        ////System.out.println("End offset"+ annEnd.getOffset());
                        String content = d.getContent().getContent(annStart.getOffset(), annEnd.getOffset()).toString();
                        //System.out.println("Candidate: " + content);
                        candidates.add(new Entity(content, annStart.getOffset(), annEnd.getOffset(), "common entity"));
                    } catch (InvalidOffsetException ex) {
                        //System.out.println("Problem in the loop 2");
                       Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            //System.out.println("Number of extracted candidates: "+candidates.size());
            return candidates;
            
        } catch (ExecutionException ex) {
            //System.out.println("Problem 1"+ex.getMessage());
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ResourceInstantiationException ex) {
            //System.out.println("Problem 2"+ex.getMessage());
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public ArrayList<Entity> extractEntityCandidatesEN(String query, String lang, String entity_type){
        
        
        try {
            
            Document doc = Factory.newDocument(query);
            doc.setName("Query_Document");
            Corpus corpus = Factory.newCorpus("");
            corpus.add(doc);
            
            enEntityExtractionPipeline.setCorpus(corpus);
            enEntityExtractionPipeline.execute();
            
            Document[] docs = (Document[]) corpus.toArray(new Document[corpus.size()]);
            //System.out.println("EN Entity Extraction pipeline finished");
            ArrayList<Entity> candidates = new ArrayList<Entity>();
            
            Document d = docs[0];
            AnnotationSet as_all = d.getAnnotations();
            
            //System.out.println("Found AS: "+ as_all.size());
            if(entity_type.equals("all") || entity_type.equals("ne")){
                
                AnnotationSet as_named_entity = as_all.get("ne");
                Iterator anot = as_named_entity.iterator();

                while(anot.hasNext()){
                    try {
                        Annotation isaAnnot = (gate.Annotation) anot.next();
                        Node annStart = isaAnnot.getStartNode();
                        Node annEnd = isaAnnot.getEndNode();
                        ////System.out.println("Start offset"+ annStart.getOffset());
                        ////System.out.println("End offset"+ annEnd.getOffset());
                        String content = d.getContent().getContent(annStart.getOffset(), annEnd.getOffset()).toString();
                        //System.out.println("Candidate: " + content);
                        candidates.add(new Entity(content, annStart.getOffset(), annEnd.getOffset(), "named entity"));
                    } catch (InvalidOffsetException ex) {
                        Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if(entity_type.equals("all") || entity_type.equals("ce")) {
                
                AnnotationSet as_common_nouns = as_all.get("e");
                Iterator cn_anot = as_common_nouns.iterator();

                while(cn_anot.hasNext()){
                    try {
                        Annotation isaAnnot = (gate.Annotation) cn_anot.next();
                        Node annStart = isaAnnot.getStartNode();
                        Node annEnd = isaAnnot.getEndNode();
                        ////System.out.println("Start offset"+ annStart.getOffset());
                        ////System.out.println("End offset"+ annEnd.getOffset());
                        String content = d.getContent().getContent(annStart.getOffset(), annEnd.getOffset()).toString();
                        //System.out.println("Candidate: " + content);
                        candidates.add(new Entity(content,  annStart.getOffset(), annEnd.getOffset(), "common entity"));
                    } catch (InvalidOffsetException ex) {
                        Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            
            return candidates;
            
        } catch (ExecutionException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ResourceInstantiationException ex) {
            Logger.getLogger(THDInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}