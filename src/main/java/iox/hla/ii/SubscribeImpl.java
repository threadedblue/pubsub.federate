package iox.hla.ii;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

public class SubscribeImpl implements Subscribe {

	private static final Logger log = LogManager.getLogger(SubscribeImpl.class);

	@Override
	public EObject receiveInteraction(Double timeStep, EObject interaction) {
		EClass eClass = interaction.eClass();
		return interaction;
	}

	@Override
	public EObject receiveObject(Double timeStep, EObject object) {
		return receiveInteraction(timeStep, object);
	}

	@Override
	public void receiveMessage(Double timeStep, EObject message) {}
}
