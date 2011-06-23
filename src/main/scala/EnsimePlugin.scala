import sbt._
import Keys._
import java.io.FileWriter

/**
 * A minimal plugin that writes a minimal .ensime file
 */
object EnsimePlugin extends Plugin
{
  override lazy val settings = Seq(commands += myCommand)

  lazy val myCommand = 
    Command.command("ensime") { (state: State) =>
      val extr = Project.extract(state)
      import extr._
      val nameOpt: Option[String] = name in currentRef get structure.data
			       
      val cpOpt : Option[Seq[Attributed[File]]] = 
	Project.evaluateTask(fullClasspath in (currentRef, Test), state) flatMap {
	  case Inc(_) => None
	  case Value(v) => Some(v)
	}
      val baseOpt: Option[File] = baseDirectory in currentRef get structure.data

      for (dir <-baseOpt) {
	val fn = dir / ".ensime"
	val fw = new FileWriter(fn)
	fw.write("(\n:use-sbt t")
	try {
	  for (n <- nameOpt) { fw.write(":project-name \"" + n +"\"") }
	  for (cp <- cpOpt) {
	    val (jars, dirs) = cp.map(_.data).partition(f => f.exists && f.isFile)
	    def fmt(files: Seq[File]) = files.map("\"" + _ + "\"").reduceRight(_+" "+_)
	    fw.write(":compile-jars (" + fmt(jars) + ")")
	    fw.write(":class-dirs ("+ fmt(dirs) +")")
	  }
	  fw.write(")")
	} finally {
	  fw.close()
	}
      }
      state
    }

  def show[T](s: Seq[T]) =
    s.map("'" + _ + "'").mkString("[", ", ", "]")
}
