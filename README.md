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
the two main classes with `sbt run`:

```
$ export IRC_HOST=irc.libera.chat
$ export IRC_PORT=7000
$ export IRC_USER=my_nick
$ export IRC_PASS=my_password
$ export IRC_CHANNELS=#my_channel
$ export SQS_INBOX_URL=https://example.com/inbox.fifo
$ export SQS_OUTBOX_URL=https://example.com/outbox.fifo
$ export AWS_REGION=my_aws_region
$ export AWS_ACCESS_KEY_ID=my_access_key
$ export AWS_SECRET_ACCESS_KEY=my_secret_key
$ sbt "project irc" run
```

```
$ export DATABASE_URL=mysql://username:password@host:port/dbname
$ export FINNHUB_API_TOKEN=my_finnhub_api_token
$ export OPEN_WEATHER_MAP_API_KEY=my_open_weather_map_api_key
$ export AIRNOW_API_KEY=my_airnow_api_key
$ export SQS_INBOX_URL=https://example.com/inbox.fifo
$ export SQS_OUTBOX_URL=https://example.com/outbox.fifo
$ export AWS_REGION=my_aws_region
$ export AWS_ACCESS_KEY_ID=my_access_key
$ export AWS_SECRET_ACCESS_KEY=my_secret_key
$ sbt "project producers" run
```

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Architecture

Sectery is organized as modules with both shared and disjoint
compile-time visibility:

```
.--------.
| shared |
|        |
|    .-----.
|    | irc |
|    '-----'
|        |
|    .-----------.
|    | producers |
|    '-----------'
|        |
'--------'
```

Modules interact with each other via SQS:

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
