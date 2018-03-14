import java.util.UUID

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata

import scala.collection.JavaConversions._
import java.io.{ByteArrayInputStream, InputStream}

import SecureKeys._

object S3Utils {

  def uploadContent(fileName:String, fileUrl:String, content:Array[Byte]):String = {
    val yourAWSCredentials = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)
    val amazonS3Client = new AmazonS3Client(yourAWSCredentials)
    val s3_id = UUID.randomUUID().toString

    val userMetadataMap = Map("filename" -> fileName, "fileurl" -> fileUrl)
    val metadata: ObjectMetadata = new ObjectMetadata()
    metadata.setUserMetadata(userMetadataMap)

    val inputStream:InputStream = new ByteArrayInputStream(content, 0, content.length)
    amazonS3Client.putObject(BUCKET_NAME, s3_id, inputStream, metadata)
    println(s"uploaded file url => $fileUrl with key => $s3_id")
    s3_id
  }

}
