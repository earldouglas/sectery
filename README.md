[![Build Status][build-badge]][build-link]

[build-badge]: https://github.com/earldouglas/sectery/workflows/build/badge.svg "Build Status"
[build-link]: https://github.com/earldouglas/sectery/actions "GitHub Actions"

# Sectery

Sectery is an digital assistant IRC bot.

## Usage

To run Sectery, you'll need API keys for
[Finnhub](https://finnhub.io/docs/api), [Dark
Sky](https://darksky.net/dev), and
[AirNow](https://docs.airnowapi.org/).

To start Sectery, first set a bunch of configuration variables, then run
the main class with `sbt run`:

```
$ export IRC_HOST=irc.libera.chat
$ export IRC_PORT=7000
$ export IRC_USER=my_nick
$ export IRC_PASS=my_password
$ export IRC_CHANNELS=#my_channel
$ export FINNHUB_API_TOKEN=my_finnhub_api_token
$ export DARK_SKY_API_KEY=my_dark_sky_api_key
$ export AIRNOW_API_KEY=my_airnow_api_key
$ export DATABASE_URL=postgress://username:password@host:port/dbname
$ sbt run
```

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Architecture

Sectery is organized as modules with both shared and disjoint
compile-time visibility:

```
.--------------------------.
| main                     |
|                          |
|    .--------.            |
|    | shared |            |
|    |        |            |
|    |    .-----.          |
|    |    | irc |          |
|    |    '-----'          |
|    |        |            |
|    |    .-----------.    |
|    |    | producers |    |
|    |    '-----------'    |
|    |        |            |
|    '--------'            |
|                          |
'--------------------------'
```

Modules interact with each other via queues and hubs:

```
      .-----.
.-----| irc |<-----------------.
|     '-----'                  |
|                              |
|       .---------.            |
|      /         / \           |
'---->|   inbox | =======..    |
       \  (hub)  \ /     ||    |
        '---------'      ||    |
                         ||    |
      .-----------.      ||    |
..====| producers |-.<===''    |
||    '-----------' |-.        |
||      '-----------' |        |
||        '-----------'        |
||                             |
||      .-----------.          |
||     /           / \         |
''===>|   outbox  | -----------'
       \  (queue)  \ /
        '-----------'
```

Modules access various external resources:

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
