import java.io.InputStream
import java.net.URL
import java.util.Base64
import java.net.URLDecoder

import org.apache.commons.io.FilenameUtils


import org.apache.commons.io.IOUtils
import org.apache.commons.codec.digest.DigestUtils

import scala.util.Try
import Models._

object FileUtils {


  def uploadResource(url:String, pageUrl:String):Option[FileMetaData] = {
    println(s"uploading resource to S3 from url => $url")
    //TODO: check content type is pdf or not
    getByteContent(url).flatMap(content => {
      val fileName = extractFileName(url)
      val contentId = sha256Hexa(content)
      val res:Option[FileMetaData] = uploadFileToS3(base64Encoded(content), url, pageUrl).map(s3Id => FileMetaData(fileName, contentId, s3Id, url.trim))
      res
    })
  }



  private def uploadFileToS3(content:Array[Byte], fileUrl:String, pageUrl:String):Option[String] = S3Utils.uploadContent(extractFileName(fileUrl), fileUrl, pageUrl, content)

  private def getByteContent(url: String):Option[Array[Byte]] = {
    val httpcon = new URL(url).openConnection()
    httpcon.addRequestProperty("User-Agent", "Mozilla/4.76")
    val input:Option[InputStream] = Try(httpcon.getInputStream()).toOption
    try {
      input.map(IOUtils.toByteArray(_))
    }
    finally {
      input.map(_.close())
    }
  }

  private def sha256Hexa(content:Array[Byte]):String = DigestUtils.sha256Hex(content)
  private def base64Encoded(content:Array[Byte]):Array[Byte] = Base64.getEncoder().encode(content)
  private def extractFileName(url:String):String =
    URLDecoder.decode(FilenameUtils.getName(new URL(url).getPath()), "utf-8")

}
