package languageServer

import core.language.Language
import jsonRpc.{JsonRpcConnection, LazyLogging, SerialWorkQueue, WorkItem}
import lsp.SharedLSPServer

import scala.util.Try

trait LanguageBuilder {
  def key: String
  def build(arguments: collection.Seq[String]): Language
}

class LanguageServerMain(builders: Seq[LanguageBuilder],
                         connection: JsonRpcConnection,
                         workQueue: SerialWorkQueue[WorkItem]) extends LazyLogging {

  val languageMap = builders.map(l => (l.key, l)).toMap

  def main(args: Array[String]): Unit = {

    val languageOption = getLanguage(args)
    languageOption.foreach(language => {
      logger.debug(s"Starting server in ${System.getenv("PWD")}")
      val lspServer = Try {
        val languageServer = new MiksiloLanguageServer(language)
        new SharedLSPServer(languageServer, connection, workQueue)
      }
      lspServer.recover{case e => logger.error(e.getMessage); e.printStackTrace() }
      connection.listen()
    })
  }

  def getLanguage(args: collection.Seq[String]): Option[Language] = {
    if (builders.size == 1) {
      Some(builders.head.build(args))
    } else {
      if (args.isEmpty) {
        logger.debug("Please specify with which language to run Miksilo")
        return None
      }

      val languageOption = languageMap.get(args.head)
      languageOption match {
        case None =>
          logger.debug("Please specify with which language to run Miksilo")
          None
        case Some(languageBuilder) =>
          Some(languageBuilder.build(args.drop(1)))
      }
    }
  }
}
