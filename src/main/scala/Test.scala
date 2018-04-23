//import DomUtils.browser
import akka.actor.ActorSystem

import scala.collection.mutable
import scala.concurrent.{Await, Future, Promise}
import cats._
import cats.implicits._

import scala.concurrent.duration._
import scala.util.{Success, Try}
import scala.concurrent.duration._

object Test extends App
{

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val akkaSystem = ActorSystem("web-crawler")

  def getF():Future[Int] = {
    val p = Promise[Int]()
    Future
    {
      val random:Int = (math.random() * 100).toInt
      println(s"random => $random")
      if(random % 2 == 0){
        p.success(random)
      }
    }
    p.future
  }

  Try(Await.result(getF(), 3 seconds))
   match {
    case Success(x) => println(s"Success => $x")
    case _ => println(s"Reached junk state")
  }

//  val s = "https://support.cylance.com/s///"
//  val x = '/'
//  s.reverse.dropWhile(_ == '/').reverse

//  def junk():Future[Option[Int]] = {
//    val p = Promise[Option[Int]]()
//    Future {
//     val x:Option[Int] =  for {
//        x <- 10.some
//        y <- 1.some
//        res <- (10/10).some
//      } yield res
//      println(s"x ===> $x")
//      p.success(x)
//    }
//    p.future
//  }
//  val res:Future[Int] = junk().map(_.getOrElse(0) + 1)
//  Try{Await.result(res, 10  seconds)}
//
//  res.onComplete{
//    case Success(x) => println("success")
//    case _ => println("failure")
//  }


//  println(res)
//  println("done")

//  import io.github.bonigarcia.wdm.ChromeDriverManager
//  import org.openqa.selenium.chrome.ChromeDriver
//  import org.openqa.selenium.chrome.ChromeOptions
//
//  ChromeDriverManager.getInstance().setup()
//
//  val chromePrefs:mutable.Map[String, Any] = mutable.Map[String, Any]()
//  chromePrefs.put("profile.default_content_settings.popups", 0)
//  chromePrefs.put("download.default_directory", ".")
//  chromePrefs.put("Browser.setDownloadBehavior", "allow")
//
//  val options = new ChromeOptions()
//  options.setExperimentalOption("prefs", chromePrefs)
//  options.addArguments("--disable-extensions") //to disable browser extension popup
//  options.addArguments("test-type")
//  options.addArguments("disable-popup-blocking")
//
//          options.setHeadless(true)
//  //        val capabilities = DesiredCapabilities.chrome()
//  //        capabilities.setCapability(ChromeOptions.CAPABILITY, options)
//
//  val driver = new ChromeDriver(options)
//  driver.get("https://www.citrix.com/products/netscaler-adc/resources/")
////  driver.wait(20 * 1000)
//
//  val dom = driver.getPageSource()
//  def parseString(dom:String):browser.DocumentType = browser.parseString(dom)
//
//  val allHrefs = DomUtils.getUrlsFromDoc(parseString(dom))
//  val formattedUrls = DomUtils.formatUrls("https://www.citrix.com/products/netscaler-adc/resources/", allHrefs)
//
//  val (resourceUrls, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)
//  println(resourceUrls)
//
//  driver.close()
}
