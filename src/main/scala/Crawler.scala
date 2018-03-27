import java.net.URL

import scala.util.Try
import cats._
import cats.instances._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

object Crawler {

  val logger = Logger(LoggerFactory.getLogger("crawler"))

  def extractUrls(commonTemplateUrls:List[String], resourceUrl:String, url:String, isAjax:Boolean):(List[String], List[String]) = {
    val allHrefs = DomUtils.extractOutLinks(resourceUrl, isAjax)
    val formattedUrls = DomUtils.formatUrls(url, allHrefs)

    val (resourceUrls, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)

    //Removing the template logic
    val validUrls = htmlUrls //.diff(commonTemplateUrls)

    val sameDomainUrls = DomUtils.filterSubdomainUrls(url, validUrls)
    (DomUtils.filterPDFUrls(resourceUrls), sameDomainUrls)
  }


  def extractFiles(resourceUrl:String, maxDepth:Int, maxNeedFiles:Int, isAjax:Boolean):Future[List[String]] = {
    Try(new URL(resourceUrl)).toOption match {
      case Some(_) =>
        val allHrefs = DomUtils.extractOutLinks(resourceUrl, isAjax)

        val formattedUrls = DomUtils.formatUrls(resourceUrl, allHrefs)
        val (_, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)
        val sameDomainUrls = DomUtils.filterSubdomainUrls(resourceUrl, htmlUrls)

        val randomSamples = DomUtils.randomSampleUrls(15, sameDomainUrls)
        val templateLinks = DomUtils.getCommonTemplateUrls(randomSamples)
        val formattedTemplateLinks = DomUtils.formatUrls(resourceUrl, templateLinks)

        logger.info(s"For resource URL => $resourceUrl, out of random samples => $randomSamples, found template urls are => $formattedTemplateLinks")

        val filesQueue = scala.collection.mutable.Set[String]()
        val processedUrls = scala.collection.mutable.Set[String]()

        var urlQueue1 = scala.collection.mutable.ListBuffer[String](resourceUrl)
        var urlQueue2 = scala.collection.mutable.ListBuffer[String]()
        var flip = true

        def runCrawl(queue1:mutable.ListBuffer[String], queue2:mutable.ListBuffer[String], fQueue:mutable.Set[String], processedQueue:mutable.Set[String]):Unit =
          queue1.distinct.map(targetUrl => {
            if(fQueue.size < maxNeedFiles){
              logger.info(s"Going to process url => $targetUrl ")
              if(fQueue.size < maxNeedFiles && !processedQueue.contains(targetUrl)){
                val (pdfs, sameDomainUrls)= extractUrls(formattedTemplateLinks, resourceUrl, targetUrl, isAjax)
                logger.info(s"extracted pdf urls from resource url => $resourceUrl, pdfs => $pdfs")
                pdfs.map(filesQueue.add(_))
                if(!pdfs.isEmpty) {
                  pdfs.grouped(10).toList.map(c => BFRedisClient.publishFileUrlsToRedis(c, resourceUrl, targetUrl, false))
                }
                sameDomainUrls.map(queue2.append(_))
              }
              logger.info(s"Total number of processed urls for resource url => $resourceUrl with count => ${processedUrls.size}")
              processedQueue.add(targetUrl)
            }
          })



        (1 to maxDepth).foreach(_ => {
          if(filesQueue.size < maxNeedFiles){
            if(flip){
              runCrawl(urlQueue1, urlQueue2, filesQueue, processedUrls)
              urlQueue1 = scala.collection.mutable.ListBuffer[String]()
              flip = false
            }else{
              runCrawl(urlQueue2, urlQueue1, filesQueue, processedUrls)
              urlQueue2 = scala.collection.mutable.ListBuffer[String]()
              flip = true
            }
          }
        })
        logger.info(s"Fetching completed for url => $resourceUrl")
        BFRedisClient.publishFileUrlsToRedis(Nil, resourceUrl, resourceUrl, true)
        Future{filesQueue.toList}

      case _ => logger.info(s"Added invalid URL => $resourceUrl")
                Future{Nil}
    }
  }
}
