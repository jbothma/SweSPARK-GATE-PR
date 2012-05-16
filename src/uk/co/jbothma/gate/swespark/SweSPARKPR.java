package uk.co.jbothma.gate.swespark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import chunker.Chunk;

import gate.creole.AbstractLanguageAnalyser;
import gate.Annotation;
import gate.AnnotationSet;
import gate.creole.ExecutionException;
import gate.creole.ANNIEConstants;
import gate.creole.metadata.CreoleResource;

@CreoleResource(name = "SweSPARK Chunker",
comment = "Chunker that can currently provide Noun Phrase annotations.")
public class SweSPARKPR extends AbstractLanguageAnalyser {

	private static final long serialVersionUID = -8974845243152085380L;

	private String inputASName, outputASName;

	public String getinputASname() {
		return inputASName;
	}

	public void setinputASname(String inputASname) {
		this.inputASName = inputASname;
	}

	public String getoutputASname() {
		return outputASName;
	}

	public void setoutputASname(String outputASname) {
		this.outputASName = outputASname;
	}

	public void execute() throws ExecutionException {
		gate.Document doc;
		String word, pos, sentence;
		AnnotationSet inputAnnSet, outputAnnSet;
		Iterator sentIter, tokIter;
		Annotation sentAnnot;
		Chunk chunker;
		String[] chunkerInput, nounPhrases, npWords;
		long sentPos;
		List<Annotation> tokAnnotList;
		AnnotationSet tokAnnots;
		
		System.out.println("SweSPARKPR: starting to execute.");
		doc = getDocument();
	    if(doc == null)
	        throw new ExecutionException("No document to process!");

		chunkerInput = new String[1];
		chunker = new Chunk();
		
	    System.out.println("SweSPARKPR: got the document.");
	    inputASName = outputASName = "Blah";
		inputAnnSet = (inputASName == null || inputASName.length() == 0)
				? doc.getAnnotations()
				: doc.getAnnotations(inputASName);

		outputAnnSet = (outputASName == null || outputASName.length() == 0)
				? doc.getAnnotations()
				: doc.getAnnotations(outputASName);
				
		System.out.println("Annotation sets: " + doc.getAnnotationSetNames());
		System.out.println("Blah Annotation types: " + inputAnnSet.getAllTypes());

		// iterate through the sentences
		sentIter = inputAnnSet.get(ANNIEConstants.SENTENCE_ANNOTATION_TYPE).iterator();

		while (sentIter.hasNext()) {
			sentence = "";
			sentAnnot = (Annotation) sentIter.next();
			sentPos = sentAnnot.getStartNode().getOffset();
			System.out.println("SweSPARKPR: new sentence: " + sentAnnot.getStartNode().getOffset() + "-" + sentAnnot.getEndNode().getOffset());
			tokAnnots = gate.Utils.getContainedAnnotations(inputAnnSet, sentAnnot, ANNIEConstants.TOKEN_ANNOTATION_TYPE);
			tokAnnotList = gate.Utils.inDocumentOrder(tokAnnots);
			
			for (Annotation tokAnnot : tokAnnotList) {
				if (tokAnnotIsWord(tokAnnot))
				{
					word = tokAnnotString(tokAnnot);
					pos = (String) tokAnnot.getFeatures().get("category");
					sentence += word + "/" + pos + " ";
				}
			}
			System.out.println(sentence);
			chunkerInput[0] = sentence;
			nounPhrases = chunker.parse_input(chunkerInput);
			for (String phrase : nounPhrases) {
				System.out.println("  " + phrase);
			}
			annotatePhrases(tokAnnotList, Arrays.asList(nounPhrases));
		}
	}
	private void annotatePhrases(List<Annotation> tokAnnotList, List<String> nounPhrases) {
		int phrasIdx = 0;
		int phrasWordIdx = 0;
		int phrasStartIdx = -1, phrasEndIdx = -1;
		int phrasAdv; // either 0 or 1
		String[] phrasWords = nounPhrases.get(phrasIdx).split(" ");
		
		for (int tokIdx = 0; tokIdx < tokAnnotList.size(); tokIdx++) {
			if (tokAnnotIsWord(tokAnnotList.get(tokIdx))) {
				System.out.println("token: " + tokAnnotString(tokAnnotList.get(tokIdx)));
				if (tokAnnotString(tokAnnotList.get(tokIdx)).equals(phrasWords[phrasWordIdx])) {
					// current phras word == current sentence word
					if (phrasWordIdx == 0) {
						System.out.println("first word match");
						// if it's the start of a phrase,
						// note the position in token list
						phrasStartIdx = tokIdx;
					}
					if (phrasWordIdx == (phrasWords.length - 1)) {
						System.out.println("last word match");
						// if it's the end of the phrase,
						// note the position in the token list and annotate the phrase
						phrasEndIdx = tokIdx;
						System.out.println(
								"Phrase " + nounPhrases.get(phrasIdx) + " " + 
								phrasStartIdx + "-" + phrasEndIdx);
						
						if (phrasIdx == (nounPhrases.size() - 1)) {
							// if it's the end of the phrases, stop
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
	private boolean tokAnnotIsWord(Annotation tokAnnot) {
		return tokAnnot.getFeatures().get(ANNIEConstants.TOKEN_KIND_FEATURE_NAME).equals("word");
	}
	private String tokAnnotString(Annotation tokAnnot) {
		return (String) tokAnnot.getFeatures().get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME);
	}
//	/**
//	 * Add noun phrase annotations to corpus.
//	 * 
//	 * Advances along token annotations and phrase words,
//	 * trying to allign phrases to token annotations in the sentence.
//	 * When an entire phrase can be alligned with a series of tokens,
//	 * those tokens will be annotated as a noun phrase.
//	 * 
//	 * @param tokAnnotList
//	 * @param nounPhrases
//	 */
//	private void annotatePhrases(List<Annotation> tokAnnotList, List<String> nounPhrases) {
//		Annotation nextTokAnnot;
//		String nextSentWord, nextPhraseWord;
//		long phraseStartOffset, phraseEndOffset;
//		
//		while (tokAnnotList.size() > 0 && nounPhrases.size() > 0) {
//			List<String> phraseWordList = Arrays.asList(nounPhrases.get(0).split(" "));
//			nounPhrases.remove(0);
//			while (tokAnnotList.size() > 0 && phraseWordList.size() > 0) {
//				nextTokAnnot = tokAnnotList.get(0);
//				nextSentWord = (String) nextTokAnnot.getFeatures().get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME);
//				nextPhraseWord = phraseWordList.get(0);
//				if (nextSentWord.equals(nextPhraseWord)) {
//					phraseStartOffset = nextTokAnnot.getStartNode().getOffset();
//					tokAnnotList.remove(0);					
//					while (tokAnnotList.size() > 0 && phraseWordList.size() > 0) {
//						nextTokAnnot = tokAnnotList.get(0);
//						nextSentWord = (String) nextTokAnnot.getFeatures().get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME);
//						nextPhraseWord = phraseWordList.get(0);
//						if (nextSentWord.equals(nextPhraseWord)) {
//							
//					}
//				} else {
//					tokAnnotList.remove(0);
//				}
//			}
//		}
////		if (nounPhrases.length > 0) {
////			String[] phraseWords = nounPhrases[0].split(" "); 
////			for (Annotation tokAnnot : tokAnnotList) {
////				if (tokAnnot.getFeatures().get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME)
////						.equals(phraseWords[0])) {
////					System.out.println(nounPhrases[0]);
////					startAnnot(tokAnnot, phraseWords);
////					nounPhrases.
////				}
////			}
//		}
//	}
	
}
