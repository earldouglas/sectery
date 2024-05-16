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

```
$ sbt test
```

## Integration testing

To test against a real IRC server, export the necessary env vars and
run the test main method:

```
$ IRC_USER=redacted \
  IRC_PASS=redacted \
  IRC_HOST=redacted \
  IRC_PORT=redacted \
  OPEN_WEATHER_MAP_API_KEY=redacted \
  AIRNOW_API_KEY=redacted \
  FINNHUB_API_TOKEN=redacted \
  OPENAI_APIKEY=redacted \
  sbt Test/run
```

This uses Testcontainers to run the necessary RabbitMQ and MariaDB
dependencies within local Docker containers.

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
