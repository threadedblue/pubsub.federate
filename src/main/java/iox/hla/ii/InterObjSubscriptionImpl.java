package iox.hla.ii;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.util.EcoreUtil;

public abstract class InterObjSubscriptionImpl implements InterObjSubscription {

	private static final Logger log = LogManager.getLogger(InterObjSubscriptionImpl.class);

	@Override
	public EObject receiveInteraction(Double timeStep, EObject interaction) {
		return interaction;
	}

	@Override
	public EObject receiveObject(Double timeStep, String objectClassName, String objectName,
			Map<String, byte[]> attributes) {
		return receiveInteraction(timeStep, objectClassName, attributes);
	}

}
