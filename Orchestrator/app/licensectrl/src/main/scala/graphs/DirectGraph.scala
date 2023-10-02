package graph
import scala.collection.mutable.Map
import scala.io.Source
import scala.util.Using

class DirectedGraph {
  private val adjacencyList = Map[String, List[String]]()

  def addEdge(source: String, destination: String): Unit = {
    val neighbors = adjacencyList.getOrElse(source, List())
    adjacencyList(source) = destination :: neighbors
  }

  def getNeighbors(node: String): List[String] = {
    adjacencyList.getOrElse(node, List())
  }

  override def toString: String = adjacencyList.toString()

  // Funzione per verificare se due nodi sono sullo stesso percorso
  def areNodesOnSamePath(node1: String, node2: String): Boolean = {
    // Funzione di ricerca di profondità (DFS) da node1 a node2
    def dfsFromNode1(currentNode: String, visited: Set[String]): Boolean = {
      if (currentNode == node2) {
        true
      } else {
        val unvisitedNeighbors = getNeighbors(currentNode).filterNot(visited.contains)
        unvisitedNeighbors.exists(neighbor => dfsFromNode1(neighbor, visited + currentNode))
      }
    }

    // Funzione di ricerca di profondità (DFS) da node2 a node1
    def dfsFromNode2(currentNode: String, visited: Set[String]): Boolean = {
      if (currentNode == node1) {
        true
      } else {
        val unvisitedNeighbors = getNeighbors(currentNode).filterNot(visited.contains)
        unvisitedNeighbors.exists(neighbor => dfsFromNode2(neighbor, visited + currentNode))
      }
    }

    // Esegue la ricerca in entrambe le direzioni
    dfsFromNode1(node1, Set()) || dfsFromNode2(node2, Set())
  }
  // Funzione per caricare un grafo da un file
  def loadFromFile(filePath: String): Unit = {
    Using(Source.fromFile(filePath)) { source =>
      source.getLines().foreach { line =>
        val Array(node1, node2) = line.split(",")
        addEdge(node1, node2)
      }
    }.getOrElse(println(s"Errore nella lettura del file: $filePath"))
  }

}
