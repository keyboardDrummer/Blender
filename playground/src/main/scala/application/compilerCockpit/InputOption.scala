package application.compilerCockpit

import java.io.InputStream

trait InputOption {
  def getInput: InputStream
}
