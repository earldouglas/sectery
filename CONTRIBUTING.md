[![Build Status][build-badge]][build-link]

[build-badge]: https://github.com/earldouglas/sectery/workflows/build/badge.svg "Build Status"
[build-link]: https://github.com/earldouglas/sectery/actions "GitHub Actions"

# Contributing

## Producer

Message responses are coded in the [`Producer`][Producer.scala]
implementations in the [`sectery.producers` package][sectery.producers].

[Producer.scala]: src/main/scala/sectery/Producer.scala
[sectery.producers]: src/main/scala/sectery/producers/

To add support for a new message response:

1. Write a new [`Producer`][Producer.scala] implementation, e.g.
   [`Count`][Count.scala]
2. Write a test for it, e.g. [`CountSpec`][CountSpec.scala]
3. Add it to the list of producers in
   [`Producer.producers`][Producer.producers]

[Count.scala]: https://github.com/earldouglas/sectery/blob/d96e9bcb85816d8793fc6c10547feb5117a82ed1/src/main/scala/sectery/producers/Count.scala
[CountSpec.scala]: https://github.com/earldouglas/sectery/blob/d96e9bcb85816d8793fc6c10547feb5117a82ed1/src/test/scala/sectery/producers/CountSpec.scala
[Producer.producers]: https://github.com/earldouglas/sectery/blob/d96e9bcb85816d8793fc6c10547feb5117a82ed1/src/main/scala/sectery/Producer.scala#L75

## Testing

For operational convenience, tests use SQLite.  Production uses
PostgreSQL, so SQL queries need to be portable between the two.

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
