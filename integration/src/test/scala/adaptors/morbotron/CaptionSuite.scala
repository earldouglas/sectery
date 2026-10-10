package sectery.adaptors.morbotron

import munit.FunSuite
import sectery._
import sectery.adaptors._
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class CaptionSuite extends FunSuite:

  given logger: Logger[Id] with
    override def debug(message: => String) =
      println(message)
    override def error(message: => String) =
      println(message)

  given httpClient: HttpClient[Id] =
    new LiveHttpClient[Id]

  test("caption"):

    val obtained: Option[Caption.Caption] =
      Caption.caption("S05E01", 377835)

    val expected: Option[Caption.Caption] =
      Some(
        Caption.Caption(
          Episode = Caption.Episode(
            Id = 313,
            Key = "S05E01",
            Season = 5,
            EpisodeNumber = 1,
            Title = "Crimes of the Hot",
            Director = "Peter Avanzino",
            Writer = "Aaron Ehasz",
            OriginalAirDate = "2002-11-10",
            WikiLink = "https://en.wikipedia.org/wiki/Crimes_of_the_Hot"
          ),
          Frame = Caption.Frame(
            Id = 2987490,
            Episode = "S05E01",
            Timestamp = 377835
          ),
          Subtitles = List(
            Caption.Subtitles(
              Id = 221990,
              RepresentativeTimestamp = 375541,
              Episode = "S05E01",
              StartTimestamp = 374291,
              EndTimestamp = 376835,
              Content = "I'm sure those windmills will keep them cool.",
              Language = "en"
            ),
            Caption.Subtitles(
              Id = 221991,
              RepresentativeTimestamp = 377627,
              Episode = "S05E01",
              StartTimestamp = 376919,
              EndTimestamp = 378504,
              Content = "Windmills do not work that way!",
              Language = "en"
            ),
            Caption.Subtitles(
              Id = 221992,
              RepresentativeTimestamp = 378878,
              Episode = "S05E01",
              StartTimestamp = 378587,
              EndTimestamp = 379588,
              Content = "Good night!",
              Language = "en"
            )
          ),
          Nearby = List(
            Caption.Nearby(
              Id = 2003314,
              Episode = "S05E01",
              Timestamp = 378311
            ),
            Caption.Nearby(
              Id = 2003313,
              Episode = "S05E01",
              Timestamp = 378528
            ),
            Caption.Nearby(
              Id = 2003321,
              Episode = "S05E01",
              Timestamp = 378728
            ),
            Caption.Nearby(
              Id = 2003319,
              Episode = "S05E01",
              Timestamp = 378945
            ),
            Caption.Nearby(
              Id = 2003324,
              Episode = "S05E01",
              Timestamp = 379145
            ),
            Caption.Nearby(
              Id = 2003322,
              Episode = "S05E01",
              Timestamp = 379362
            ),
            Caption.Nearby(
              Id = 2003323,
              Episode = "S05E01",
              Timestamp = 379562
            )
          )
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
