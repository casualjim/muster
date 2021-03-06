package muster

import com.fasterxml.jackson.databind.ObjectMapper
import muster.codec.json._

class JsonParsersBenchmark extends com.google.caliper.SimpleBenchmark {

   import Benchmarks._

   val mapper = new ObjectMapper()
   val jsonSmart = new net.minidev.json.parser.JSONParser()


   def timeMusterJacksonParserForLarge(reps: Int): Unit =
     for (i <- 0 to reps) codec.jackson.JacksonCodec.createCursor(json).nextNode()

   def timeMusterJawnParserForLarge(reps: Int): Unit =
     for (i <- 0 to reps) codec.jawn.JawnCodec.createCursor(json).nextNode()

   def timeJson4sNativeForLarge(reps: Int): Unit =
     for (i <- 0 to reps) org.json4s.native.JsonMethods.parse(json)

   def timeJson4sJacksonForLarge(reps: Int): Unit =
     for (i <- 0 to reps) org.json4s.jackson.JsonMethods.parse(json)

   def timeJacksonForLarge(reps: Int): Unit =
     for (i <- 0 to reps) mapper.readTree(json)

   def timeJsonSmartForLarge(reps: Int): Unit =
     for (i <- 0 to reps) jsonSmart.parse(json)

 }