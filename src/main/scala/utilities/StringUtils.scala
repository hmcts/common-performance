package utilities

import scala.util.Random

object StringUtils {

  def randomString(length: Int): String = {
    val letters = ('A' to 'Z') ++ ('a' to 'z')
    LazyList.continually(letters(Random.nextInt(letters.length))).take(length).mkString
  }

}
