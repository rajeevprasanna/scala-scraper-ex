import java.io.InputStream
import java.net.URL
import java.util.Base64
import java.net.URLDecoder

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.codec.digest.DigestUtils

import scala.util.Try
import Models._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import URLExtensions._
import ExtensionUtils._


import scala.concurrent.{Future, Promise}

object FileUtils extends AppContext {

  val logger = Logger(LoggerFactory.getLogger("FileUtils"))

  def uploadResource(url:String, pageUrl:String):Future[Option[FileMetaData]] = {
    logger.debug(s"triggering resource upload process to S3 from url => $url")
    val p = Promise[Option[FileMetaData]]()
    Future{
      //TODO: check content type is pdf or not
      val metadata = getByteContent(url).flatMap(content => {
        val fileName = extractFileName(url)
        val contentId = sha256Hexa(content)
        val res:Option[FileMetaData] = uploadFileToS3(base64Encoded(content), url, pageUrl).map(s3Id => FileMetaData(fileName, contentId, s3Id, url.trim))
        res
      })
      p.success(metadata)
    }
    p.future
  }

  private def uploadFileToS3(content:Array[Byte], fileUrl:String, pageUrl:String):Option[String] = S3Utils.uploadContent(extractFileName(fileUrl), fileUrl, pageUrl, content)

  private def getByteContent(url: String):Option[Array[Byte]] = {

    def getContent():Option[Array[Byte]] = {
      val httpcon = new URL(url).openConnection()
      httpcon.addRequestProperty("User-Agent", "Mozilla/4.76")
      if(httpcon.getContentType == "application/pdf"){
        val input:Option[InputStream] = Try(httpcon.getInputStream()).toOption
        try {
          input.map(IOUtils.toByteArray(_))
        }
        finally {
          input.map(_.close())
        }
      }else{
        val headerUrl = httpcon.getHeaderField("Location")
        if(headerUrl != null && headerUrl.isPdfUrl &&  headerUrl != url){
          logger.debug(s"trying to download from redirect url => $headerUrl for actual url => $url")
          getByteContent(headerUrl)
        }else{
          logger.error(s"received content type different for url => $url and headerUrl => $headerUrl")
          None
        }
      }
    }
    Try(getContent()).processTry(s"Error in fetching content from url => $url").getOrElse(None)
  }

  private def sha256Hexa(content:Array[Byte]):String = DigestUtils.sha256Hex(content)
  private def base64Encoded(content:Array[Byte]):Array[Byte] = Base64.getEncoder().encode(content)
  private def extractFileName(url:String):String =
    URLDecoder.decode(FilenameUtils.getName(new URL(url).getPath()), "utf-8")

}
