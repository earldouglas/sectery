package sectery.adaptors.frinkiac

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
      Caption.caption("S04E16", 164915)

    val expected: Option[Caption.Caption] =
      Some(
        Caption.Caption(
          Episode = Caption.Episode(
            Id = 410,
            Key = "S04E16",
            Season = 4,
            EpisodeNumber = 16,
            Title = "Duffless",
            Director = "Jim Reardon",
            Writer = "David M. Stern",
            OriginalAirDate = "1993-02-18",
            WikiLink = "https://en.wikipedia.org/wiki/Duffless",
            VideoWidth = 480,
            VideoHeight = 360
          ),
          Frame = Caption.Frame(
            Id = 3514571,
            Episode = "S04E16",
            Timestamp = 164915
          ),
          Subtitles = List(
            Caption.Subtitles(
              Id = 244624,
              RepresentativeTimestamp = 161370,
              Episode = "S04E16",
              StartTimestamp = 160827,
              EndTimestamp = 162162,
              Content = "Uh-oh.",
              Language = "en"
            ),
            Caption.Subtitles(
              Id = 244625,
              RepresentativeTimestamp = 163038,
              Episode = "S04E16",
              StartTimestamp = 162245,
              EndTimestamp = 163997,
              Content = "Did I say that or just think it?",
              Language = "en"
            ),
            Caption.Subtitles(
              Id = 244626,
              RepresentativeTimestamp = 164706,
              Episode = "S04E16",
              StartTimestamp = 164080,
              EndTimestamp = 165623,
              Content = "I got to think of a lie fast.",
              Language = "en"
            ),
            Caption.Subtitles(
              Id = 244627,
              RepresentativeTimestamp = 166375,
              Episode = "S04E16",
              StartTimestamp = 165707,
              EndTimestamp = 167584,
              Content = "Homer, are you going to the Duff Brewery?",
              Language = "en"
            )
          ),
          MinTimestamp = 1001,
          MaxTimestamp = 1383215
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
