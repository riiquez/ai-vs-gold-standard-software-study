package jetuml.model;

import jetuml.model.nodes.ActorNode;
import jetuml.model.nodes.CallNode;
import jetuml.model.nodes.ClassNode;
import jetuml.model.nodes.FieldNode;
import jetuml.model.nodes.FinalStateNode;
import jetuml.model.nodes.ImplicitParameterNode;
import jetuml.model.nodes.InitialStateNode;
import jetuml.model.nodes.InterfaceNode;
import jetuml.model.nodes.NoteNode;
import jetuml.model.nodes.ObjectNode;
import jetuml.model.nodes.PackageDescriptionNode;
import jetuml.model.nodes.PackageNode;
import jetuml.model.nodes.PointNode;
import jetuml.model.nodes.StateNode;
import jetuml.model.nodes.UseCaseNode;

/** Visita cada tipo concreto de nó. O modelo não conhece desenho nem Swing. */
public interface NodeVisitor<T> {
    T visitClass(ClassNode node);

    T visitInterface(InterfaceNode node);

    T visitPackage(PackageNode node);

    T visitPackageDescription(PackageDescriptionNode node);

    T visitNote(NoteNode node);

    T visitPoint(PointNode node);

    T visitImplicitParameter(ImplicitParameterNode node);

    T visitCall(CallNode node);

    T visitState(StateNode node);

    T visitInitialState(InitialStateNode node);

    T visitFinalState(FinalStateNode node);

    T visitObject(ObjectNode node);

    T visitField(FieldNode node);

    T visitActor(ActorNode node);

    T visitUseCase(UseCaseNode node);
}
