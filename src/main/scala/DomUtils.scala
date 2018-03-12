
import Main.doc
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

  def getUrlsFromDoc(text:browser.DocumentType):List[String] = {
      val aDoms = doc >> elementList("a")
      aDoms.flatMap(e => Try(e.attrs("href")).toOption)
  }

  def fetchRoot(url:String):String = ???
}
