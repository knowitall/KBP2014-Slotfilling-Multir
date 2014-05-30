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
import edu.washington.multirframework.corpus.SentNamedEntityLinkingInformation.NamedEntityLinkingAnnotation
import java.io.BufferedWriter
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.FileWriter

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
		     QuerySetSerialization.getRevelantDocIdMap(Args(3))
		    }
		    else{
		      // make this map and write it out
		      //val m = getRelevantDocuments(queries,solr).toMap
		      val qm = SolrHelper.getRelevantDocuments(queries)
		      val qidMap = qm.toList.map(f => (f._1.id,f._2)).toMap
		      QuerySetSerialization.writeRelevantDocIdMap(qidMap,Args(3))
		      qidMap
		    }
		  }
		 
		  
		  val multirExtractor = new MultiModelMultirExtractorVersion1()
		  val bw = new BufferedWriter(new FileWriter(new File(Args(4))))
		  //extract
		  val sortedQueries = queries.sortBy(f => entityRelevantDocSerialization.get(f.id).size)
		  for(query <- sortedQueries){
		      val documents = processDocuments(entityRelevantDocSerialization.values.flatten.toSet)
		      for(document <- documents){
		        if(document.isDefined){
		          val extractions = multirExtractor.extract(document.get, query).asScala
		          for(e <- extractions){
		            println(e)
		            val docText = document.get.get(classOf[CoreAnnotations.TextAnnotation])
		            val minIndex = math.min(e.getArg1().getStartOffset(),e.getArg2().getStartOffset())
		            val maxIndex = math.max(e.getArg2().getEndOffset(),e.getArg1().getEndOffset())
		            println("Sentence = " +docText.subSequence(minIndex,maxIndex))
		            bw.write(e + "\n")
		            bw.write("Sentence = " +docText.subSequence(minIndex,maxIndex)+"\n")
		          }
		        }
		      }
		  }
		  bw.close()
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
  
  def processDocumentsStanford(documents: Set[String]):  List[Option[Annotation]]  ={
    println("Number of docs = " + documents.size)
       var start :Long = 0
       var end: Long = 0    
    for(doc <- documents.toList) yield{
      start = System.currentTimeMillis()
      val a =processDocument(doc)
      end = System.currentTimeMillis()
      println("Document took " + (end-start) + " milliseconds")
      a
    }
  }
  
  def processDocuments(documents: Set[String]): List[Option[Annotation]] = {
	 println("Number of docs = " + documents.size)
	 var start :Long = 0
	 var end: Long = 0    
    for(doc <- documents.toList) yield{
      start = System.currentTimeMillis()
      val a =processDocument(doc)
      end = System.currentTimeMillis()
      println("Document took " + (end-start) + " milliseconds")
      a
    }
  }
  
  def cjParseDocument(docName: String): Option[Annotation] = {
      try{
        val rawDoc = SolrHelper.getRawDoc(docName)
        val preprocessedAndParsedDoc = CorpusPreprocessing.getTestDocumentFromRawString(rawDoc,docName)
        println("Document was cj parsed")
        Some(preprocessedAndParsedDoc)
      }
      catch{
        case e: Exception => e.printStackTrace()
        None
      }
  }
  
  def linkDocument(docName: String): Option[Annotation] ={
    try{
        val rawDoc = SolrHelper.getRawDoc(docName)
        val processedDoc = new Annotation(rawDoc)
        println("Document was linked")
        Some(processedDoc)
      }
      catch{
        case e: Exception => e.printStackTrace()
        None
      }
  }
  
  def processDocument(docName: String) : Option[Annotation]  ={
     try{
         println("Processing document " +docName)
    	 val stanfordDoc = stanfordProcessDocument(docName)
    	 val cjParsedDoc = cjParseDocument(docName)
    	 val linkedDoc = linkDocument(docName)
    	 if(stanfordDoc.isDefined && cjParsedDoc.isDefined && linkedDoc.isDefined){
    	   Some(joinAnnotations(stanfordDoc.get,cjParsedDoc.get,linkedDoc.get))
    	 }
    	 else{
    	   None
    	 }
      }
      catch{
        case e: Exception => e.printStackTrace()
        None
      }
  }
  
  def joinAnnotations(stanfordDoc: Annotation, cjParsedDoc: Annotation, linkedDoc: Annotation) : Annotation = {
    
    //add coref annotations to cjParsedDoc
    cjParsedDoc.set(classOf[CorefCoreAnnotations.CorefChainAnnotation],stanfordDoc.get(classOf[CorefCoreAnnotations.CorefChainAnnotation]))
    cjParsedDoc.set(classOf[NamedEntityLinkingAnnotation],linkedDoc.get(classOf[NamedEntityLinkingAnnotation]))

    cjParsedDoc
  }
  
  def stanfordProcessDocument(docName: String) : Option[Annotation] = {
      try{
        val rawDoc = SolrHelper.getRawDoc(docName)
        val processedDoc = new Annotation(rawDoc)
        annotatorHelper.getCorefPipeline().annotate(processedDoc)
        println("Document was Stanford Annotated")
        Some(processedDoc)
      }
      catch{
        case e: Exception => e.printStackTrace()
        None
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