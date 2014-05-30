package edu.washington.cs.knowitall.kbp2014.multir.slotfiller

import edu.washington.multirframework.corpus.Corpus
import edu.washington.multirframework.corpus.DefaultCorpusInformationSpecification
import edu.washington.multirframework.corpus.DocumentInformationI
import edu.washington.multirframework.corpus.DocCorefInformation
import edu.washington.multirframework.corpus.SentInformationI
import edu.washington.multirframework.corpus.SentNamedEntityLinkingInformation
import edu.washington.multirframework.corpus.TokenInformationI
import java.io.File
import edu.stanford.nlp.pipeline.Annotation
import edu.washington.multir.sententialextraction.DocumentExtractor
import edu.washington.multir.preprocess.CorpusPreprocessing
import edu.stanford.nlp.ling.CoreAnnotations
import java.util.Properties
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.dcoref.CorefCoreAnnotations
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem
import edu.stanford.nlp.dcoref.Document
import edu.stanford.nlp.dcoref.RuleBasedCorefMentionFinder
import edu.stanford.nlp.dcoref.Dictionaries
import collection.JavaConverters._

object RunKBPMultirExtractor {
  
//      val corefProps = new Properties();
//	    corefProps.put("annotators", "dcoref");
//	    corefProps.put("clean.allowflawedxml", "true");
//	    val corefAnnotation = new StanfordCoreNLP(corefProps)
  val annotatorHelper = new StanfordAnnotatorHelperMethods()

  val cis  = new DefaultCorpusInformationSpecification()
  val javaDocInfoList = new java.util.ArrayList[DocumentInformationI]()
  javaDocInfoList.add(new DocCorefInformation())
  cis.addDocumentInformation(javaDocInfoList)
  val javaSentInfoList = new java.util.ArrayList[SentInformationI]()
  javaSentInfoList.add(new SentNamedEntityLinkingInformation())
  cis.addSentenceInformation(javaSentInfoList)
  
  def main(Args: Array[String]){
	  val queries = KBPQuery.parseKBPQueries(Args(0))
	  println("Printing " + queries.size + " queries....")
	  for(q <- queries){
	    println(q)
	  }
	  
	  val mode = Integer.parseInt(Args(1))
	  
	  //mode 0 is for doing document processing at run time
	  if(mode == 0){
		  // "old" or "new"
		  val conf = Args(2)
		  SolrHelper.setConfigurations(conf,false)
	
		  val entityRelevantDocSerialization = {
		    val f = new File(Args(3))
		    if(f.exists()){
		     QuerySetSerialization.getRevelantDocIdMap(Args(2))
		    }
		    else{
		      // make this map and write it out
		      //val m = getRelevantDocuments(queries,solr).toMap
		      val qm = SolrHelper.getRelevantDocuments(queries)
		      val qidMap = qm.toList.map(f => (f._1.id,f._2)).toMap
		      QuerySetSerialization.writeRelevantDocIdMap(qidMap,Args(2))
		      qm 
		    }
		  }
		  
		  //extract
//		  queries.sortBy(f => entityRelevantDocSerialization.get())
//		  for(q <- queries)
		  
		  val multirCorpus = 
		  if(Args.size > 4){
		    new Corpus(Args(4),cis,true)
		  }
		  else{
		    //process documents and create new corpus
		    processDocumentsStanford(entityRelevantDocSerialization.values.flatten.toSet)
		  }
	  }
	  //mode 1 is for using a fully processed document corpus
	  else if(mode == 1){
	    val multirCorpus = new Corpus(Args(2),cis,true)
	    val multirExtractor = new MultiModelMultirExtractorVersion1()
	    
	    val entityRelevantDocSerialization = QuerySetSerialization.getRevelantDocIdMap(Args(3))
	    
	    //val docIterator = multirCorpus.getDocumentIterator().asScala
	    var count = 0
//	    for(doc <- docIterator){
//	      for(q <- queries){
//	        //val docText = doc.get(classOf[CoreAnnotations.TextAnnotation])
//	        //val hasAlias = q.aliases.exists(f => docText.contains(f))
//	        //if(hasAlias){
//	          val extractions = multirExtractor.extract(doc, q).asScala
//	          for(e <- extractions) println(e)
//	        //}
//	      }
//	      count +=1
//	      if(count % 1000 == 0) println(count + " docs processed")
//	    }
	    
	    for(q <- queries.sortBy(f => if(entityRelevantDocSerialization.contains(f.id))(f.id).size else 0)){
	      println(q)
	      if(entityRelevantDocSerialization.contains(q.id)){
		      val documents = entityRelevantDocSerialization(q.id)
		      for(docName <- documents){
		        println(docName)
		        val doc = multirCorpus.getDocument(docName)
		        if(doc !=null){
		        val extractions = multirExtractor.extract(doc,q).asScala
		        
		        for(e <- extractions) println(e)
		        }
		      }
	    	}
	    }
	  }
	  else{
	    throw new IllegalArgumentException("Second arg must be 0 or 1 to define the mode to run in")
	  }
  }
  
//  def getRelevantDocuments(queries: List[KBPQuery],solr: SolrQueryExecutor) : List[(String,List[String])] ={
//    for(q <- queries) yield {
//      (q.name,solr.issueManualSolrQuery(q.name).toList)
//    }
//  }
  
