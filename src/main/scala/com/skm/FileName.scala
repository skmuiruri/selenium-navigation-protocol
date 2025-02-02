package com.skm

import scala.util.Failure
import scala.util.Success
import scala.util.Try

final case class FileName(name: String, extension: String)

object FileName {
  def extractFileName(input: String): Try[FileName] =
    Option(input).map(_.trim).filter(_.nonEmpty) match {
      case Some(value) =>
        val lastDot = value.lastIndexOf(".")
        if (lastDot <= 0 || lastDot == value.length - 1)
          Failure(new Exception("File name must contain a valid name and extension"))
        else {
          val (fileName, fileExt) = value.splitAt(lastDot)
          Success(FileName(fileName, fileExt.drop(1)))
        }
      case None => Failure(new Exception("Invalid or empty file name"))
    }
}