import java.io.InputStream
import java.net.URL
import java.util.Base64

import org.apache.commons.io.FilenameUtils


import org.apache.commons.io.IOUtils
import org.apache.commons.codec.digest.DigestUtils

import scala.util.Try

object FileUtils {

  def uploadFile(url:String):Option[String] = {
    val content = getByteContent(url)
    content match {
      case Some(c) => val re = S3Utils.uploadContent(extractFileName(url), url, c)
                      Some(re)
      case _ => None
    }
  }

  def getByteContent(url: String):Option[Array[Byte]] = {
    val input:Option[InputStream] = Try(new URL(url).openStream).toOption
    try {
      input.map(IOUtils.toByteArray(_))
    }
    finally {
      input.map(_.close())
    }
  }

  def sha256Hexa(content:Array[Byte]):String = DigestUtils.sha256Hex(content)
  def base64Encoded(content:Array[Byte]):Array[Byte] = Base64.getEncoder().encode(content)

  def extractFileName(url:String):String = FilenameUtils.getName(new URL(url).getPath())
}
