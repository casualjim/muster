package muster
package codec
package json4s


import java.util.{Date, TimeZone}
import java.util

import muster.codec.json.{Aliased, Ac}
import org.json4s._
import JsonDSL._
import org.specs2.mutable.Specification

class ObjectJValueDecompositionSpec extends Specification {
  implicit val defaultFormats = DefaultFormats
  val format = JValueFormat

  val refJunk = Junk(2, "cats")
  val refJunkDict: String = org.json4s.jackson.Serialization.write(refJunk)

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  type Foo = Junk

  case class WithAlias(in: Foo)


  def write[T](value: T)(implicit cons: Producer[T]): JValue = format.from(value)

  "A JValue decomposer" should {

    "write a date" in {
      val date = new Date()
      val ds = SafeSimpleDateFormat.Iso8601Formatter.format(date)
      write(SafeSimpleDateFormat.Iso8601Formatter.parse(ds)) must_== JString(ds)
    }

    "write a symbol" in {
      val sym = 'a_symbol_of_sorts
      val js = JString(sym.name)
      write(sym) must_== js
    }

    "write a map with option values" in {
      val dict = Map("one" -> 1, "two" -> null, "three" -> 394)
      val json = org.json4s.Extraction.decompose(dict)
      write(Map("one" -> Some(1), "two" -> None, "three" -> Some(394))) must_== json
    }
    "write a map with a list of option values" in {
      val dict = Map("one" -> List(1), "two" -> List(3, null, 4), "three" -> List(394))
      val json = org.json4s.Extraction.decompose(dict)
      write(Map("one" -> List(Some(1)), "two" -> List(Some(3), None, Some(4)), "three" -> List(Some(394)))) must_== json
    }

    "write a very simple case class" in {
      val js = ("id" -> 1) ~ ("name" -> "Tom")
      val friend = Friend(1, "Tom")
      write[Friend](friend) must_== js
    }

    "write a very simple case class" in {
      val js = ("one" -> 1) ~ ("two" -> "Tom")
      write(Simple(1, "Tom")) must_== js
    }

    "write a very simple case class with an option field with the value provided" in {
      val js = ("one" -> 1) ~ ("two" -> "Tom")
      write(WithOption(1, Some("Tom"))) must_== js
    }

    "write a very simple case class with an option field with null" in {
      val js = ("one" -> 1) ~ ("two" -> JNull)
      write(WithOption(1, None)) must_== js
    }

    "write a very simple case class with an option field omitted" in {
      import muster.codec.json.OptionOverride._
      val js: JValue = ("one" -> 1) ~ ("two" -> JNothing)
      write(WithOption(1, None)) must_== js
    }

    "write list of simple case classes" in {
      val js = JArray(("one" -> 1) ~ ("two" -> "hello") :: ("one" -> 2) ~ ("two" -> "world") :: Nil)
      write(List(Simple(1, "hello"), Simple(2, "world"))) must_== js
    }

    "write a case class with a single list" in {
      val js: JValue = "lst" -> JArray(JInt(1) :: JInt(2) :: JInt(3) :: Nil)
      write(WithList(List(1, 2, 3))) must_== js
    }

    "write an object with list and map" in {
      val js = ("lst" -> JArray(JInt(1) :: JInt(2) :: JInt(3) :: Nil)) ~ ("map" -> ("foo" -> 1) ~ ("bar" -> 2))
      write(ObjWithListMap(List(1, 2, 3), Map("foo" -> 1, "bar" -> 2))) must_== js
    }

    "write an object with a date" in {
      val date = new Date
      val ds = SafeSimpleDateFormat.Iso8601Formatter.format(date)
      val pd = SafeSimpleDateFormat.Iso8601Formatter.parse(ds)
      val js: JValue = "date" -> ds
      write(WithDate(pd)) must_== js
    }

    "write an object with a Symbol" in {
      val js: JValue = "symbol" -> "baz"
      write(WithSymbol('baz)) must_== js
    }

    "write a NotSimple class" in {
      val source = NotSimple(456, Simple(1, "Tom"))
      val js = Extraction.decompose(source)
      write(source) must_== js
    }

    val junkJson = """{"in1":123,"in2":"456"}"""
    val thingWithJunkJson = s"""{"name":"foo","junk":$junkJson}"""
    val junk = Junk(123, "456")
    val thingWithJunk = ThingWithJunk("foo", junk)
    "write a ThingWithJunk" in {
      write(thingWithJunk) must_== Extraction.decompose(thingWithJunk)
    }

    "write type aliased thing with junk when alias is defined in a package object" in {
      val source = aliasing.WithAlias(junk)
      write(source) must_== Extraction.decompose(source)
    }

    "write type aliased thing with junk when alias is defined in an object" in {
      val source = Aliased.WithAlias(junk)
      write(source) must_== Extraction.decompose(source)
    }

    "write type aliased thing with junk when alias is defined in this class" in {
      val source = this.WithAlias(junk)
      write(source) must_== Extraction.decompose(source)
    }

    "write type aliased thing with junk when alias is defined in another class and companion object is used to invoke the macro" in {
      val ac = new Ac
      val source = ac.WithAlias(junk)
      write(source) must_== Extraction.decompose(source)
    }

    "write a crazy thing" in {
      val source = Crazy("bar", thingWithJunk)
      write(source) must_== Extraction.decompose(source)
    }

    "write an option inside an option for a null" in {
      val source = OptionOption(None)
      write(source) must_== JObject("in" -> JNull :: Nil)
    }

    "write an option inside an option for a value" in {
      val source = OptionOption(Some(Some(1)))
      write(source) must_== Extraction.decompose(source)
    }

    object ImplOverride {

      implicit object ImplOverrideWritable extends Producer[ImplOverride] {
        def produce(value: ImplOverride, formatter: OutputFormatter[_]): Unit = {
          formatter.startObject()
          formatter.startField("number")
          formatter.int(value.nr)
          formatter.endObject()
        }
      }

    }
    case class ImplOverride(nr: Int)
    "write with the custom implicit if one is provided" in {
      val js: JValue = "number" -> 3854
      write(ImplOverride(3854)) must_== js
    }

    "write a class with java style getter/setter definitions" in {
      val js = ("id" -> 1) ~ ("name" -> "Tom")
      val jstyle = new JavaStyle
      jstyle.setId(1)
      jstyle.setName("Tom")
      write(jstyle) must_== js
    }

    "write a java class" in {
      val js = ("id" -> 1) ~ ("name" -> "Tom")
      val simpleJava = new SimpleJava
      simpleJava setId 1
      simpleJava setName "Tom"
      write(simpleJava) must_== js
    }

    "write a java class with a list and name property" in {
      val js = ("name" -> "a thing") ~ ("lst" -> List(1,2,3,4))
      val result = new JavaListAndName
      result setName "a thing"
      val lst = new util.ArrayList[Integer]
      lst.add(1)
      lst.add(2)
      lst.add(3)
      lst.add(4)
      result setLst lst
      write(result) must_== js
    }

    "write a java class with a set and name property" in {
      val js = ("name" -> "a thing") ~ ("set" ->  List(1,2,3,4))
      val result = new JavaSetAndName
      result setName "a thing"
      val lst = new util.HashSet[Integer]
      lst.add(1)
      lst.add(2)
      lst.add(3)
      lst.add(4)
      result setSet lst
      write(result) must_== js
    }

    "write a java class with a map and name property" in {
      val js = ("name" -> "a thing") ~ ("dict" -> ("one" -> 1) ~ ("two" -> 2))
      val result = new JavaMapAndName
      result setName "a thing"
      val dict = new util.TreeMap[String, Integer]()
      dict.put("one", 1)
      dict.put("two", 2)
      result setDict dict
      write(result) must_== js
    }

    "write a java class with a list of map of list" in {
      val js = ("name" -> "a thing") ~ ("lst" -> List(("one" -> List(1,2,3)) ~ ("two" -> List(4,5,6))))
      val result = new JavaListOfMapOfList
      result setName "a thing"
      val dict = new util.TreeMap[String, util.List[Integer]]()
      val lst1 = new util.ArrayList[Integer]()
      lst1.add(1)
      lst1.add(2)
      lst1.add(3)
      val lst2 = new util.ArrayList[Integer]()
      lst2.add(4)
      lst2.add(5)
      lst2.add(6)
      dict.put("one", lst1)
      dict.put("two", lst2)
      val lst = new util.ArrayList[util.Map[String, util.List[Integer]]]()
      lst.add(dict)
      result setLst lst
      write(result) must_== js
    }

    "writes mutable junk" in {
      val js = ("in1" -> 2334) ~ ("in2" -> "some string")
      write(MutableJunk(2334, "some string")) must_== js
    }

    "writes mutable junk with a field" in {
      val js = ("in1" -> 2334) ~ ("in2" -> "some string")
      val expected = MutableJunkWithField(2334)
      expected.in2 = "some string"
      write(expected) must_== js
    }

    "write mutable junk with a junk field" in {
      val js = ("in1" -> 3994) ~ ("in2" -> Extraction.decompose(junk))
      val expected = MutableJunkWithJunk(3994)
      expected.in2 = junk
      write(expected) must_== js
    }

    "write a class with a default value and data is in the json" in {
      val js = ("in1" -> 123) ~ ("in2" -> "not the default")
      write(JunkWithDefault(123, "not the default")) must_== js
    }

    "write a class with a default value and use the default value for a missing json value" in {
      val js = ("in1" -> 123) ~ ("in2" -> "Default...")
      write(JunkWithDefault(123)) must_== js
    }

    "write a class with a list and name property" in {
      val js = jackson.parseJson("""{"name":"list and name","lst":[3949,3912,2050]}""")
      write(WithListAndName("list and name", List(3949, 3912, 2050))) must_== js
    }

    "write a class with an object list and a name" in {
      val js = jackson.parseJson(s"""{"name":"list and name","list":[$thingWithJunkJson]}""")
      write(WithObjList("list and name", List(thingWithJunk))) must_== js
    }

    "write a curried object" in {
      val js = jackson.parseJson("""{"in1":1395,"in2":4059,"in3":395}""")
      write(Curried(1395, 4059)(395)) must_== js
    }

    "write an object with a type param" in {
      val js = jackson.parseJson("""{"in1":39589}""")
      write(WithTpeParams(39589)) must_== js
    }

    "write an object with a seq" in {
      write(WithSeq(Seq(1, 3, 4))) must_== jackson.parseJson("""{"in":[1,3,4]}""")
    }

    "write an object with nested type params" in {
      val js = jackson.parseJson("""{"in1":12,"in2":{"in1":94}}""")
      write(new WithNstedTpeParams(12, WithTpeParams(94))) must_== js
    }

    "write an object with nested type params and a resolved param" in {
      val js = jackson.parseJson("""{"in3":12,"in4":{"in1":94}}""")
      write(ResolvedParams(12, WithTpeParams(94))) must_== js
    }

    "write an object with date and a name" in {
      val date = new Date
      val ds = SafeSimpleDateFormat.Iso8601Formatter.format(date)
      val pd = SafeSimpleDateFormat.Iso8601Formatter.parse(ds)
      val js = jackson.parseJson(s"""{"name":"with date and name","date":"$ds"}""")
      write(new WithDateAndName("with date and name", pd)) must_== js
    }

    "write a regular class with a val definition and use the data from json" in {
      val js = jackson.parseJson("""{"in":49}""")
      write(new ClassWithDef(49)) must_== js
    }

    "write a regular class with a val definition and use the default value" in {
      val js = jackson.parseJson("""{"in":4}""")
      write(new ClassWithDef()) must_== js
    }

    "write an object with a default complex object" in {
      val js = jackson.parseJson(s"""{"name":"junk with default","junk":$junkJson}""")
      write(ObjWithDefJunk("junk with default", junk)) must_== js
    }

    "write an object with a default complex object and use the default value" in {
      val js = jackson.parseJson(s"""{"name":"junk with default","junk":{"in1":-1,"in2":"Default"}}""")
      write(ObjWithDefJunk("junk with default")) must_== js
    }
  }
}