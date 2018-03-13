import net.ruippeixotog.scalascraper.browser.JsoupBrowser

object Crawler {

  def extractUrls(commonTemplate:String, resourceUrl:String, url:String):(List[String], List[String]) = {
    val doc = DomUtils.fetchDocument(url)
    val allHrefs = DomUtils.getUrlsFromDoc(doc)

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
    val allHrefs = DomUtils.getUrlsFromDoc(doc)

    val formattedUrls = DomUtils.formatUrls(url, allHrefs)
    val (_, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)
    val sameDomainUrls = DomUtils.filterSubdomainUrls(url, htmlUrls)

    val randomSamples = DomUtils.randomSampleUrls(5, sameDomainUrls)
    val commonHtml = DomUtils.commonPartsOfTemplate(randomSamples).getOrElse("")

    val filesQueue = scala.collection.mutable.Set[String]()

    var urlQueue1 = scala.collection.mutable.ListBuffer[String](url)
    var urlQueue2 = scala.collection.mutable.ListBuffer[String]()
    var flip = true

    (1 to maxDepth).map(_ => {
      if(filesQueue.size < maxNeedFiles){
        if(flip){
          urlQueue1.distinct.map(targetUrl => {
            if(filesQueue.size < maxNeedFiles){
              val (pdfs, sameDomainUrls)= extractUrls(commonHtml, url, targetUrl)
              pdfs.map(filesQueue.add(_))

              sameDomainUrls.map(urlQueue2.append(_))
            }
          })
          urlQueue1 = scala.collection.mutable.ListBuffer[String]()
        }else{

          urlQueue2.distinct.map(targetUrl => {
            if(filesQueue.size < maxNeedFiles){
              val (pdfs, sameDomainUrls)= extractUrls(commonHtml, url, targetUrl)
              pdfs.map(filesQueue.add(_))

              sameDomainUrls.map(urlQueue1.append(_))
            }
          })
          urlQueue2 = scala.collection.mutable.ListBuffer[String]()
        }
      }
    })
    filesQueue.toList
  }
}