package edu.washington.cs.knowitall.kbp2014.multir.slotfiller

object KBPQueryEntityType extends Enumeration{
  type KBPQueryEntityType = Value
  val ORG, PER = Value
  
  def fromString(str: String) = str.trim.toLowerCase match {
    case "per" | "person" => PER
    case "org" | "organization" => ORG
    case _ => throw new RuntimeException(s"Invalid KBPQueryEntityType: $str")
  }
  
  def toString(t: KBPQueryEntityType) = t match {
    case ORG => "ORG"
    case PER => "PER"
  }
}