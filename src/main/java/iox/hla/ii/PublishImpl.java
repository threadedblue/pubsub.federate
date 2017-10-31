package iox.hla.ii;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.emf.ecore.EObject;

import hla.rti.FederateNotExecutionMember;
import hla.rti.NameNotFound;
import hla.rti.ObjectNotKnown;
import hla.rti.RTIinternalError;
import iox.hla.core.federatecore.FederatecoreFactory;
import iox.hla.core.federatecore.JoinInteraction;
import iox.hla.core.federatecore.ResignInteraction;

public class PublishImpl implements Publish {

	private static final long serialVersionUID = -3409552497539566001L;

	private static final Logger log = LogManager.getLogger(PublishImpl.class);
	Queue<EObject> publications = new ConcurrentLinkedQueue<EObject>();
	protected PubSubFederate federate;

	public PublishImpl() {
		super();
	}

	@Override
	public Queue<EObject> getPreSynchInteractions() {
//		JoinInteraction joinInteraction = FederatecoreFactory.eINSTANCE.createJoinInteraction();
//		joinInteraction.setFederateName(federate.getFederateName());
//		publications.add(joinInteraction);
//		ResignInteraction resignInteraction = FederatecoreFactory.eINSTANCE.createResignInteraction();
//		resignInteraction.setFederateName(federate.getFederateName());
//		publications.add(resignInteraction);
		return publications;
	}

	@Override
	public Queue<EObject> getPublications(Double logicalTime) {
		return publications;
	}

	@Override
	public void addInteraction(EObject def) {
		addInterObj(def);
		log.trace("addInteraction=" + def);
	}

	@Override
	public void addObject(EObject def)
			throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ObjectNotKnown {
		addInterObj(def);
		log.trace("addObject=" + def);
	}

	private void addInterObj(EObject def) {
		publications.add(def);
	}

	@Override
	public void afterReadytoPopulate() {
	}

	@Override
	public void afterReadytoRun() {
	}

	@Override
	public void afterAdvanceLogicalTime() {
	}
}
