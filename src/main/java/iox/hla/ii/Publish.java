package iox.hla.ii;

import java.util.Queue;

import org.eclipse.emf.ecore.EObject;

import hla.rti.FederateNotExecutionMember;
import hla.rti.NameNotFound;
import hla.rti.ObjectNotKnown;
import hla.rti.RTIinternalError;

public interface Publish {
	
	public Queue<EObject> getPreSynchInteractions();

	public Queue<EObject> getPublications(Double logicalTime);

	public void afterReadytoPopulate();

	public void afterReadytoRun();
	
	public void afterAdvanceLogicalTime();

	void addInteraction(EObject def);

	void addObject(EObject def) throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ObjectNotKnown;
}