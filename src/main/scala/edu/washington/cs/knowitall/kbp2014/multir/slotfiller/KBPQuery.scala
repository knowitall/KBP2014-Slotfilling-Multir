package edu.washington.cs.knowitall.kbp2014.multir.slotfiller

import scala.xml.XML
import KBPQueryEntityType._

case class KBPQuery (val id: String, val name: String, val doc: String,
    val begOffset: Int, val endOffset: Int, val entityType: KBPQueryEntityType){
  
  def aliases():List[String] = name :: List[String]()
  
  override def toString():String = id + "\t" + name
  
}

object KBPQuery {

  import KBPQueryEntityType._

  
  private def parseSingleKBPQueryFromXML(queryXML: scala.xml.Node): Option[KBPQuery] = {

    try{
	    val idText = queryXML.attribute("id") match 
	    		{case Some(id) if id.length ==1 => id(0).text
	    		 case None => throw new IllegalArgumentException("no id value for query in xml doc")
	    		}
	    val nameText = queryXML.\\("name").text
	    val docIDText = queryXML.\\("docid").text
	    val begText = queryXML.\\("beg").text
	    val begInt = begText.toInt
	    val endText = queryXML.\\("end").text
	    val endInt = endText.toInt
	    val entityTypeText = queryXML.\\("enttype").text
	    val entityType = entityTypeText match {
	      case "ORG" => ORG
	      case "PER" => PER
	      case _ => throw new IllegalArgumentException("improper 'enttype' value in xml doc")
	    }

	    new Some(KBPQuery(idText,nameText,docIDText,begInt,endInt,entityType))
    }
  }
  

  def parseKBPQueries(pathToFile: String): List[KBPQuery] = {
    
     val xml = XML.loadFile(pathToFile)
     val queryXMLSeq = xml.\("query")
     
     val kbpQueryList = for( qXML <- queryXMLSeq) yield parseSingleKBPQueryFromXML(qXML)
    
     kbpQueryList.toList.flatten
  }
  
}
