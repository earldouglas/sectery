![Build Status](https://github.com/earldouglas/sectery/workflows/build/badge.svg)

# Sectery

Sectery is an digital assistant IRC bot.

## Producer

Message responses are coded in the [`Producer`][Producer.scala]
implementations in the [`sectery.producers` package][sectery.producers].

To add support for a new message response, write a new `Producer`
implementation, and add it to the list of producers in
[`Producer.producers`][Producer.scala].

[Producer.scala]: src/main/scala/sectery/Producer.scala
[sectery.producers]: src/main/scala/sectery/producers/

## References

### PircBotX

* https://github.com/pircbotx/pircbotx
* https://pircbotx.github.io/pircbotx/2.2/apidocs/index.html

### ZIO

* https://zio.dev/docs/overview/overview_index
* https://javadoc.io/doc/dev.zio/zio_2.12/1.0.8/zio/index.html
