import java.util.UUID

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata

import scala.collection.JavaConversions._
import java.io.{ByteArrayInputStream, InputStream}

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.util.Try

object S3Utils {

  val logger = Logger(LoggerFactory.getLogger("S3Utils"))

  def uploadContent(fileName:String, fileUrl:String, pageUrl:String, content:Array[Byte]):Option[String] = {
    val yourAWSCredentials = new BasicAWSCredentials(ConfReader.S3_ACCESS_KEY, ConfReader.S3_SECRET_KEY)
    val amazonS3Client = new AmazonS3Client(yourAWSCredentials)
    val s3_id = UUID.randomUUID().toString

    val userMetadataMap = Map("filename" -> fileName, "fileurl" -> fileUrl, "pageurl" -> pageUrl)
    val metadata: ObjectMetadata = new ObjectMetadata()
    metadata.setContentLength(content.length)
    metadata.setUserMetadata(userMetadataMap)

    Try {
      val inputStream:InputStream = new ByteArrayInputStream(content, 0, content.length)
      amazonS3Client.putObject(ConfReader.S3_BUCKET_NAME, s3_id, inputStream, metadata)
      logger.info(s"uploaded file url => $fileUrl with key => $s3_id")
      s3_id
    }.toOption
  }

}
