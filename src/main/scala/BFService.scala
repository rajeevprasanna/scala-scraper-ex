

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model._
import AppContext._
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success}
import Models._

import spray.json._
import SecureKeys._
import scala.concurrent.ExecutionContext.Implicits.global

object BFService {

  implicit def materializer:ActorMaterializer = ActorMaterializer()

  implicit  def toByteString = (s:String) => ByteString(s)

  private def payload(params:String) = HttpEntity.Strict(
    contentType = ContentTypes.`application/json`,
    data = params
  )

  def uploadFilesMetadata(sourceUrl:String, files:List[FileMetaData]):Unit = {
    println(s"uploading file metadata => $files")
    import FilePersistencePayloadJsonProtocol._
    val filePersistencePayload:String = FilePersistencePayload(sourceUrl, files, BF_API_SECRET).toJson.toString
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(POST, uri = SAVE_FILE_META_DATA, entity = payload(filePersistencePayload)))
    responseFuture
      .onComplete {
        case Success(res) => println(res)
        case Failure(_)   => sys.error("something wrong")
      }
  }

  def markProcessComplete(sourceUrl:String):Unit = {
    println(s"marking processing completion for url => $sourceUrl")
    import ProcessingCompletePayloadJsonProtocol._
    val urlProcessingCompletedPayload:String = ProcessingCompletePayload(BF_API_SECRET, List(sourceUrl)).toJson.toString
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(POST, uri = MARK_CRAWL_COMPLETED_FOR_URL, entity = payload(urlProcessingCompletedPayload)))
    responseFuture
      .onComplete {
        case Success(res) => println(res)
        case Failure(_)   => sys.error("something wrong")
      }
  }

  private def respToString:(ResponseEntity => Future[String]) = (entity:ResponseEntity) => entity.dataBytes.runFold(ByteString.empty) { case (acc, b) => acc ++ b }.map(_.utf8String)
  def filterProcessedUrls(sourceUrl:String, urls:List[String]):Future[List[String]] = {
    println(s"filter processing for urls => $urls")
    urls match {
      case _ if urls.isEmpty => Future{Nil}
      case _ =>
          import ProcessedFilesPayloadJsonProtocol._
          val filterProcessedFilesPayload:String = ProcessedFilesPayload(BF_API_SECRET, urls, sourceUrl).toJson.toString
          val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(POST, uri = FILTER_ALREADY_CRAWLED_URLS, entity = payload(filterProcessedFilesPayload)))
          val result:Future[List[String]] = responseFuture
            .flatMap(res => {
              import FilteredUrlsProtocol._
              val urlsResp:Future[List[String]] =
                respToString(res.entity).flatMap(str => {
                  val urls: List[String] = JsonParser(str).convertTo[FilteredUrls].urls
                  println(s"urls after filtering => $urls")
                  Future{urls}
                })
              urlsResp
            })
          result
      }
    }
}
