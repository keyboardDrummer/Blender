package miksilo.lspprotocol.jsonRpc

import java.io.InputStream

import miksilo.editorParser.LazyLogging

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
  * A Language Server message Reader. It expects the following format:
  *
  * <Header> '\r\n' <Content>
  *
  * Header := FieldName ':' FieldValue '\r\n'
  *
  * Currently there are two defined header fields:
  * - 'Content-Length' in bytes (required)
  * - 'Content-Type' (string), defaults to 'application/vscode-jsonrpc; charset=utf8'
  *
  * @note The header part is defined to be ASCII encoded, while the content part is UTF8.
  */
class JVMMessageReader(in: InputStream) extends MessageReader with LazyLogging {
  val BufferSize = 8192

  private val buffer = new Array[Byte](BufferSize)
  @volatile
  private var data = ArrayBuffer.empty[Byte]
  @volatile
  private var streamClosed = false

  private val lock = new Object

  private var started = false

  private class PumpInput extends Thread("Input Reader") {
    override def run(): Unit = {
      var nRead = 0
      do {
        nRead = in.read(buffer)
        if (nRead > 0) lock.synchronized {
          data ++= buffer.slice(0, nRead)
          lock.notify()
        }
      } while (nRead > 0)
      logger.info("End of stream, terminating thread")
      lock.synchronized {
        streamClosed = true
        lock.notify() // some threads might be still waiting for input
      }
    }
  }

  /**
    * Return headers, if any are available. It returns only full headers, after the
    * \r\n\r\n mark has been seen.
    *
    * @return A map of headers. If the map is empty it could be that the input stream
    *         was closed, or there were no headers before the delimiter. You can disambiguate
    *         by checking {{{this.streamClosed}}}
    */
  private final def readHeaders(): Map[String, String] = lock.synchronized {
    val EmptyPair = "" -> ""
    val EmptyMap = Map.empty[String, String]
    def atDelimiter(idx: Int): Boolean = {
      (data.size >= idx + 4
        && data(idx) == '\r'
        && data(idx + 1) == '\n'
        && data(idx + 2) == '\r'
        && data(idx + 3) == '\n')
    }

    while (data.size < 4 && !streamClosed) lock.wait()

    if (streamClosed) return EmptyMap

    var i = 0
    while (i + 4 < data.size && !atDelimiter(i)) {
      i += 1
    }

    if (atDelimiter(i)) {
      val headers = new String(data.slice(0, i).toArray, MessageReader.AsciiCharset)
      logger.debug(s"Received headers:\n$headers")

      val pairs = headers.split("\r\n").filter(_.trim.length() > 0) map { line =>
        line.split(":") match {
          case Array(key, value) => key.trim -> value.trim
          case _ =>
            logger.error(s"Malformed input: $line")
            EmptyPair
        }
      }

      // drop headers
      data = data.drop(i + 4)

      // if there was a malformed header we keep trying to re-sync and read again
      if (pairs.contains(EmptyPair)) {
        logger.error("There was an empty pair in $pairs, trying to read another header.")
        readHeaders()
      } else pairs.toMap
    } else if (streamClosed) {
      EmptyMap
    } else {
      lock.wait()
      readHeaders()
    }
  }

  /**
    * Return `len` bytes of content as a string encoded in UTF8.
    *
    * @note If the stream was closed this method returns the empty string.
    */
  private def getContent(len: Int): String = lock.synchronized {
    while (data.size < len && !streamClosed) lock.wait()

    if (streamClosed) ""
    else {
      assert(data.size >= len)
      val content = data.take(len).toArray
      data = data.drop(len)
      new String(content, MessageReader.Utf8Charset)
    }
  }

  /**
    * Return the next JSON RPC content payload. Blocks until enough data has been received.
    */
  def nextPayload(): Future[String] = {
    if (!started) {
      started = true
      (new PumpInput).start()
    }

    if (streamClosed) Future.successful(null)

    else {
      // blocks until headers are available
      val headers = readHeaders()

      if (headers.isEmpty && streamClosed)
        Future.successful(null)
      else {
        val length = headers.get("Content-Length") match {
          case Some(len) => try len.toInt catch { case e: NumberFormatException => -1 }
          case _ => -1
        }

        if (length > 0) {
          val content = getContent(length)
          if (content.isEmpty && streamClosed) Future.successful(null) else Future.successful(content)
        } else {
          logger.error("Input must have Content-Length header with a numeric value.")
          nextPayload()
        }
      }
    }
  }
}

