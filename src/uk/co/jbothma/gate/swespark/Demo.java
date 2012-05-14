package uk.co.jbothma.gate.swespark;

import java.io.File;
import java.net.MalformedURLException;

import gate.DataStore;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.SerialAnalyserController;
import gate.persist.SerialDataStore;
import gate.util.GateException;

public class Demo {
	private static String dataStorePath = "/home/jdb/workspace/SweSPARKGATEPR/demo/ManskligaRattigheter/";
	
	public static void main(String[] args) throws GateException, MalformedURLException {
		Gate.init();
		Gate.getCreoleRegister().registerDirectories(
	            new File("/home/jdb/workspace/MorphP").toURI().toURL());
		Gate.getCreoleRegister().registerDirectories(
	            new File(System.getProperty("user.dir")).toURI().toURL());
		System.out.println(System.getProperty("user.dir"));
		
		// get the datastore
		SerialDataStore dataStore = (SerialDataStore) Factory.openDataStore("gate.persist.SerialDataStore", "file:///" + dataStorePath);
		dataStore.open();
		System.out.println("Demo: opened datastore.");
		
		// get the corpus
		Object corpusID = dataStore.getLrIds("gate.corpora.SerialCorpusImpl").get(0);
		FeatureMap corpFeatures = Factory.newFeatureMap();
		corpFeatures.put(DataStore.LR_ID_FEATURE_NAME, corpusID);
		corpFeatures.put(DataStore.DATASTORE_FEATURE_NAME, dataStore);
		//tell the factory to load the Serial Corpus with the specified ID from the specified  datastore
		gate.Corpus corpus = (gate.Corpus)Factory.createResource("gate.corpora.SerialCorpusImpl", corpFeatures);
		System.out.println("Demo: got the corpus.");
		
		// setup the pipeline
		SerialAnalyserController pipeline = (SerialAnalyserController)Factory
	            .createResource("gate.creole.SerialAnalyserController");
//		pipeline.add((gate.LanguageAnalyser)Factory
//	              .createResource("sheffield.creole.morph.Morpher"));
		pipeline.add((gate.LanguageAnalyser)Factory
	              .createResource("uk.co.jbothma.gate.swespark.SweSPARKPR"));
		pipeline.setCorpus(corpus);
		System.out.println("Demo: pipeline is ready.");
		
		// execute the pipeline
		pipeline.execute();
		System.out.println("Demo: pipeline is executed.");
	}

}
