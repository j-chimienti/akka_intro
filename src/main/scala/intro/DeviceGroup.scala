package intro

import akka.actor._
import intro.DeviceGroupQuery.RequestAllTemperatures
import intro.DeviceManager.RequestTrackDevice

import scala.concurrent.duration._

class DeviceGroup(groupId: String) extends Actor with ActorLogging {

  import DeviceGroup._

  var deviceIdToActorRef : Map[String, ActorRef] = Map.empty[String, ActorRef]

  def actorToDeviceId: Map[ActorRef, String] = deviceIdToActorRef.map(_.swap)

  def receive : Receive = {

    case RequestAllTemperatures(requestId) =>
      context.actorOf(DeviceGroupQuery.props(actorToDeviceId, requestId, sender(), 3.seconds))

    case RequestDeviceList(requestId) =>
      sender() ! ReplyDeviceList(requestId, deviceIdToActorRef.keySet)

    case trackMsg @ RequestTrackDevice(groupId, deviceId) if groupId == this.groupId =>
      deviceIdToActorRef.get(deviceId) match {
        case None =>
          val device = context.actorOf(Device.props(groupId, deviceId), s"device-$deviceId")
          deviceIdToActorRef += deviceId -> device
          context watch device
          device forward trackMsg
        case Some(device) =>
          device forward trackMsg

      }
    case RequestTrackDevice(groupId, _) =>
      log.warning(s"Ignoring track device request $groupId. This actor is responsible for ${this.groupId}")
    case Terminated(actor) =>
      actorToDeviceId.get(actor).map(deviceId => deviceIdToActorRef -= deviceId)
  }
}

object DeviceGroup {
  def props(groupId: String) : Props = Props(new DeviceGroup(groupId))

  case class RequestDeviceList(requestId: Long)
  case class ReplyDeviceList(requestId: Long, devices: Set[String])
}