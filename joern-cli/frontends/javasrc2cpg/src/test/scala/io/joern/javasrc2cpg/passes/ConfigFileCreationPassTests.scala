package io.joern.javasrc2cpg.passes

import better.files.File
import io.joern.javasrc2cpg.testfixtures.JavaSrcCode2CpgFixture
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.semanticcpg.language._
import io.shiftleft.utils.ProjectRoot

import java.nio.file.Paths

class ConfigFileCreationPassTests extends JavaSrcCode2CpgFixture {

  private val testConfigDir: String =
    ProjectRoot.relativise("joern-cli/frontends/javasrc2cpg/src/test/resources/config_tests")

  "it should find the correct config files" in {
    val foundFiles        = new ConfigFileCreationPass(testConfigDir, new Cpg()).generateParts().map(_.canonicalPath)
    val absoluteConfigDir = File(testConfigDir).canonicalPath
    foundFiles should contain theSameElementsAs Array(
      Paths.get(absoluteConfigDir, "application.conf").toString,
      Paths.get(absoluteConfigDir, "basic.jsp").toString,
      Paths.get(absoluteConfigDir, "basic.properties").toString,
      Paths.get(absoluteConfigDir, "routes").toString,
      Paths.get(absoluteConfigDir, "basic.tf").toString,
      Paths.get(absoluteConfigDir, "basic.tfvars").toString,
      Paths.get(absoluteConfigDir, "basic.vm").toString,
      Paths.get(absoluteConfigDir, "batis", "conf.xml").toString,
      Paths.get(absoluteConfigDir, "dwr.xml").toString,
      Paths.get(absoluteConfigDir, "faces-config.xml").toString,
      Paths.get(absoluteConfigDir, "nested", "nested.properties").toString,
      Paths.get(absoluteConfigDir, "struts.xml").toString,
      Paths.get(absoluteConfigDir, "web.xml").toString
    )
  }

  "it should populate config files correctly" in {
    val cpg = code("""
        |public class Foo{}
	    |""".stripMargin)
      .moreCode(code = "key=value", fileName = "config.properties")

    cpg.typeDecl.name("Foo").nonEmpty shouldBe true
    cpg.configFile.l match {
      case List(configFile) =>
        configFile.name shouldBe "config.properties"
        configFile.content shouldBe "key=value"

      case result => fail(s"Expected single config file but got $result")
    }
  }

}
