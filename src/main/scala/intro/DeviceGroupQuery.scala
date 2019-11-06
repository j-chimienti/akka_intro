package intro

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated, Timers}
import intro.Device.{ReadTemperature, RespondTemperature}

import scala.concurrent.duration.FiniteDuration

class DeviceGroupQuery(actorToDeviceId: Map[ActorRef, String], requestId: Long, requester: ActorRef, timeout: FiniteDuration) extends Actor with ActorLogging with Timers {


  import DeviceGroupQuery._

  override def preStart(): Unit = {
    timers.startSingleTimer(CollectionTimeout, CollectionTimeout, timeout)
    actorToDeviceId.keySet.foreach(device => {
      context watch device
      device ! ReadTemperature(requestId)
    })
  }

  override def postStop(): Unit = timers.cancel(CollectionTimeout)

  override def receive: Receive = waitingForReplies(Map.empty[String, TemperatureReading], actorToDeviceId)

  def waitingForReplies(repliesSoFar: Map[String, TemperatureReading], stillWaiting: Map[ActorRef, String]): Receive = {

    case CollectionTimeout =>
      val timedOutReplies = stillWaiting.keySet.map(a => actorToDeviceId(a) -> DeviceTimedOut)
      requester ! RespondAllTemperatures(requestId, repliesSoFar ++ timedOutReplies)
    case RespondTemperature(requestId, temperature) if requestId == this.requestId =>
      val reading = temperature match {
        case None => TemperatureNotAvailable
        case Some(temp) => Temperature(temp)
      }
      receivedResponse(sender(), reading, repliesSoFar, stillWaiting)

    case Terminated(deviceActor) =>
      receivedResponse(deviceActor, DeviceNotAvailable, repliesSoFar, stillWaiting)

  }

  def receivedResponse(deviceActor: ActorRef, reading: TemperatureReading, repliesSoFar: Map[String, TemperatureReading], stillWaiting: Map[ActorRef, String]) = {

    val sw: Map[ActorRef, String] = stillWaiting - deviceActor
    val rsf : Map[String, TemperatureReading] = repliesSoFar + (actorToDeviceId(deviceActor) -> reading)
    if (sw.isEmpty) {
      requester ! RespondAllTemperatures(requestId, rsf)
      self ! PoisonPill
    } else {
      context become waitingForReplies(rsf, sw)
    }

  }

}

object DeviceGroupQuery {

  def props(actorToDeviceId: Map[ActorRef, String], requestId: Long, requester: ActorRef, timeout: FiniteDuration) : Props =
    Props(new DeviceGroupQuery(actorToDeviceId, requestId, requester, timeout))

  case class RequestAllTemperatures(requestId: Long)
  case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

  trait TemperatureReading
  case object DeviceTimedOut extends TemperatureReading
  case object DeviceNotAvailable extends TemperatureReading
  case object TemperatureNotAvailable extends TemperatureReading
  case class Temperature(temperature: Double) extends TemperatureReading

  case object CollectionTimeout
}
