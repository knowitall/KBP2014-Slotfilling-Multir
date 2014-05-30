package edu.washington.cs.knowitall.kbp2014.multir.slotfiller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import edu.knowitall.collection.immutable.Interval;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.util.CoreMap;





public class StanfordAnnotatorHelperMethods {
	
	private final  StanfordCoreNLP suTimePipeline;
	private final  StanfordCoreNLP corefPipeline;
	private String filePath = "/homes/gws/jgilme1/docs/";
	private Map<String,Annotation> corefAnnotationMap;
	private Map<String,Annotation> suTimeAnnotationMap;
	
	
	public StanfordAnnotatorHelperMethods(){
		Properties suTimeProps = new Properties();
		suTimeProps.put("annotators", "tokenize, ssplit, pos, lemma, cleanxml, ner");
		suTimeProps.put("sutime.binders", "0");
		suTimeProps.put("clean.datetags","datetime|date|dateline");
		this.suTimePipeline = new StanfordCoreNLP(suTimeProps);
		
		Properties corefProps = new Properties();
	    corefProps.put("annotators", "tokenize, cleanxml, ssplit, pos, lemma, ner, parse, dcoref");
	    corefProps.put("clean.allowflawedxml", "true");
	    corefProps.put("ner.useSUTime", "false");
	    //clean all xml tags
		this.corefPipeline = new StanfordCoreNLP(corefProps);
		
		corefAnnotationMap = new HashMap<String,Annotation>();
		suTimeAnnotationMap = new HashMap<String,Annotation>();


	}
	
	public StanfordCoreNLP getCorefPipeline(){return corefPipeline;}
	
	public static void main(String[] args) throws FileNotFoundException, IOException{
		StanfordAnnotatorHelperMethods sh = new StanfordAnnotatorHelperMethods();
		sh.runSuTime("testXMLDoc");
		
	}
	
	public void clearHashMaps(){
		corefAnnotationMap.clear();
		suTimeAnnotationMap.clear();
	}
	
	public void runSuTime(String docID) throws FileNotFoundException, IOException{
		Annotation document;
		if(suTimeAnnotationMap.containsKey(docID)){
			document = suTimeAnnotationMap.get(docID);
		}
		else{
		  String filePathPlusDocId = this.filePath+docID;
		  FileInputStream in = new FileInputStream(new File(filePathPlusDocId));
		  String fileString = IOUtils.toString(in,"UTF-8");
		  in.close();
		
		  document = new Annotation(fileString);
		  suTimePipeline.annotate(document);
		  suTimeAnnotationMap.put(docID, document);
		}
		
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for(CoreMap sentence: sentences){
	    	for(CoreLabel token: sentence.get(TokensAnnotation.class)){
	    		String word = token.get(TextAnnotation.class);
	    		String ne = token.get(NamedEntityTagAnnotation.class);
	    		String net = token.get(NormalizedNamedEntityTagAnnotation.class);
	    		Timex tt = token.get(TimexAnnotation.class);
	    		String tts = "";
	    		if(tt != null){
	    			tts = tt.value();
	    		}
	    		System.out.println(word+ " " + ne + " " + net + " " + tts);
	    	}
	    }
	    
	    String s =document.get(NamedEntityTagAnnotation.class);
	    System.out.println(s);

	}
	
	private String normalizeTimex(Timex t){
		if(t.timexType() == "DATE"){
	      String timexString = t.value();
	      if (timexString == null) return "";
	      String formattedString = normalizeDate(timexString);
		  return formattedString;
		}
		else{
			return "";
		}
	}
	
	private String normalizeDate(String dateString){
		  String formattedString = null;
	      if(Pattern.matches("\\w{4}", dateString)){
	    	  formattedString = dateString +"-XX-XX";
	      }
	      else if(Pattern.matches("\\w{2}-\\w{2}",dateString)){
	    	  formattedString = "XXXX-" + dateString; 
	      }
	      else if(Pattern.matches("\\w{4}-\\w{2}", dateString)){
	    	  formattedString = dateString + "-XX";
	      }
		  
	      if(formattedString == null){
	    	  return dateString;
	      }
	      else{
	    	  return formattedString;
	      }
	}
	

	
	public String getNormalizedDate(Interval charInterval, String docId, String originalString) throws IOException{
		Annotation document;
		if(suTimeAnnotationMap.containsKey(docId)){
			document = suTimeAnnotationMap.get(docId);
		}
		else{
			String xmlDoc = SolrHelper.getRawDoc(docId);
			if(xmlDoc.trim().isEmpty()){
				return originalString;
			}
			document = new Annotation(xmlDoc);
			suTimePipeline.annotate(document);
			suTimeAnnotationMap.put(docId, document);
		}
	
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		    for(CoreMap sentence: sentences){
		    	for(CoreLabel token: sentence.get(TokensAnnotation.class)){
		    		Timex tt = token.get(TimexAnnotation.class);
		    		if(charInterval.intersects(Interval.closed(token.beginPosition(), token.endPosition()))){
		    			if(tt != null && tt.value() != null){
		    				return normalizeTimex(tt);
		    			}
		    		}
		    	}
		    }
	       return normalizeDate(originalString);
	}
	

	


}
