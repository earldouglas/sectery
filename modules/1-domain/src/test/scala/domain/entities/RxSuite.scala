package sectery.domain.entities

import munit.FunSuite

class RxSuite extends FunSuite:

  test("Rx#unapply strips leading nick") {
    assertEquals(
      obtained = Rx.unapply(Rx("#foo", "bar", "<baz> raz qux")),
      expected = Some(("#foo", "bar", "raz qux"))
    )
  }
