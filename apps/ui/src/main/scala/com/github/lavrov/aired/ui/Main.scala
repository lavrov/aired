package com.github.lavrov.aired.ui

import com.thoughtworks.binding.{Binding, dom}

import org.scalajs.dom.document
import org.scalajs.dom.html.UList

object Main {

  def number = 5

  @dom def list: Binding[UList] =
    <ul>
      <li>one</li>
      <li>two</li>
      <li>123456</li>
    </ul>

  def main(args: Array[String]): Unit = {
    dom.render(document.getElementById("app"), list)
  }

}
