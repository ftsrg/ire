import java.io.{ByteArrayOutputStream, FileOutputStream}

import com.esotericsoftware.kryo.io.Output
import com.twitter.chill.{Input, ScalaKryoInstantiator}
import org.scalatest.{BeforeAndAfterAll, WordSpec}

/**
 * Created by janosmaginecz on 05/05/15.
 */
class WildcardInputTest extends WordSpec with BeforeAndAfterAll {
  "WildcardInput" must {
    "store attributes" in {
      val input = new WildcardInput
      input.addAttribute("_6", "length", 1L)
      input.addAttribute("_6", "length", 2L)
      assert(input.attributes("length")(6) == Set(1L, 2L))
    }
    "store types" in {
      val input = new WildcardInput
      input.addAttribute("_6", "type", "train")
      input.addAttribute("_7", "type", "elephant")
      assert(input.types("train") == Set(6L))

    }
    "do grouping" in {
      val input = new WildcardInput
      for (
        i <- 1 to 3
      ) {
        input.addAttribute("_6", "length", i)
        input.addAttribute("_" + i.toString, "type", "rain")
      }
      val attributeChangeSetSizes = collection.parallel.mutable.ParHashSet[Long]()
      val typeChangeSetSizes = collection.parallel.mutable.ParHashSet[Long]()
      input.sendData(
        attributeFunc = Map(
          "length" -> ((a: ChangeSet) => {
            attributeChangeSetSizes += a.positive.size
          })
        ),
        typeFunc = Map(
          "rain" -> ((a: ChangeSet) => {
            typeChangeSetSizes += a.positive.size
          })
        ),
        messageSize = 2
      )
      /*collection.grouped does not necessarily fill the first group, so if there are 3 elements (messageSize=2),
      they might come as 2,1 or 1,2
       */
      assert(attributeChangeSetSizes == Set(1, 2))
      assert(typeChangeSetSizes == Set(1, 2))
    }
    "be able to serialize and deserialize" in {
      val inputNode = new WildcardInput
      inputNode.addAttribute("_6", "length", 1L)
      inputNode.addAttribute("_6", "length", 2L)
      inputNode.addAttribute("_6", "type", "train")
      val instantiator = new ScalaKryoInstantiator
      instantiator.setRegistrationRequired(false)

      val kryo = instantiator.newKryo()
      val baos = new ByteArrayOutputStream
      val output = new Output(baos, 4096)
      kryo.writeObject(output, inputNode)
      val deserialized = kryo.readObject(new Input(baos.toByteArray), classOf[WildcardInput])
      assert(deserialized.attributes("length")(6L).size == 2)
      assert(deserialized.types("train").size == 1)
    }
  }
}
