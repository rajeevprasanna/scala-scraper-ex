import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.stream.{Graph, SourceShape}
import akka.stream.scaladsl.{RunnableGraph, Sink, Source}

import scala.concurrent.duration.FiniteDuration

object CrawlTrigger extends AppContext {

  def startCrawling() = {
    val pollInterval = FiniteDuration.apply(1, TimeUnit.MILLISECONDS)
    val sourceGraph:Graph[SourceShape[ResourceUrlPayload], NotUsed] = new PollingSource(BFRedisClient.fetchCrawlUrl, pollInterval)
    val crawlUrlSource:Source[ResourceUrlPayload, NotUsed] = Source.fromGraph(sourceGraph)
    val res:RunnableGraph[NotUsed] = crawlUrlSource.mapAsyncUnordered(ConfReader.parallelCrawlingThreads)(urlPayload => Crawler.extractFiles(urlPayload.url, ConfReader.maxCrawlDepth, ConfReader.maxNeededFiles, urlPayload.is_ajax.getOrElse(false))).to(Sink.ignore)
    res.run()
  }

}
