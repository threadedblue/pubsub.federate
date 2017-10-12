package iox.hla.ii;

import org.eclipse.emf.ecore.EObject;

public interface Subscribe {

	EObject receiveInteraction(Double timeStep, EObject interaction);
	
	EObject receiveObject(Double timeStep, EObject object);

	void receiveMessage(Double timeStep, EObject message);
}
