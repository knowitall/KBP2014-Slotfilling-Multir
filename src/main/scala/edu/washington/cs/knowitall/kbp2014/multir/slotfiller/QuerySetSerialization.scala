package edu.washington.cs.knowitall.kbp2014.multir.slotfiller

import scala.io.Source
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
object QuerySetSerialization {

  
  def getRevelantDocIdMap(pathToFile: String):Map[String,List[String]] = {
    val s = Source.fromFile(pathToFile)
    val tuples = for(l <-s.getLines) yield {
      val pieces = l.split("\t")
      if(pieces.size ==2) (pieces(0),pieces(1).split("\\s+").toList)
      else (pieces(0),Nil)
    }
    tuples.toMap
  }
  
  def writeRelevantDocIdMap(m: Map[String,List[String]], f: String) {
    val bw = new BufferedWriter(new FileWriter(new File(f)))
  
    for((k,v) <- m){
      bw.write(k)
      bw.write("\t")
      bw.write(v.mkString(" "))
      bw.write("\n")
    }
    
    bw.close()
  }
}