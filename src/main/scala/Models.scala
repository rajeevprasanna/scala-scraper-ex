
object Models {
  import spray.json._

  case class FileMetaData(name:String, content_id:String, s3_id:String, url:String)
  object FileMetaDataJsonProtocol extends DefaultJsonProtocol {
    implicit val fileMetaDataFormat = jsonFormat4(FileMetaData.apply)
  }

  case class FilePersistencePayload(source_url:String, files:List[FileMetaData], session_id:String)
  object FilePersistencePayloadJsonProtocol extends DefaultJsonProtocol {
    import FileMetaDataJsonProtocol._
    implicit val filePersistencePayloadFormat = jsonFormat3(FilePersistencePayload.apply)
  }

  case class ProcessingCompletePayload(session_id:String, urls:List[String])
  object ProcessingCompletePayloadJsonProtocol extends DefaultJsonProtocol {
    implicit val processingCompletePayloadFormat = jsonFormat2(ProcessingCompletePayload.apply)
  }

  case class ProcessedFilesPayload(session_id:String, urls:List[String], source_url:String)
  object ProcessedFilesPayloadJsonProtocol extends DefaultJsonProtocol {
    implicit val CrawledFilesPayloaddFormat = jsonFormat3(ProcessedFilesPayload.apply)
  }

  case class FilteredUrls(urls:List[String])
  object FilteredUrlsProtocol extends DefaultJsonProtocol {
    implicit val filteredUrlsFormat = jsonFormat1(FilteredUrls.apply)
  }
}
