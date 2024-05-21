[![Build Status][build-badge]][build-link]

[build-badge]: https://github.com/earldouglas/sectery/workflows/build/badge.svg "Build Status"
[build-link]: https://github.com/earldouglas/sectery/actions "GitHub Actions"

# Sectery

Sectery is an digital assistant IRC bot.

## Usage

To run Sectery, you'll need API keys for
[Finnhub](https://finnhub.io/docs/api), [Dark
Sky](https://darksky.net/dev), [AirNow](https://docs.airnowapi.org/),
and [OpenAI](https://platform.openai.com/docs/quickstart/api-keys).

To start Sectery, first set a bunch of configuration variables, then run
the two main classes with `sbt run`:

```
$ export IRC_HOST=irc.libera.chat
$ export IRC_PORT=7000
$ export IRC_USER=my_nick
$ export IRC_PASS=my_password
$ export IRC_CHANNELS=#my_channel
$ export RABBIT_MQ_HOSTNAME=localhost
$ export RABBIT_MQ_PORT=5672
$ export RABBIT_MQ_USERNAME=guest
$ export RABBIT_MQ_PASSWORD=guest
$ sbt "project irc" run
```

```
$ export DATABASE_URL=jdbc:mysql://username:password@host:port/dbname
$ export FINNHUB_API_TOKEN=my_finnhub_api_token
$ export OPEN_WEATHER_MAP_API_KEY=my_open_weather_map_api_key
$ export AIRNOW_API_KEY=my_airnow_api_key
$ export RABBIT_MQ_HOSTNAME=localhost
$ export RABBIT_MQ_PORT=5672
$ export RABBIT_MQ_USERNAME=guest
$ export RABBIT_MQ_PASSWORD=guest
$ export OPENAI_APIKEY=my_openai_api_key
$ sbt "project producers" run
```

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Architecture

### Layers

Sectery's code is organized as layered modules with both shared and
disjoint compile-time visibility.  Each layer has access to the layers
beneath.

```
.----------------------------------------------------------------------.
|                                Layer 5                           ZIO |
|                                                                      |
|                irc ----------> shared <---------- producers          |
| o                                                                    |
'-|--------------------------------------------------------------------'
  |
.-|--------------------------------------------------------------------.
| v                              Layer 4                               |
| |                                                                    |
| |                             adaptors (implement L2 effects)        |
| | o                                                                  |
'-|-|------------------------------------------------------------------'
  | |
  | |                                                             impure
==|=|===================================================================
  | |                                                               pure
  | |
.-|-|------------------------------------------------------------------.
| v v                            Layer 3                               |
| | |                                                                  |
| | |                           use cases                              |
| | |                             |   |                                |
| | |        responders <---------'   '---------> announcers           |
| | | o                                                                |
'-|-|-|----------------------------------------------------------------'
  | | |
.-|-|-|----------------------------------------------------------------.
| v v v                          Layer 2                               |
| | | |                                                                |
| | | |                          effects                               |
| | | | o                                                              |
'-|-|-|-|--------------------------------------------------------------'
  | | | |
.-|-|-|-|--------------------------------------------------------------.
| v v v v                        Layer 1                               |
|                                                                      |
|                                domain                                |
|                                 |  |                                 |
|              entities <---------'  '---------> operations            |
|                                                                      |
'----------------------------------------------------------------------'
```

Layers 1 through 3 are all pure functions and data structures modelling
the domain, business rules, and effects.

Layers 4 and 5 both impure, with effects implementations underneath ZIO
to wire everything together.

### Message queue

Modules interact with each other via RabbitMQ:

```
                      .------------.
                     / \            \
    .-----------------> |   inbox    |=================..
    |                \ /            /                  ||
    |                 '------------'                   vv
.-------.                                        .-------------.
|  irc  |                                        |  producers  |-.
'-------'                                        '-------------' |-.
    ^                                              '-------------' |
    |                                                '-------------'
    |                 .------------.                   ||
    |                /            / \                  ||
    '---------------|   outbox   | <===================''
                     \            \ /
                      '------------'
```

### External dependencies

Modules access various internal and external resources:

```
.-----.         .------------.
| irc |-------->| IRC Server |
'-----'         '------------'

.-----------.         .----------------.
| producers |-.======>| 3rd party APIs |-.
'-----------' |-.     '----------------' |-.
  '-----------' |       '----------------' |
    '-----------'         '----------------'
         ||
         ||           .-------.
         ''==========>| RDBMS |
                      '-------'
```