  def processDocumentsManual(documents: Set[String]){
        println("Number of docs = " + documents.size)
    var start :Long = 0
    var end: Long = 0
    for(doc <- documents.take(3)){
      start = System.currentTimeMillis()
      try{
        val rawDoc = SolrHelper.getRawDoc(doc)
        println(doc)
        val preprocessedAndParsedDoc = CorpusPreprocessing.getTestDocumentFromRawString(rawDoc,doc)

        //run coref
        //corefAnnotation.annotate(preprocessedAndParsedDoc)
        val d = new Document()
        //need to set a few variables first
        //need to set indices on tokens
        setIndices(preprocessedAndParsedDoc)
        val corefMentionFinder = new RuleBasedCorefMentionFinder()
        d.annotation = preprocessedAndParsedDoc
        d.predictedOrderedMentionsBySentence = corefMentionFinder.extractPredictedMentions(preprocessedAndParsedDoc, 0, new Dictionaries())
        val coref = new SieveCoreferenceSystem(new Properties())
        coref.coref(d)
        
        
        println("DOC COREF = " + preprocessedAndParsedDoc.get(classOf[CorefCoreAnnotations.CorefChainAnnotation]))
        
        
        
        //run wikifier

      }
      catch{
        case e: Exception => e.printStackTrace()
      }
      end = System.currentTimeMillis()
      println("Document took " + (end-start) + " milliseconds")
    }
  }
  
  def processDocumentsStanford(documents: Set[String]){
    println("Number of docs = " + documents.size)
       var start :Long = 0
       var end: Long = 0    
    for(doc <- documents.take(3)){

      start = System.currentTimeMillis()

      try{
        val rawDoc = SolrHelper.getRawDoc(doc)
        println(doc)
        val processedDoc = new Annotation(rawDoc)
        annotatorHelper.getCorefPipeline().annotate(processedDoc)
        println("preprocessed and parsed document")
        println("DOC STIRNG = " + processedDoc.get(classOf[CoreAnnotations.TextAnnotation]))
        println("DOC COREF = " + processedDoc.get(classOf[CorefCoreAnnotations.CorefChainAnnotation]))
      }
      catch{
        case e: Exception => e.printStackTrace()
      }
      end = System.currentTimeMillis()
      println("Document took " + (end-start) + " milliseconds")
      
    }
    
  }
  
  def setIndices (doc: Annotation){
    val sentences = doc.get(classOf[CoreAnnotations.SentencesAnnotation]).asScala
    for(s <- sentences){
      val coreLabels = s.get(classOf[CoreAnnotations.TokensAnnotation])
      for(index <- 1 to coreLabels.size()){
        coreLabels.get(index-1).setIndex(index)
      }
    }
  }
  

}