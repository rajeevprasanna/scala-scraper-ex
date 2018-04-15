import java.net.URL

import akka.stream.scaladsl.{Sink, Source}
import scala.util.Try
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

import ExtensionUtils._


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
    logger.info(s"Going to start crawling for resourceUrl => $resourceUrl")

    Try(new URL(resourceUrl)).processTry(s"Error in resourceUrl parsing. url => $resourceUrl") match {
      case Some(_) =>
        val allHrefs = DomUtils.extractOutLinks(resourceUrl, isAjax)

        val formattedUrls = DomUtils.formatUrls(resourceUrl, allHrefs)
        val (_, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)
        val sameDomainUrls = DomUtils.filterSubdomainUrls(resourceUrl, htmlUrls)

        val randomSamples = DomUtils.randomSampleUrls(15, sameDomainUrls)
        val templateLinks = DomUtils.getCommonTemplateUrls(randomSamples)
        val formattedTemplateLinks = DomUtils.formatUrls(resourceUrl, templateLinks)

        logger.debug(s"For resource URL => $resourceUrl, out of random samples => $randomSamples, found template urls are => $formattedTemplateLinks")

        val filesQueue = scala.collection.mutable.Set[String]()
        val processedUrls = scala.collection.mutable.Set[String]()
        val parallelCount = if(isAjax) ConfReader.ajaxParallelCount else ConfReader.nonAjaxParallelCount

        def runCrawl(queue1: mutable.ListBuffer[String], depth: Int): Future[mutable.ListBuffer[String]] = {

            val queue2 = mutable.ListBuffer[String]()
            val res:Future[_] = Source.fromIterator(() => queue1.toIterator).mapAsyncUnordered(parallelCount) { targetUrl: String => {
                                val p = Promise[Unit]()
                                Future{
                                  Try{
                                    if (filesQueue.size < maxNeedFiles && !processedUrls.contains(targetUrl) && processedUrls.size < ConfReader.maxCrawlPages) {
                                      logger.debug(s"Going to process ${if(isAjax) "ajax" else  ""} url => $targetUrl at the depth => $depth. downloaded files count => ${filesQueue.size}")
                                      val (pdfs, sameDomainUrls) = extractUrls(formattedTemplateLinks, resourceUrl, targetUrl, isAjax)
                                      logger.debug(s"extracted pdf urls from resource url => $resourceUrl, pdfs => $pdfs")
                                      pdfs.map(filesQueue.add(_))
                                      if (!pdfs.isEmpty) {
                                        pdfs.grouped(10).toList.map(c => BFRedisClient.publishFileUrlsToRedis(c, resourceUrl, targetUrl, false))
                                      }
                                      logger.debug(s"same domain urls extracted from page => $targetUrl are => $sameDomainUrls")
                                      sameDomainUrls.map(queue2.append(_))
                                      logger.debug(s"At depth => $depth, Total number of processed urls for resource url => $resourceUrl with count => ${processedUrls.size} downloaded files count => ${filesQueue.size}")
                                      processedUrls.add(targetUrl)
                                    } else if(processedUrls.contains(targetUrl)) {
                                      logger.debug(s"At depth => $depth, found processed Url => $targetUrl")
                                    } else if(filesQueue.size > maxNeedFiles) {
                                      logger.info(s"At depth => $depth, Already fetched required files. count => ${filesQueue.size} for resourceUrl => $resourceUrl")
                                    }
                                  }
                                  p.success(Unit)
                                }
                                p.future
                              }
          }.runWith(Sink.ignore)
          res.map(_ => queue2)
        }

        def crawl(depth:Int, queue1: mutable.ListBuffer[String]):Future[Unit] = depth match {
          case _ if depth > maxDepth || filesQueue.size > maxNeedFiles  =>
            logger.info(s"Fetching completed for url => $resourceUrl with total files count => ${filesQueue.size}")
            BFRedisClient.publishFileUrlsToRedis(Nil, resourceUrl, resourceUrl, true)
             Future{} //Exit

          case _  =>    logger.info(s"Going to depth => $depth for resource url => $resourceUrl")
                        runCrawl(queue1, depth).flatMap(queue2 => crawl(depth+1, queue2.distinct))

        }

        crawl(0, mutable.ListBuffer[String](resourceUrl))

      case _ => logger.error(s"Added invalid URL => $resourceUrl")
                Future{}
    }
  }
}
