
object Crawler {

  def extractUrls(commonTemplate:String, resourceUrl:String, url:String):(List[String], List[String]) = {
    val doc = DomUtils.fetchDocument(url)
    val allHrefs = doc.map(DomUtils.getUrlsFromDoc(_)).getOrElse(Nil)

    val commonHTMLDom = DomUtils.parseString(commonTemplate)
    val commonUrls = DomUtils.getUrlsFromDoc(commonHTMLDom)

    val validUrls = allHrefs.diff(commonUrls)

    val formattedUrls = DomUtils.formatUrls(url, validUrls)
    val (resourceUrls, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)
    val sameDomainUrls = DomUtils.filterSubdomainUrls(url, htmlUrls)
    (DomUtils.filterPDFUrls(resourceUrls), sameDomainUrls)
  }


  def extractFiles(url:String, maxDepth:Int, maxNeedFiles:Int):List[String] = {
    val doc = DomUtils.fetchDocument(url)
    val allHrefs = doc.map(DomUtils.getUrlsFromDoc(_)).getOrElse(Nil)

    val formattedUrls = DomUtils.formatUrls(url, allHrefs)
    val (_, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)
    val sameDomainUrls = DomUtils.filterSubdomainUrls(url, htmlUrls)

    val randomSamples = DomUtils.randomSampleUrls(5, sameDomainUrls)
    val commonHtml = DomUtils.commonPartsOfTemplate(randomSamples).getOrElse("")

    val filesQueue = scala.collection.mutable.Set[String]()
    val processedUrls = scala.collection.mutable.Set[String]()

    var urlQueue1 = scala.collection.mutable.ListBuffer[String](url)
    var urlQueue2 = scala.collection.mutable.ListBuffer[String]()
    var flip = true

    (1 to maxDepth).foreach(_ => {
      if(filesQueue.size < maxNeedFiles){
        if(flip){
          urlQueue1.distinct.map(targetUrl => {
            println(s"Going to process url => $targetUrl ")
            if(filesQueue.size < maxNeedFiles && !processedUrls.contains(targetUrl)){
              val (pdfs, sameDomainUrls)= extractUrls(commonHtml, url, targetUrl)
              pdfs.map(filesQueue.add(_))
              if(!pdfs.isEmpty) {
               pdfs.grouped(10).map(BFRedisClient.publishFileUrlsToRedis(_, url, false))
              }

              sameDomainUrls.map(urlQueue2.append(_))
            }
            processedUrls.add(targetUrl)
          })
          urlQueue1 = scala.collection.mutable.ListBuffer[String]()
          flip = false
        }else{
          urlQueue2.distinct.map(targetUrl => {
            println(s"Going to process url => $targetUrl ")
            if(filesQueue.size < maxNeedFiles  && !processedUrls.contains(targetUrl)){
              val (pdfs, sameDomainUrls)= extractUrls(commonHtml, url, targetUrl)
              pdfs.map(filesQueue.add(_))
              if(!pdfs.isEmpty) {
                pdfs.grouped(10).map(BFRedisClient.publishFileUrlsToRedis(_, url, false))
              }

              sameDomainUrls.map(urlQueue1.append(_))
            }
            processedUrls.add(targetUrl)
          })
          urlQueue2 = scala.collection.mutable.ListBuffer[String]()
          flip = true
        }
      }
    })
    println(s"Fetching completed for url => $url")
    BFRedisClient.publishFileUrlsToRedis(Nil, url, true)
    filesQueue.toList
  }
}
