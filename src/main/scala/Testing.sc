case class Voff(a: String, b: Int)

val voff = Voff("hei", 3)

Voff.tupled

Voff.unapply(Voff("a", 1))


Voff.unapply(Voff("a", 1)).map(Voff.tupled(_))
