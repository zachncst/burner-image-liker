import cats.data.Xor
import com.twitter.finagle.service.DelayedFactory
import com.twitter.logging.Logging
import com.twitter.util.Await
import io.finch.Endpoint
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import com.twitter.conversions.time._
import com.twitter.util.Duration._
import com.twitter.util.Timer
import com.twitter.util.JavaTimer

import scala.collection.mutable

/**
  * Created by zachncst on 12/6/16.
  */

case class BurnerEvent(`type`: String, payload: String,
                       fromNumber: String, toNumber: String,
                       userId: String, burnerId: String)

case class UploadError(msg: String) extends Exception(msg)


object ImageLikerService extends Configuration {

  import com.twitter.logging.Logger

  private val log = Logger.get(getClass)

  implicit val timer = new JavaTimer()
  val dropboxClient = DropboxClient(dropboxConfig)

  def checkUploadResult(job: String): Unit =
    dropboxClient.uploadPictureStatus(job).delayed(5.seconds).onSuccess {
      case Xor.Right(response) => response.status match {
        case "PENDING" => checkUploadResult(job)
        case "DOWNLOADING" => checkUploadResult(job)
        case "COMPLETE" => Unit
        case "FAILED" => throw new UploadError("Upload FAILED")
      }
      case Xor.Left(error: ErrorResponse) => log.error(s"Error polling result ${error}")
        throw new UploadError(error.getError)
    }


  def postEvent: Endpoint[Unit] =
    post("event" :: body.as[BurnerEvent]) { e: BurnerEvent =>
      e.`type` match {
        case "inboundMedia" => {
          dropboxClient.getFileMetaData(e.payload).onSuccess {
            case Xor.Left(_) => {
              log.info(s"Not found, adding image ${e.payload}")
              dropboxClient.uploadPicture(e.payload).onSuccess {
                case Xor.Left(error) => {
                  log.error(s"Failed to upload image ${e.payload} $error")
                  throw UploadError("Failed to upload")
                }
                case Xor.Right(uploadPictureResponse) => {
                  log.info(s"Image uploaded started successfully status is ${uploadPictureResponse.status}")
                  //checkUploadResult(uploadPictureResponse.job)
                  if (uploadPictureResponse.status == "FAILED") {
                    //Won't capture failures, logic above will
                    throw UploadError("Failed to upload")
                  }
                  val image = Image(e.payload, e.fromNumber)
                  ImageDB.save(image)
                }
              }
            }
            case Xor.Right(_) => {
              log.info(s"Found image ${e.payload}, adding to DB only")
              ImageDB.save(Image(e.payload, e.fromNumber))
            }
          }

          NoContent[Unit]
        }
        case "inboundText" => {
          val fileNames = ImageDB.getFileNames()
          e.payload.split(" ").foreach(word => {
            val image = fileNames.get(word.toLowerCase())
            image match {
              case Some(image) => {
                ImageDB.like(image.url)
              }
              case None =>
            }
          })

          NoContent[Unit]
        }
        case _ => {
          log.info(s"Media type did not match inboundMedia ${e.`type`}")
          throw UploadError(s"Media type did not match inboundMedia ${e.`type`}")
        }
      }
    } handle {
      case e: UploadError => BadRequest(e)
    }

  def getReport: Endpoint[Map[String, Int]] =
    get("report") {
      Ok(ImageDB.list().map(i => (i.url, i.likes)).toMap)
    }
}
