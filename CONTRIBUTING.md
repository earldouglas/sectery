[![Build Status][build-badge]][build-link]

[build-badge]: https://github.com/earldouglas/sectery/workflows/build/badge.svg "Build Status"
[build-link]: https://github.com/earldouglas/sectery/actions "GitHub Actions"

# Contributing

## Producer

Message responses are coded in the [`Producer`][Producer.scala]
implementations in the [`sectery.producers` package][sectery.producers]
of the `producers` module.

[Producer.scala]: modules/producers/src/main/scala/sectery/Producer.scala
[sectery.producers]: modules/producers/src/main/scala/sectery/producers/

To add support for a new message response:

1. Write a new [`Producer`][Producer.scala] implementation, e.g.
   [`Count`][Count.scala]
2. Write a test for it, e.g. [`CountSpec`][CountSpec.scala]
3. Add it to the list of producers in
   [`Producer.producers`][Producer.producers]

[Count.scala]: modules/producers/src/main/scala/sectery/producers/Count.scala
[CountSpec.scala]: modules/producers/src/test/scala/sectery/producers/CountSpec.scala
[Producer.producers]: modules/producers/src/main/scala/sectery/Producer.scala

## Testing

For operational convenience, tests use H2.  Production uses MySQL, so
SQL queries need to be portable between the two.

To test with RabbitMQ, first run a local instance with Docker:

```
$ docker run -p 5672:5672 --rm rabbitmq:3
```

Then provide the configuration env vars to the sbt process:

```
$ RABBIT_MQ_HOSTNAME=localhost \
  RABBIT_MQ_PORT=5672 \
  RABBIT_MQ_USERNAME=guest \
  RABBIT_MQ_PASSWORD=guest \
  sbt test
```

## References

### PircBotX

Sectery uses [PircBotX](https://github.com/pircbotx/pircbotx) to
interface with an IRC server.

* https://github.com/pircbotx/pircbotx
* https://pircbotx.github.io/pircbotx/2.2/apidocs/index.html

### ZIO

Sectery uses [ZIO](https://zio.dev/) for asynchronicity and structuring
effects.

* https://zio.dev/docs/overview/overview_index
* https://javadoc.io/doc/dev.zio/zio_2.12/1.0.8/zio/index.html
