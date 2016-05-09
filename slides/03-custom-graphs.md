# Custom Flow Graph
```
/**
  * Akka Streams graph stage thats accepts a *SORTED* list stream of (Master, Detail) and
  * groups them into Combined records. Accomplishes the equivalent of groupBy(Master) but optimized
  * for a sorted stream.
  */
class MasterDetailGraphStage[Combined, Master, Detail]
(combine: (Set[(Master, Detail)]) => Combined)
  extends GraphStage[FlowShape[(Master, Detail), Combined]] {
  type MasterDetail = (Master, Detail)
  val in = Inlet[(Master, Detail)]("in")
  val out = Outlet[Combined]("out")

  val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      var buffer = Set.empty[MasterDetail]
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val elem = grab(in)
          if (buffer.nonEmpty) {
            if (buffer.head._1 == elem._1) {
              buffer = buffer + elem
              pull(in)
            } else {
              push(out, combine(buffer))
              buffer = Set(elem)
            }
          } else {
            buffer = Set(elem)
            pull(in)
          }
        }

        override def onUpstreamFinish(): Unit = {
          if (buffer.nonEmpty) emit(out, combine(buffer))
          complete(out)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }
}
```
