import com.twitter.util.{Await, Future}
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http._
import io.circe.Error
import io.circe.parser._
import io.circe.syntax._
import cats.data.Xor
import com.twitter.io.Buf


/**
  * Created by zachncst on 12/6/16.
  */

class ErrorResponse(error : String) {
  def getError : String = error
}
case class GenericErrorResponse(error : String) extends ErrorResponse(error)
case class NotFoundResponse() extends ErrorResponse("Not found")


case class FileMetaData(size : String, rev : String, thumb_exists : Boolean,
                           bytes : Int, modified : String, client_mtime : String,
                            path : String, is_dir : Boolean, icon: String,
                            root : String, mime_type : String, revision : Int)

case class UploadPictureBody(path: String, url: String)
case class UploadPictureResponse(status: String, job: String)
case class UploadPictureStatusResponse(status: String, error: Option[String])


class DropboxClient(token: String, folder: String) {
  import io.circe.generic.auto._

  val hostname = "api.dropboxapi.com"
  val client: Service[Request, Response] = Http.client.withTls(hostname).newService(s"${hostname}:443")
  val headers = Map("Authorization" -> s"Bearer ${token}")

  def parseError[T](response : Response): Xor[ErrorResponse, T] = {
    Xor.left(decode[GenericErrorResponse](response.contentString)
      .leftMap(c => GenericErrorResponse(c.getMessage))
      .getOrElse(GenericErrorResponse("Failed to parse")))
  }

  def parseNotFound[T](): Xor[ErrorResponse, T] = {
    Xor.left(NotFoundResponse())
  }

  def getFileMetaData(url: String) : Future[Xor[ErrorResponse,FileMetaData]] = {
    val path = folder + "/" + url.replace('/', '_')
    val request = RequestBuilder().url(s"https://${hostname}:443/1/metadata/auto/${path}")
      .addHeaders(headers)
      .buildGet()

    client(request).map( response => response.status match {
      case Status.Ok => decode[FileMetaData](response.contentString)
      .leftMap(c => GenericErrorResponse(c.getMessage))
      case Status.NotFound => parseNotFound()
      case _ => parseError(response)
    })
  }

  def uploadPicture(url : String) : Future[Xor[ErrorResponse, UploadPictureResponse]] = {
    val path = folder + "/" + url.replace('/', '_')
    val param = ("url", url)
    val request = RequestBuilder().url(s"https://${hostname}:443/1/save_url/auto/${path}")
      .addHeaders(headers)
      .addFormElement(param)
      .buildFormPost(multipart = false)

    client(request).map( response => response.status match {
      case Status.Ok => decode[UploadPictureResponse](response.contentString)
        .leftMap(c => GenericErrorResponse(c.getMessage))
      case _ => parseError(response)
    })
  }

  def uploadPictureStatus(jobId : String) : Future[Xor[ErrorResponse, UploadPictureStatusResponse]] = {
    val request = RequestBuilder().url(s"https://${hostname}:443/1/save_url_job/${jobId}")
      .addHeaders(headers)
      .buildGet()

    client(request).map( response => response.status match {
      case Status.Ok =>println(response.contentString)
        decode[UploadPictureStatusResponse](response.contentString)
        .leftMap(c => GenericErrorResponse(c.getMessage))
      case _ => parseError(response)
    })
  }
}

object DropboxClient {
  def apply(token: String, folder : String) = new DropboxClient(token, folder)
  def apply(dropboxConfig: DropboxConfig) = new DropboxClient(dropboxConfig.token, dropboxConfig.folder)
}