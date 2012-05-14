package uk.co.jbothma.gate.swespark;

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
				if (tokAnnot.getFeatures().get(ANNIEConstants.TOKEN_KIND_FEATURE_NAME).equals("word"))
				{
					word = (String) tokAnnot.getFeatures().get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME);
					pos = (String) tokAnnot.getFeatures().get("category");
					sentence += word + "/" + pos + " ";
				}
			}
			System.out.println(sentence);
			chunkerInput[0] = sentence;
			nounPhrases = chunker.parse_input(chunkerInput);
			annotatePhrases(tokAnnotList, nounPhrases);
		}
	}
	
	private void annotatePhrases(List<Annotation> tokAnnotList, String[] nounPhrases) {
		if (nounPhrases.length > 0) {
			String[] phraseWords = nounPhrases[0].split(" "); 
			for (Annotation tokAnnot : tokAnnotList) {
				if (tokAnnot.getFeatures().get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME)
						.equals(phraseWords[0])) {
					System.out.println(nounPhrases[0]);
					startAnnot(tokAnnot, phraseWords);
				}
			}
		}
	}
	private void startAnnot(Annotation tokAnnot, String[] phraseWords) {
		System.out.println("Phrase start " + tokAnnot.getStartNode().getOffset() + " " + phraseWords[0]);
	}
	
	private void contAnnot() {
		
	}
}
