package iox.hla.ii;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;

public interface InterObjSubscription {

	EObject receiveInteraction(Double timeStep, EObject interaction);
	
	EObject receiveObject(Double timeStep, EObject object);
}
