package practice

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Graph, Outlet, SourceShape}

import scala.concurrent.Future

object Ex1 extends App {

  class NumbersSource extends GraphStage[SourceShape[Int]] {

    val out : Outlet[Int] = Outlet("NumbersSource")
    override val shape:SourceShape[Int] = SourceShape(out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        private var counter = 1

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            push(out, counter)
            counter += 1
          }
        })
      }
  }

  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer

  val system = ActorSystem.create
  implicit  val materializer = ActorMaterializer.create(system)
  import scala.concurrent.ExecutionContext.Implicits.global

  val sourceGraph:Graph[SourceShape[Int], NotUsed] = new NumbersSource
  val mySource:Source[Int, NotUsed] = Source.fromGraph(sourceGraph)

  val result1: Future[Int] = mySource.take(10).runFold(0)(_ + _)
  val result2: Future[Int] = mySource.take(100).runFold(0)(_ + _)

  result1.map(println)
  result2.map(println)
}
