package org.gizmosoft.streams

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, ThrottleMode}
import akka.{Done, NotUsed}

import scala.concurrent.Future
import scala.concurrent.duration._

object StreamsApp extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source: Source[BigInt, NotUsed] = Source(BigInt(1) to BigInt(100))

//  val done: Future[Done] = source.runForeach(i ⇒ println(i))(materializer)
  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

//  val sink: Sink[BigInt, Future[Done]] = Flow[BigInt].toMat(Sink.foreach(i => println(i)))(Keep.right)
//  val done: Future[Done] = factorials.runWith(sink)

  val sinkPrinter: Sink[BigInt, Future[Done]] = Sink.foreach(i => println(i))
//  val sink = Sink.fold[Int, Int](0)(_ + _)

//  val done: Future[Done] = factorials
//    .zipWith(source)((num, idx) ⇒ s"$idx! = $num")
//    .throttle(1, 1.second, 1, ThrottleMode.shaping)
//    .runWith(sink)

//  val runnable: RunnableGraph[Future[Done]] = factorials.toMat(sink)(Keep.right)
  val runnable = RunnableGraph.fromGraph(GraphDSL.create(sinkPrinter) {
    implicit builder => sink =>
    import GraphDSL.Implicits._
    val sharedDoubler = Flow[BigInt].map(_ * 2)

    source ~> sharedDoubler ~> sink.in
    ClosedShape
  })

  val done: Future[Done] = runnable.run()

  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())
}
