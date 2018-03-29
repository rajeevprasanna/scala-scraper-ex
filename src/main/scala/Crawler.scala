import java.net.URL

import akka.stream.scaladsl.{Sink, Source}
import scala.util.Try
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.{Future, Promise}


object Crawler extends AppContext {

  val logger = Logger(LoggerFactory.getLogger("crawler"))

  def extractUrls(commonTemplateUrls:List[String], resourceUrl:String, url:String, isAjax:Boolean):(List[String], List[String]) = {
    val allHrefs = DomUtils.extractOutLinks(url, isAjax)
    val formattedUrls = DomUtils.formatUrls(url, allHrefs)

    val (resourceUrls, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)

    logger.debug(s"found html outlinks from page => $url, urls are => $htmlUrls and file urls are => $resourceUrls")

    //Removing the template logic
    val validUrls = htmlUrls //.diff(commonTemplateUrls)

    val sameDomainUrls = DomUtils.filterSubdomainUrls(url, validUrls)
    (DomUtils.filterPDFUrls(resourceUrls), sameDomainUrls)
  }


  def extractFiles(resourceUrl:String, maxDepth:Int, maxNeedFiles:Int, isAjax:Boolean):Future[Unit] = {
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
        val parallelCount = if(isAjax) 1 else 5

        def runCrawl(queue1: mutable.ListBuffer[String], depth: Int): Future[mutable.ListBuffer[String]] = {
            val queue2 = mutable.ListBuffer[String]()
            val res:Future[_] = Source.fromIterator(() => queue1.toIterator).mapAsyncUnordered(parallelCount) { targetUrl: String => {
                                val p = Promise[Unit]()
                                Future{
                                  if (filesQueue.size < maxNeedFiles && !processedUrls.contains(targetUrl)) {
                                      logger.info(s"Going to process url => $targetUrl at the depth => $depth")
                                      val (pdfs, sameDomainUrls) = extractUrls(formattedTemplateLinks, resourceUrl, targetUrl, isAjax)
                                      logger.info(s"extracted pdf urls from resource url => $resourceUrl, pdfs => $pdfs")
                                      pdfs.map(filesQueue.add(_))
                                      if (!pdfs.isEmpty) {
                                        pdfs.grouped(10).toList.map(c => BFRedisClient.publishFileUrlsToRedis(c, resourceUrl, targetUrl, false))
                                      }
                                      logger.debug(s"same domain urls extracted from page => $targetUrl are => $sameDomainUrls")
                                      sameDomainUrls.map(queue2.append(_))
                                      logger.info(s"At depth => $depth, Total number of processed urls for resource url => $resourceUrl with count => ${processedUrls.size}")
                                    processedUrls.add(targetUrl)
                                  }else{
                                    logger.info(s"At depth => $depth, Already fetched required files. count => ${filesQueue.size} or found processed Url => $targetUrl")
                                  }
                                  p.success(Unit)
                                }
                                p.future
                              }
          }.runWith(Sink.ignore)
          res.map(_ => queue2)
        }

        def crawl(depth:Int, queue1: mutable.ListBuffer[String]):Future[Unit] = depth match {
          case _ if depth > maxDepth =>
            logger.info(s"Fetching completed for url => $resourceUrl with total files count => ${filesQueue.size}")
            logger.debug(s"processed urls => $processedUrls")
            BFRedisClient.publishFileUrlsToRedis(Nil, resourceUrl, resourceUrl, true)
            Future{} //Exit


          case _  if filesQueue.size < maxNeedFiles =>
                        logger.info(s"Going to depth => $depth for resource url => $resourceUrl")
                        return runCrawl(queue1, depth).map{queue2 =>
                          return crawl(depth+1, queue2.distinct)
                        }
        }

        crawl(0, mutable.ListBuffer[String](resourceUrl))

      case _ => logger.info(s"Added invalid URL => $resourceUrl")
                Future{}
    }
  }
}
