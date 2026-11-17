package jetuml.model;

/** Observador do modelo. A visão se inscreve; o diagrama não conhece a interface gráfica. */
public interface DiagramListener {
    void diagramChanged(Diagram diagram);
}
