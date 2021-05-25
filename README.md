![Build Status](https://github.com/earldouglas/sectery/workflows/build/badge.svg)

# Sectery

Sectery is an digital assistant IRC bot.

## Producer

Message responses are coded in the [`Producer`][1] implementations in
the [`sectery.producers` package][2].

To add support for a new message response, write a new `Producer`
implementation, and add it to the list of producers in
[`Producer.producers`][1].

[1]: src/main/scala/sectery/Producer.scala
[2]: src/main/scala/sectery/producers/

## References

### PircBotX

* https://github.com/pircbotx/pircbotx
* https://pircbotx.github.io/pircbotx/2.2/apidocs/index.html

### ZIO

* https://zio.dev/docs/overview/overview_index
* https://javadoc.io/doc/dev.zio/zio_2.12/1.0.8/zio/index.html
