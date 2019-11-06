package intro

import java.security.SecureRandom

import akka.actor.{Actor, ActorLogging, Props}
import intro.DeviceManager.{DeviceRegistered, RequestTrackDevice}

class Device(groupId: String, deviceId: String) extends Actor with ActorLogging {

  import Device._

  override def preStart(): Unit = log.info("Device actor {}-{} started", groupId, deviceId)
  override def postStop(): Unit = log.info("Device actor {}-{} stopped", groupId, deviceId)

  var lastTemperatureRecorded : Option[Double] = None

  def getTemperatue: Double = {
    val r = new SecureRandom()
    r.nextDouble() * 100
  }
  override def receive: Receive = {

    case RecordTemperature(requestId, temperature) =>
      lastTemperatureRecorded = Some(temperature)
      sender() ! TemperatureRecorded(requestId)
    case RequestTrackDevice(groupId, deviceId) if groupId == this.groupId && deviceId == this.deviceId =>
      sender() ! DeviceRegistered
    case RequestTrackDevice(groupId, deviceId) =>
      log.warning(s"Ignoring Track Request for ${groupId}-${deviceId}. This actor is responsible for ${this.groupId}-${this.deviceId}")
    case ReadTemperature(requestId) =>
      sender() ! RespondTemperature(requestId, lastTemperatureRecorded)

  }
}

object Device {

  def props(groupId: String, deviceId: String) = Props(new Device(groupId, deviceId))

  case class RecordTemperature(requestId: Long, temperature: Double)
  case class TemperatureRecorded(requestId: Long)

  case class ReadTemperature(requestId: Long)
  case class RespondTemperature(requestId: Long, value: Option[Double])
}