import java.io.InputStream
import java.net.URL
import java.util.Base64

import org.apache.commons.io.FilenameUtils


import org.apache.commons.io.IOUtils
import org.apache.commons.codec.digest.DigestUtils

import scala.util.Try
import Models._

object FileUtils {


  def uploadResource(url:String):Option[FileMetaData] = {
    println(s"uploading resource from url => $url")
    getByteContent(url).map(content => {
      val fileName = extractFileName(url)
      val contentId = sha256Hexa(content)
      val s3Id = uploadFileToS3(base64Encoded(content), url)
      FileMetaData(fileName, contentId, s3Id, url)
    })
  }



  private def uploadFileToS3(content:Array[Byte], url:String):String = S3Utils.uploadContent(extractFileName(url), url, content)

  private def getByteContent(url: String):Option[Array[Byte]] = {
    val input:Option[InputStream] = Try(new URL(url).openStream).toOption
    try {
      input.map(IOUtils.toByteArray(_))
    }
    finally {
      input.map(_.close())
    }
  }

  private def sha256Hexa(content:Array[Byte]):String = DigestUtils.sha256Hex(content)
  private def base64Encoded(content:Array[Byte]):Array[Byte] = Base64.getEncoder().encode(content)
  private def extractFileName(url:String):String = FilenameUtils.getName(new URL(url).getPath())
}
