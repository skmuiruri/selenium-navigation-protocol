package com.skm

import java.nio.file.Path
import scala.util.Failure
import scala.util.Success
import scala.util.Try

final case class Screenshot private (name: String, path: Path)

object Screenshot {

  def apply(name: String, path: Path): Try[Screenshot] =
    (Option(name).map(_.trim).filter(_.nonEmpty), Option(path)) match {
      case (None, _)                        => Failure(new Exception("Invalid screenshot name provided"))
      case (_, None)                        => Failure(new Exception("Null is not a valid file path"))
      case (Some(fileName), Some(filePath)) => Success(new Screenshot(fileName, filePath))
    }
}
