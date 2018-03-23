import java.net.URL

import scala.util.Try
import cats._
import cats.instances._

object Crawler {

  def extractUrls(commonTemplateUrls:List[String], resourceUrl:String, url:String, isAjax:Boolean):(List[String], List[String]) = {
    val doc = DomUtils.fetchDocument(url, isAjax)
    val allHrefs = doc.map(DomUtils.getUrlsFromDoc(_)).getOrElse(Nil)
    val formattedUrls = DomUtils.formatUrls(url, allHrefs)

    val (resourceUrls, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)

    //Removing the template logic
    val validUrls = htmlUrls //.diff(commonTemplateUrls)

    val sameDomainUrls = DomUtils.filterSubdomainUrls(url, validUrls)
    (DomUtils.filterPDFUrls(resourceUrls), sameDomainUrls)
  }


  def extractFiles(resourceUrl:String, maxDepth:Int, maxNeedFiles:Int, isAjax:Boolean):List[String] = {
    Try(new URL(resourceUrl)).toOption match {
      case Some(_) =>
        val doc = DomUtils.fetchDocument(resourceUrl, isAjax)
        val allHrefs = doc.map(DomUtils.getUrlsFromDoc(_)).getOrElse(Nil)

        val formattedUrls = DomUtils.formatUrls(resourceUrl, allHrefs)
        val (_, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)
        val sameDomainUrls = DomUtils.filterSubdomainUrls(resourceUrl, htmlUrls)

        val randomSamples = DomUtils.randomSampleUrls(15, sameDomainUrls)
        val templateLinks = DomUtils.getCommonTemplateUrls(randomSamples)
        val formattedTemplateLinks = DomUtils.formatUrls(resourceUrl, templateLinks)

        println(s"For resource URL => $resourceUrl, out of random samples => $randomSamples, found template urls are => $formattedTemplateLinks")

        val filesQueue = scala.collection.mutable.Set[String]()
        val processedUrls = scala.collection.mutable.Set[String]()

        var urlQueue1 = scala.collection.mutable.ListBuffer[String](resourceUrl)
        var urlQueue2 = scala.collection.mutable.ListBuffer[String]()
        var flip = true

        (1 to maxDepth).foreach(_ => {
          if(filesQueue.size < maxNeedFiles){
            if(flip){
              urlQueue1.distinct.map(targetUrl => {
                if(filesQueue.size < maxNeedFiles){
                  println(s"Going to process url => $targetUrl ")
                  if(filesQueue.size < maxNeedFiles && !processedUrls.contains(targetUrl)){
                    val (pdfs, sameDomainUrls)= extractUrls(formattedTemplateLinks, resourceUrl, targetUrl, isAjax)
                    println(s"extracted pdf urls from resource url => $resourceUrl, pdfs => $pdfs")
                    pdfs.map(filesQueue.add(_))
                    if(!pdfs.isEmpty) {
                      pdfs.grouped(10).toList.map(c => BFRedisClient.publishFileUrlsToRedis(c, resourceUrl, targetUrl, false))
                    }

                    sameDomainUrls.map(urlQueue2.append(_))
                  }
                  println(s"Total number of processed urls for resource url => $resourceUrl with count => ${processedUrls.size}")
                  processedUrls.add(targetUrl)
                }
              })
              urlQueue1 = scala.collection.mutable.ListBuffer[String]()
              flip = false
            }else{
              urlQueue2.distinct.map(targetUrl => {
                if(filesQueue.size < maxNeedFiles){
                  println(s"Going to process url => $targetUrl ")
                  if(filesQueue.size < maxNeedFiles  && !processedUrls.contains(targetUrl)){
                    val (pdfs, sameDomainUrls)= extractUrls(formattedTemplateLinks, resourceUrl, targetUrl, isAjax)
                    println(s"extracted pdf urls from resource url => $resourceUrl, pdfs => $pdfs")
                    pdfs.map(filesQueue.add(_))
                    if(!pdfs.isEmpty) {
                      pdfs.grouped(10).toList.map(c => BFRedisClient.publishFileUrlsToRedis(c, resourceUrl, targetUrl, false))
                    }
                    sameDomainUrls.map(urlQueue1.append(_))
                  }
                  println(s"Total number of processed urls for resource url => $resourceUrl with count => ${processedUrls.size}")
                  processedUrls.add(targetUrl)
                }
              })
              urlQueue2 = scala.collection.mutable.ListBuffer[String]()
              flip = true
            }
          }
        })
        println(s"Fetching completed for url => $resourceUrl")
        BFRedisClient.publishFileUrlsToRedis(Nil, resourceUrl, resourceUrl, true)
        filesQueue.toList

      case _ => println(s"Added invalid URL => $resourceUrl")
                Nil
    }
  }
}
