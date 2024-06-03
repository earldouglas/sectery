package sectery.slack

import com.slack.api.model.event.MessageBotEvent
import com.slack.api.model.event.MessageEvent

case class Event(
    from: String,
    channel: String,
    thread: String,
    text: String
)

object Event:

  def apply(event: MessageEvent): Event =
    Event(
      from = event.getUser(),
      channel = event.getChannel(),
      thread = event.getTs(),
      text = event.getText()
    )

  def apply(event: MessageBotEvent): Event =
    Event(
      from = event.getUsername(),
      channel = event.getChannel(),
      thread = event.getTs(),
      text = event.getText()
    )
