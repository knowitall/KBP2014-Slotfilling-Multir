package edu.washington.cs.knowitall.kbp2014.multir.slotfiller

import jp.sf.amateras.solr.scala.SolrClient
import edu.stanford.nlp.dcoref.CorefChain.CorefMention

object SolrHelper {
  
  val solrUrlForXMLDocsFromOldCorpus = "http://knowitall:knowit!@rv-n16.cs.washington.edu:9325/solr/oldCorpus"
  val solrUrlForXMLDocsFromNewCorpus = "http://knowitall:knowit!@rv-n16.cs.washington.edu:9325/solr/newCorpus"
  var solrXMLDocsClient : Option[SolrClient] = None
  
  
  def setConfigurations(oldOrNew: String, corefOn: Boolean){
    oldOrNew match{
      case "old" => {solrXMLDocsClient = Some(new SolrClient(solrUrlForXMLDocsFromOldCorpus)) }
      case "new" => {solrXMLDocsClient = Some(new SolrClient(solrUrlForXMLDocsFromNewCorpus)) }
    }
    
  }
  
  def getRelevantDocuments(queries: List[KBPQuery]): Map[KBPQuery,List[String]] = {
    val query = solrXMLDocsClient.get.query("*:*").fields("docid","xml");
    var result = query.rows(1000).getResultAsMap()
    var start = 0
    val queryDocMap = scala.collection.mutable.Map[KBPQuery,List[String]]()
    var count = 0
    while(result.documents.size > 0){
    for( doc <- result.documents){
      val docString = doc.get("xml").toString()
      val noNewLineDocString = docString.replaceAll("\n+", " ");
      for(q <- queries){
        for(alias <- q.aliases){
          if(noNewLineDocString.contains(alias)){
            val docid = doc.get("docid").get.toString
            println(docid + " contains string " + alias)
            val r = queryDocMap.get(q)
            if(r.isDefined){
              val currentList = r.get
              if(!currentList.contains(docid)) queryDocMap.put(q,docid :: currentList)
            }
            else{
              val newList = List[String](docid)
              queryDocMap.put(q,newList)
            }
          }
        }
      }
      count+=1
      if(count % 100 == 0){
        println(count + " docs processed")
      }
      if(count % 1000 == 0){
        start += 1000
        result = query.start(start).rows(1000).getResultAsMap()
      }
    }
    }
    queryDocMap.toMap
  }
  
  
  def getRawDoc(docId: String): String = {
    val query = solrXMLDocsClient.get.query("docid:\""+ docId + "\"")
    val result = query.getResultAsMap()
    if(result.documents.length != 1){
      System.err.println(docId + " was not found in corpus");
      ""
    }
    else{
      result.documents.head("xml").toString
    }
  }

}