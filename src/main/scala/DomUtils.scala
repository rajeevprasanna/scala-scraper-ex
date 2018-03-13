import java.net.URL

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import scala.concurrent.duration._
import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

import scala.util.Try

object DomUtils {

  val browser = JsoupBrowser()

  def fetchDocument(url:String):browser.DocumentType = browser.get(url)

  def parseString(dom:String):browser.DocumentType = browser.parseString(dom)

  def getUrlsFromDoc(doc:browser.DocumentType):List[String] = {
      val aDoms = doc >> elementList("a")
      aDoms.flatMap(e => Try(e.attrs("href")).toOption)
  }

  def fetchRoot(fullURL:String):String = {
    val formatted = checkForProtocol(fullURL)
    val url = removeQueryString(formatted)
    val url2 = new URL(url)
    url.split(url2.getPath).head
  }

  def checkForProtocol(url:String):String = url.startsWith("http") match {
    case true => url
    case _ => "http://" + url
  }

  def removeQueryString(url:String):String = {
    val formatted = checkForProtocol(url)
    formatted.split("\\?").head
  }

  def filterPDFUrls(urls:List[String]):List[String] =
    urls.partition(url => {
      val extension = getUrlExtension(url)
      extension == ".pdf"
    })._1


  def extractResourceUrls(urls:List[String]):(List[String], List[String]) = urls.partition(url => {
    val extension = getUrlExtension(url)
    val resourceExtensions = Set(".pdf", ".doc", "gif", "gif", "jpg", "jpg", "png", "png", "ico", "ico", "css", "css", "sit", "sit", "eps", "eps", "wmf", "wmf", "zip", "zip", "ppt", "ppt", "mpg", "mpg", "xls", "xls", "gz", "gz", "rpm", "rpm", "tgz", "tgz", "mov", "mov", "exe", "exe", "jpeg", "jpeg", "bmp", "bmp", "js", "js")
    resourceExtensions.contains(extension)
  })

  def filterSubdomainUrls(resourceURL:String, urls:List[String]):List[String] = {
    val resourceDomain = getDomain(resourceURL)
    urls.filter(getDomain(_) == resourceDomain)
  }

  def getDomain(url:String) = {
    val hostName = new URL(url).getHost
    hostName.split("\\.").tail.reduce(_+"."+_)
  }


  def formatUrls(sourceUrl:String, urls:List[String]):List[String] = {
    val rootUrl = fetchRoot(sourceUrl)
    val formattedSourceUrl = removeQueryString(sourceUrl)
    urls.flatMap(url => {
      url.trim match {
        case x if x.startsWith("/") => Some(rootUrl + x)
        case x if x.startsWith("http") => Some(x)
        case x if x.startsWith("#") => None
        case _ => Some(formattedSourceUrl + url)
      }
    }).distinct
  }

  def getUrlExtension(url:String):String = {
    val path = new URL(url).getPath
    val lastIndex = path.lastIndexOf(".")
    if(lastIndex == -1) "" else path.substring(lastIndex).toLowerCase()
  }

  def randomSampleUrls(n:Int, urls:List[String]):List[String] = {
    import scala.util.Random
    val maxSamples = Math.min(n, urls.length)
    Random.shuffle(urls).take(maxSamples)
  }

  def commonPartsOfTemplate(urls:List[String]):Option[String] = {
    val divList = urls.map(fetchDocument(_)).map(_ >> elementList("div")).map(divList => divList.map(_.toString)).map(_.toSet)
    val tuples = divList.sliding(2).toList.map(el => el.head.intersect(el.last))
    val patternCounter = tuples.groupBy(identity).map(el => (el, el._2.length)).toList
    patternCounter.sortBy(_._2).head._1._1.headOption
  }
}
