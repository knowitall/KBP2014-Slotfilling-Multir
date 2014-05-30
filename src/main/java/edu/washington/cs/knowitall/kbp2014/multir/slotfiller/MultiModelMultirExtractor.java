package edu.washington.cs.knowitall.kbp2014.multir.slotfiller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.sententialextraction.DocumentExtractor;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.corpus.SentOffsetInformation.SentStartOffset;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;

public abstract class MultiModelMultirExtractor {

	protected List<SententialInstanceGeneration> sigs;
	protected List<String> modelFilePaths;
	protected ArgumentIdentification ai;
	protected FeatureGenerator fg;
	
	public MultiModelMultirExtractor(){
		sigs = new ArrayList<>();
		modelFilePaths = new ArrayList<>();
		ai = null;
		fg = null;
	}
	
	public List<Extraction> extract(Annotation doc, KBPQuery q) throws IOException{
		List<Extraction> extractions = new ArrayList<>();

		List<Pair<SententialInstanceGeneration,DocumentExtractor>> sigModelPairs = getSigModelPairs(q);
		List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap s : sentences){
			List<Argument> arguments = ai.identifyArguments(doc,s);
			for(Pair<SententialInstanceGeneration,DocumentExtractor> sigModelPair : sigModelPairs){
				DocumentExtractor de = sigModelPair.second;
				SententialInstanceGeneration sig = sigModelPair.first;
				List<Pair<Argument,Argument>> sententialPairs = sig.generateSententialInstances(arguments, s);
				for(Pair<Argument,Argument> sententialPair : sententialPairs){
					Triple<String,Double,Double> result = de.extractFromSententialInstance(sententialPair.first, sententialPair.second, s, doc);
					String rel = result.first;
					double score = result.third;
					if(!rel.equals("NA")){
						//add new extraction
						Argument arg1 = sententialPair.first;
						Argument arg2 = sententialPair.second;
						arg1 = new Argument(arg1.getArgName(),s.get(SentStartOffset.class)+arg1.getStartOffset(),s.get(SentStartOffset.class)+arg1.getEndOffset());
						arg2 = new Argument(arg2.getArgName(),s.get(SentStartOffset.class)+arg2.getStartOffset(),s.get(SentStartOffset.class)+arg2.getEndOffset());
						String arg1Link = null;
						String arg2Link = null;
						String arg1BestMention = null;
						String arg2BestMention = null;
						String docName = doc.get(SentDocName.class);
						Integer sentNum = s.get(SentGlobalID.class);
						Integer arg1BestMentionSentNum = null;
						Integer arg2BestMentionSentNum = null;
						
						Extraction e = new Extraction(arg1,arg2,rel,score,
								arg1Link,arg2Link,arg1BestMention,arg2BestMention,
								docName,sentNum,arg1BestMentionSentNum,arg2BestMentionSentNum);
						extractions.add(e);
					}
				}
			}
		}
		return extractions;
	}
	
	private List<Pair<SententialInstanceGeneration,DocumentExtractor>> getSigModelPairs(KBPQuery q) throws IOException{
		List<Pair<SententialInstanceGeneration,DocumentExtractor>> sigModelPairs = new ArrayList<>();
		
		for(int i =0; i < sigs.size(); i++){
			String modelFilePath = modelFilePaths.get(i);
			BufferedReader br = new BufferedReader(new FileReader(new File(modelFilePath+"/mapping")));
			String firstLine = br.readLine();
			Integer numRels = Integer.parseInt(firstLine.trim());
			for(int j =0; j < numRels; j++){
				String nextRel = br.readLine().trim();
				if(nextRel.contains(q.entityType().toString().toLowerCase())){
					DocumentExtractor de = new DocumentExtractor(modelFilePath,fg,ai,sigs.get(i));
					Pair<SententialInstanceGeneration,DocumentExtractor> newPair = new Pair<>(sigs.get(i),de);
					sigModelPairs.add(newPair);
					break;
				}
			}
			br.close();
		}
		return sigModelPairs;
	}
}
