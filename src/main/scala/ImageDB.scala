import scala.collection.mutable

/**
  * Created by zachncst on 12/6/16.
  */

case class Image(url : String, addedBy: String, likes : Int = 0) {
  def getName() : String = url.split("/").reverse.head.toLowerCase()
}

case class ImageNotFound() extends Exception

object ImageDB {
  private[this] val db: mutable.Map[String, Image] = mutable.Map.empty[String, Image]

  def get(id: String): Option[Image] = synchronized { db.get(id) }
  def list(): List[Image] = synchronized { db.values.toList.sortBy(- _.likes) }
  def getFileNames() : Map[String, Image] = synchronized { db.values.map(i => (i.getName(), i)).toMap }
  def save(t: Image): Unit = synchronized { db += (t.url -> t) }
  def like(id : String): Unit = synchronized {
    db.get(id) match {
      case Some(t) => db += (t.url -> t.copy(likes=t.likes+1))
      case None => throw ImageNotFound()
    }
  }
}
