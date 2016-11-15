package application.compilerBuilder

import java.awt._
import javax.swing._
import javax.swing.border.BevelBorder
import javax.swing.event.{ListSelectionEvent, ListSelectionListener}

import application.compilerCockpit.MarkOutputGrammar
import application.{InjectorListCellRenderer, StyleSheet}
import core.particles.Delta
import org.jdesktop.swingx.JXList
import transformations.javac.JavaCompiler
import application.compilerBuilder.ParticleInstance._

object CompilerBuilderPanel {
  val availableParticles = JavaCompiler.allTransformations ++ Seq(MarkOutputGrammar)
}

class CompilerBuilderPanel extends JPanel(new GridBagLayout()) {

  val painter = new ParticleLabelPainter(this, CompilerBuilderPanel.availableParticles)

  setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED))
  setAutoscrolls(true)

  val programPanel = new CompilerStatePanel(this)
  val compilerParticles = programPanel.selectedParticles
  val presetsPanel: JPanel = new PresetsPanel(programPanel.compilerName, compilerParticles)

  val presetsConstraints = getConstraints
  add(presetsPanel, presetsConstraints)

  val availableScrollPane = getAvailableScrollPane
  val availableListConstraints: GridBagConstraints = getConstraints
  add(availableScrollPane, availableListConstraints)

  val programPanelConstraints = getConstraints
  programPanelConstraints.weightx = if (!StyleSheet.presentationMode) 2 else 1
  add(programPanel, programPanelConstraints)

  def getInjectorListVisuals(list: JXList) = {

    val result = new JPanel(new BorderLayout())

    list.setAutoCreateRowSorter(true)

    list.addListSelectionListener(new ListSelectionListener {
      override def valueChanged(e: ListSelectionEvent): Unit = {
        painter.select(list.getSelectedValues.map(i => i.getParticle).toSeq)
      }
    })

    val listener = new SearchFieldListener(list)
    val searchField = new JTextField(20)
    searchField.getDocument.addDocumentListener(listener)
    result.add(searchField, BorderLayout.PAGE_START)

    val scrollPane: JScrollPane = StyleSheet.getAnyListVisuals(list)
    list.setCellRenderer(new InjectorListCellRenderer(painter))

    result.add(scrollPane, BorderLayout.CENTER)

    result
  }

  def getAvailableScrollPane = {
    val availableItems: Seq[Delta] = CompilerBuilderPanel.availableParticles.sortBy(i => i.name)
    val availableList = new ParticleList()
    availableList.setModel(new AbstractListModel[Delta] {
      def getSize: Int = availableItems.length
      def getElementAt(i: Int): Delta = availableItems(i)
    })

    availableList.setTransferHandler(new ParticleProviderTransferHandler(availableList))
    availableList.setDragEnabled(true)

    val result = getInjectorListVisuals(availableList)
    StyleSheet.setTitleBorder(result, "Available")
    result
  }

  def getConstraints: GridBagConstraints = {
    val cons = new GridBagConstraints()
    cons.fill = GridBagConstraints.BOTH
    cons.weightx = 1
    cons.weighty = 1
    cons
  }
}
