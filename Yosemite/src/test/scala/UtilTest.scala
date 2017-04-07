import org.scalatest.{FunSpec, ShouldMatchers}

/**
  * Created by zhanghan on 17/4/5.
  */

class Artist(val firstName: String, val lastName: String)

// Album 有三个属性 title表示专辑名称 year表示发行年份 artist表示专辑作者
class Album(val title: String, val year: Int, val artist: Artist)

class AlbumTest extends FunSpec with ShouldMatchers {
  describe("An Album") {
    it("can add an Artist object to the album") {
      val album = new Album("Thriller", 1981, new Artist("Michael", "Jackson"))
      album.artist.firstName should be("Michael")
    }
  }

  describe("han's first test case"){
    it("test"){
      val t=2
      t should be(3)
    }
  }


}


