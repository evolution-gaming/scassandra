package com.evolution.scassandra.compat

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import scala.util.Using

class TranslationDocSpec extends AnyFunSuite with Matchers {

  private lazy val rows: Map[String, String] = {
    val lines = Using.resource(Source.fromResource("legacy-config-translation.md")) { _.getLines().toList }
    val key = """^\| `([^`]+)` \|.*""".r
    lines.collect { case line @ key(path) => path -> line }.toMap
  }

  test("every known key is documented with its status") {
    val undocumented = V3Translation.Keys.filterNot { key =>
      rows.get(key.path).exists { row => row.contains(s"| ${ key.status } |") }
    }

    undocumented shouldEqual List.empty
  }

  test("every documented key is known") {
    rows.keySet -- V3Translation.Keys.map(_.path) shouldEqual Set.empty
  }
}
