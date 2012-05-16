package uk.co.jbothma.gate.swespark;
 

import gate.Annotation;
import gate.AnnotationSet;
import gate.Factory;
import gate.creole.ANNIEConstants;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleResource;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import chunker.Chunk;

/**
 * GATE Wrapper for the Java Swe-SPARK NP-chunker.
 * http://stp.lingfil.uu.se/~bea/resources/spark/
 * Mirrored at https://github.com/jbothma/Swe-SPARK-Java
 * 
 * Expects inputAS to have ANNIEConstants.TOKEN_ANNOTATION_TYPE
 * and ANNIEConstants.SENTENCE_ANNOTATION_TYPE.
 * Also expects annotations of type ANNIEConstants.TOKEN_ANNOTATION_TYPE
 * that have feature ANNIEConstants.TOKEN_KIND_FEATURE_NAME of "word" 
 * to have a feature "category" which holds the PAROLE POS tag for the string.
 * 
 * This took a little bit of inspiration from mark.chunking.GATEWrapper
 * for aligning the phrase to the tokens and then annotating the phrase.
 */
@CreoleResource(name = "SweSPARK Chunker",
comment = "Chunker that can currently provide Noun Phrase annotations.")
public class SweSPARKPR extends AbstractLanguageAnalyser {

	private static final long serialVersionUID = -8974845243152085380L;

	private String inputASName, outputASName;

	public String getinputASname() {
		return inputASName;
	}

	public void setInputASname(String inputASname) {
		this.inputASName = inputASname;
	}

	public String getoutputASname() {
		return outputASName;
	}

	public void setOutputASname(String outputASname) {
		this.outputASName = outputASname;
	}

	public void execute() throws ExecutionException {
		gate.Document doc;
		String word, pos, sentence;
		AnnotationSet inputAS, outputAS;
		Iterator<Annotation> sentIter;
		Annotation sentAnnot;
		Chunk chunker;
		String[] chunkerInput, nounPhrases;
		List<Annotation> tokAnnotList;
		AnnotationSet tokAnnots;
		
		doc = getDocument();
	    if(doc == null)
	        throw new ExecutionException("No document to process!");

		chunkerInput = new String[1];
		chunker = new Chunk();
	    
		inputAS = (inputASName == null || inputASName.trim().equals(""))
				? document.getAnnotations()
				: doc.getAnnotations(inputASName);				
		outputAS = (outputASName == null || outputASName.trim().equals(""))
				? document.getAnnotations()
        		: doc.getAnnotations(outputASName);
		
		assertAnnotationTypes(inputAS, new String[] {
			ANNIEConstants.SENTENCE_ANNOTATION_TYPE,
			ANNIEConstants.TOKEN_ANNOTATION_TYPE,
		});
			
		// iterate through the sentences
		sentIter = inputAS.get(ANNIEConstants.SENTENCE_ANNOTATION_TYPE).iterator();

		while (sentIter.hasNext()) {
			sentence = "";
			sentAnnot = (Annotation) sentIter.next();
			tokAnnots = gate.Utils.getContainedAnnotations(
					inputAS, sentAnnot, ANNIEConstants.TOKEN_ANNOTATION_TYPE);
			tokAnnotList = gate.Utils.inDocumentOrder(tokAnnots);
			
			for (Annotation tokAnnot : tokAnnotList) {
				if (tokAnnotIsWord(tokAnnot))
				{
					word = tokAnnotString(tokAnnot);
					pos = (String) tokAnnot.getFeatures().get("category");
					sentence += word + "/" + pos + " ";
				}
			}

			chunkerInput[0] = sentence;
			nounPhrases = chunker.parse_input(chunkerInput);
			
			alignAndAnnotPhrases(tokAnnotList, Arrays.asList(nounPhrases), outputAS);
		}
	}
	
	/**
	 * Align the phrases to a given sentence and add annotations to corpus.
	 * 
	 * This is called once per sentence. 
	 */
	private void alignAndAnnotPhrases(
			List<Annotation> tokAnnotList,
			List<String> nounPhrases,
			AnnotationSet outputAS) {
		int phrasIdx = 0;
		int phrasWordIdx = 0;
		int phrasStartIdx = -1, phrasEndIdx = -1;
		int phrasAdv; // either 0 or 1
		String[] phrasWords;
		
		/*
		 * Advance along tokens within the sentence, ignoring non-word tokens.
		 * 
		 * Maintain an index to the current phrase in the set of phrases
		 * and another index to the current word in the words in the current phrase.
		 * 
		 * While a phrase matches a series of tokens, advance the word index.
		 * If the whole phrase matches, annotate the phrase in the corpus.
		 * If part of the phrase doesn't match, continue along the sentence while
		 * starting at the beginning of the phrase - this is assuming that
		 * the beginning of the phrase matched but the phrase really occurs later 
		 * in the sentence
		 */
		phrasWords = nounPhrases.get(phrasIdx).split(" ");
		for (int tokIdx = 0; tokIdx < tokAnnotList.size(); tokIdx++) {
			if (tokAnnotIsWord(tokAnnotList.get(tokIdx))) {
				if (tokAnnotString(tokAnnotList.get(tokIdx)).equals(phrasWords[phrasWordIdx])) {
					// current phras word == current sentence word
					if (phrasWordIdx == 0) {
						// if it's the start of a phrase,
						// note the position in token list
						phrasStartIdx = tokIdx;
					}
					if (phrasWordIdx == (phrasWords.length - 1)) {
						// if it's the end of the phrase,
						// note the position in the token list and annotate the phrase
						phrasEndIdx = tokIdx;
						
						annotate(outputAS, tokAnnotList, phrasStartIdx, phrasEndIdx);
						
						if (phrasIdx == (nounPhrases.size() - 1)) {
							// if it's the end of the phrases, stop with this sentence
							return;
						} else {
							// otherwise start at the beginning of the next phrase
							phrasWordIdx = 0;
							phrasAdv = 0; // overwrite first word advance
							phrasIdx++;
							phrasWords = nounPhrases.get(phrasIdx).split(" ");
						}
					} else {
						// if it's in the middle of a phrase, advance the phrase word idx
						phrasAdv = 1;
					}
					phrasWordIdx += phrasAdv;
				} else {
					// if the current phrase word != current sentence word,
					// start at the beginning of the phrase to compare to the next sentence word.
					phrasWordIdx = 0;
				}
			}
		}
	}
	
	private void annotate(AnnotationSet outputAS, List<Annotation> tokAnnotList, int startIdx, int endIdx) {
		Annotation startTokAnnot = tokAnnotList.get(startIdx);
		Annotation endTokAnnot = tokAnnotList.get(endIdx);

		outputAS.add(
				startTokAnnot.getStartNode(),
				endTokAnnot.getEndNode(),
				"NounPhrase",
				Factory.newFeatureMap()
		);
	}
	
	private boolean tokAnnotIsWord(Annotation tokAnnot) {
		return tokAnnot.getFeatures().get(ANNIEConstants.TOKEN_KIND_FEATURE_NAME).equals("word");
	}
	
	private String tokAnnotString(Annotation tokAnnot) {
		return (String) tokAnnot.getFeatures().get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME);
	}
	
	private void assertAnnotationTypes(AnnotationSet annSet, String[] annTypes) throws ExecutionException {
		for (String annType : annTypes) {
			if (!annSet.getAllTypes().contains(annType))
				throw new ExecutionException(
						"Missing required Annotation Type " + annType + 
						" in Annotation Set " + annSet.getName());
		}
	}
}
