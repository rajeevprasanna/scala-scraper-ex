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

  def getUrlsFromDoc(doc:browser.DocumentType):List[String] = {
      val aDoms = doc >> elementList("a")
      aDoms.flatMap(e => Try(e.attrs("href")).toOption)
  }

  def fetchRoot(fullURL:String):String = {
    val formatted = checkForProtocol(fullURL)
    val url = new URL(formatted)
    val lastIndex = url.getFile().lastIndexOf('/')
    val path = url.getFile().substring(0, if(lastIndex == -1)  0 else lastIndex)
    url.getProtocol() + "://" + url.getHost() + path
  }

  def checkForProtocol(url:String):String = url.startsWith("http") match {
    case true => url
    case _ => "http://" + url
  }

  def removeQueryString(url:String):String = {
    val formatted = checkForProtocol(url)
    formatted.split("\\?").head
  }

  def formatUrls(sourceUrl:String, urls:List[String]):List[String] = {
    val rootUrl = fetchRoot(sourceUrl)
    val formattedSourceUrl = removeQueryString(sourceUrl)
    urls.flatMap(url => {
      val formatted = checkForProtocol(url)
      formatted match {
        case x if x.startsWith("/") => Some(rootUrl + x)
        case x if x.startsWith("http") => Some(x)
        case x if x.startsWith("#") => None
        case _ => Some(formattedSourceUrl + formatted)
      }
    })
  }

  def getUrlExtension(url:String):String = {
    val path = new URL(url).getPath
    if(path.lastIndexOf(".") == -1) "" else path.substring(path.indexOf("."))
  }

  def randomSampleUrls(n:Int, urls:List[String]):List[String] = {
    import scala.util.Random
    val validUrls = urls.filter(el => el.endsWith(".html") || getUrlExtension(el) != "")
    val maxSamples = Math.max(n, validUrls.length)
    Random.shuffle(validUrls).take(maxSamples)
  }

  def commonPartsOfTemplate(urls:List[String]):Option[String] = {
    val divList = urls.map(fetchDocument(_)).map(_ >> elementList("div")).map(divList => divList.map(_.toString)).map(_.toSet)
    val tuples = divList.sliding(2).toList.map(el => el.head.intersect(el.last))
    val patternCounter = tuples.groupBy(identity).map(el => (el, el._2.length)).toList
    patternCounter.sortBy(_._2).head._1._1.headOption
  }
}
