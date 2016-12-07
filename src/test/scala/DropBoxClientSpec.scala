import cats.data.Xor
import com.twitter.util.Await
import knobs.{ClassPathResource, Config, Required}

import collection.mutable.Stack
import org.scalatest._

import scalaz.concurrent.Task

/**
  * Created by zachncst on 12/6/16.
  */
class DropBoxClientSpec extends FlatSpec with Matchers {
  val config : Task[Config]  = knobs.loadImmutable(
    Required(ClassPathResource("test.cfg")) :: Nil)

  val token = "n-QWqekRi50AAAAAAAAK9Xscw6ueMy9tV5N2vn6FVq35ZBirpVIQABqV7nAGCmBl"
  val folder = "test-folder"
  val dropboxClient = DropboxClient(token, folder)
  val pic1 = "https://images.pexels.com/photos/244277/pexels-photo-244277.jpeg"

  "Dropbox client" should "post files with valid urls" in {
    val response : Xor[ErrorResponse, UploadPictureResponse] = Await.result(dropboxClient.uploadPicture(pic1))
    response.isRight should equal (true)
    response.getOrElse(UploadPictureResponse("FAILED", "")) should
      (have ('status ("PENDING")) or have ('status ("COMPLETE")))
  }

  it should "get file metadata" in {
    val response : Xor[ErrorResponse, FileMetaData] = Await.result(dropboxClient.getFileMetaData(pic1))
    response.isRight should equal (true)
    response.toOption.get
  }

  it should "get file upload status" in {
    val work = for {
      upload <- dropboxClient.uploadPicture(pic1)
      jobId = upload.toOption.get.job
      status <- dropboxClient.uploadPictureStatus(jobId)
    } yield status

      val response : Xor[ErrorResponse, UploadPictureStatusResponse] = Await.result(work)
      response.isRight should equal (true)
      response.getOrElse(UploadPictureResponse("FAILED", "")) should
        (have ('status ("PENDING")) or have ('status ("COMPLETE")) or have ('status ("DOWNLOADING")))
    }
}
