package practice

import java.util
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, TimerGraphStageLogic}
import practice.Ex1.NumbersSource

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object ex3 extends App {

  class PollingExampleSource extends GraphStage[SourceShape[Int]] {

    val pollInterval = FiniteDuration.apply(2, TimeUnit.SECONDS)

    val out : Outlet[Int] = Outlet("NumbersSource")

    override val shape:SourceShape[Int] = SourceShape(out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new TimerGraphStageLogic(shape) {

          val buffer = new ListBuffer[Int]()

          setHandler(out, new OutHandler {
            override def onPull(): Unit =
              if(!buffer.isEmpty){
                pushHead()
              } else {
                doPoll()
                if(!buffer.isEmpty){
                  pushHead()
                }else{
                  schedulePoll()
                }
              }
          })

        def schedulePoll(): Unit ={
          scheduleOnce("poll", pollInterval)
        }

        def pushHead(): Unit = {
          buffer.headOption match {
            case Some(h) =>
              buffer.remove(0)
              push(out, h)
            case _ => //ignore
          }
        }

        def doPoll(): Unit = {
          val random = (Math.random * 10).toInt
          println(s"Going to sleep $random seconds")
          Thread.sleep(2 * 1000)
          buffer.append(random)
        }
      }
  }

  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer

  val system = ActorSystem.create
  implicit  val materializer = ActorMaterializer.create(system)
  import scala.concurrent.ExecutionContext.Implicits.global

  val sourceGraph:Graph[SourceShape[Int], NotUsed] = new PollingExampleSource
  val mySource:Source[Int, NotUsed] = Source.fromGraph(sourceGraph)

  val result1: Future[Int] = mySource.take(10).runFold(0)(_ + _)
//  val result2: Future[Int] = mySource.take(100).runFold(0)(_ + _)

  result1.map(println)
//  result2.map(println)

}
