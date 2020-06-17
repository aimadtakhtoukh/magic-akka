package gatherer

import java.nio.file.{Files, Paths}

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.writePretty

import scala.concurrent.{ExecutionContextExecutor, Future}

object MagicCardInfoData extends App {
  implicit val formats: AnyRef with Formats = Serialization.formats(NoTypeHints)

  implicit val system: ActorSystem = ActorSystem("Scrapping_Jobs")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val path = Paths.get("C:/Users/Aimad/Desktop/magic/")
  if (Files.notExists(path)) { Files.createDirectories(path) }

  MagicSetSource.allMagicSetSource
    .mapAsyncUnordered(1) {
      edition => {
        val editionPath = path.resolve(s"${slug(edition)}.json")
        if (!Files.exists(editionPath)) {
          Source.single(edition)
            .via(GathererScrapper.flow())
            .map(writePretty(_))
            .reduce((acc : String, i : String) => s"$acc,\n$i")
            .map(json => s"[\n$json\n]")
            .recover {
              case t : Throwable =>
                t.getMessage
            }
            .map(ByteString(_))
            .runWith(FileIO.toPath(editionPath))
        } else {
          Future(Done)
        }
      }
    }
    .recover {
      case t : Throwable =>
        t.getMessage
    }
    .runForeach(println)
    .onComplete {
      _ => system.terminate()
    }

  private def slug(edition : String) : String =
    edition.toLowerCase.replaceAll("[^A-Za-z0-9]", "")
}
