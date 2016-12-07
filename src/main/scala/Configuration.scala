import knobs.{ClassPathResource, Config, Required}

/**
  * Created by zachncst on 12/6/16.
  */

case class DropboxConfig(token: String, folder : String)

trait Configuration {
    val config : Config  = knobs.loadImmutable(
        Required(ClassPathResource("dev.cfg")) :: Nil).run

    val port = config.require[Int]("port")
    val dropboxConfig = DropboxConfig(
        config.require[String]("dropbox.token"),
        config.require[String]("dropbox.folder"))
}
