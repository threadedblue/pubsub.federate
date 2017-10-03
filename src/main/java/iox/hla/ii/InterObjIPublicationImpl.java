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

public abstract class InterObjIPublicationImpl implements InterObjIPublication {

	private static final long serialVersionUID = -3409552497539566001L;

	private static final Logger log = LogManager
			.getLogger(InterObjIPublicationImpl.class);
	Queue<EObject> publications = new ConcurrentLinkedQueue<EObject>();

	public InterObjIPublicationImpl() {
		super();
	}
	
	@Override
	public Queue<EObject> getPreSynchInteractions(){return publications;}
	
	@Override
	public Queue<EObject> getPublications(Double logicalTime){return publications;}
	
	@Override
	public void addInteraction(EObject def) {
		addInterObj(def);
		log.trace("addInteraction=" + def);
	}
	
	@Override
	public void addObject(EObject def) throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ObjectNotKnown {
		addInterObj(def);
		log.trace("addObject=" + def);
	}
	
	private void addInterObj(EObject def) {
		publications.add(def);
	}

	@Override
	public void afterReadytoPopulate() {}

	@Override
	public void afterReadytoRun() {}

	@Override
	public void afterAdvanceLogicalTime() {}
}
