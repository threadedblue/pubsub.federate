package iox.hla.ii;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.portico.impl.hla1516e.types.HLA1516eParameterHandleValueMap;
import org.portico.impl.hla1516e.types.time.DoubleTime;

import hla.rti.ObjectNotKnown;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.NullFederateAmbassador;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.RTIinternalError;

// We assume single threaded environment
public class FederateAmbassador extends NullFederateAmbassador {
	private static final Logger log = LogManager.getLogger();

	RTIambassador rtiAmb;
	private final double federateTime = 0.0;
	private final double federateLookahead = 1.0;

	public FederateAmbassador(RTIambassador rtiAmb) {
		super();
		this.rtiAmb = rtiAmb;
	}

	// synchronization point labels that have been announced but not achieved
	private Set<String> pendingSynchronizationPoints = new HashSet<String>();

	// map the handle for a discovered object instance to its associated
	// ObjectRef
	private Map<ObjectInstanceHandle, ObjectRef> objectInstances = new HashMap<ObjectInstanceHandle, ObjectRef>();

	// names of previously discovered object instances that have since been
	// removed
	private LinkedList<String> removedObjectNames = new LinkedList<String>();

	private LinkedList<InteractionRef> receivedInteractions = new LinkedList<InteractionRef>();
	private LinkedList<ObjectRef> receivedObjectReflections = new LinkedList<ObjectRef>();

	private boolean isTimeAdvancing = false;
	private boolean isTimeRegulating = false;
	private boolean isTimeConstrained = false;

	private double logicalTime = 0D;

	@Override
	public void announceSynchronizationPoint(String synchronizationPointLabel, byte[] userSuppliedTag) {
		if (pendingSynchronizationPoints.contains(synchronizationPointLabel)) {
			log.warn("duplicate announcement of synchronization point: " + synchronizationPointLabel);
		} else {
			pendingSynchronizationPoints.add(synchronizationPointLabel);
			log.info("synchronization point announced: " + synchronizationPointLabel);
		}
	}

	@Override
	public void timeRegulationEnabled(LogicalTime theFederateTime) {
		isTimeRegulating = true;
		logicalTime = convertTime(theFederateTime);
		log.debug("time regulation enabled: t=" + logicalTime);
	}

	@Override
	public void timeConstrainedEnabled(LogicalTime theFederateTime) {
		isTimeConstrained = true;
		logicalTime = convertTime(theFederateTime);
		log.debug("time constrained enabled: t=" + logicalTime);
	}

	@Override
	public void timeAdvanceGrant(LogicalTime theTime) {
		isTimeAdvancing = false;
		logicalTime = convertTime(theTime);
		log.debug("time advance granted: t=" + logicalTime);
	}

	@Override
	public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap theInteraction,
			byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport,
			SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
		try {
			receiveInteraction(interactionClass, theInteraction, userSuppliedTag, sentOrdering, theTransport, null,
					sentOrdering, receiveInfo);
		} catch (FederateInternalError e) {
			throw new FederateInternalError(e);
		}
	}

	@Override
	public void receiveInteraction(InteractionClassHandle interactionClassHandle, ParameterHandleValueMap parameters,
			byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, LogicalTime theTime,
			OrderType receivedOrdering, SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
		String interactionName = null;
		try {
			interactionName = rtiAmb.getInteractionClassName(interactionClassHandle);
		} catch (InvalidInteractionClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
			log.error(e);
		}
		log.info("received interaction: handle=" + interactionClassHandle);
		receivedInteractions.add(new InteractionRef(interactionClassHandle, interactionName, parameters));
	}

	@Override
	public void discoverObjectInstance(ObjectInstanceHandle objectInstanceHandle, ObjectClassHandle objectClassHandle,
			String objectName) throws FederateInternalError {
		log.info("discovered new object instance: (handle, class, name)=" + "(" + objectInstanceHandle + ", " + objectClassHandle
				+ ", " + objectName + ")");
		if (objectInstances.get(objectInstanceHandle) == null) {
			objectInstances.put(objectInstanceHandle, new ObjectRef(objectInstanceHandle, objectClassHandle, objectName, null));
		} else {
			log.debug(String.format("Already discovered: theObject=%d theObjectClass=%d objectName=%s its ok carry on",
					objectInstanceHandle, objectClassHandle, objectName));
		}
	}

	@Override
	public void reflectAttributeValues(ObjectInstanceHandle objectInstanceHandle, AttributeHandleValueMap attributes,
			byte[] userSuppliedTag, OrderType sentOrder, TransportationTypeHandle transport,
			SupplementalReflectInfo reflectInfo) throws FederateInternalError {
		reflectAttributeValues(objectInstanceHandle, attributes, userSuppliedTag, sentOrder, transport, null, sentOrder,
				reflectInfo);
	}

	@Override
	public void reflectAttributeValues(ObjectInstanceHandle objectInstanceHandle, AttributeHandleValueMap attributes,
			byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, LogicalTime theTime,
			OrderType receivedOrdering, SupplementalReflectInfo reflectInfo) throws FederateInternalError {
		ObjectRef objectRef = objectInstances.get(objectInstanceHandle);
		if (objectRef == null) {
			try {
				throw new ObjectNotKnown("no discovered object instance with handle " + objectInstanceHandle);
			} catch (ObjectNotKnown e) {
				log.error(e);
			}
		}
		ObjectClassHandle objectClassHandle = objectRef.getObjectClassHandle();
		String objectName = objectRef.getObjectName();
		receivedObjectReflections.add(new ObjectRef(objectInstanceHandle, objectClassHandle, objectName, attributes));
		log.info("received object reflection for the object instance " + objectName);
	}

	@Override
	public void removeObjectInstance(ObjectInstanceHandle theObject, byte[] userSuppliedTag, OrderType sentOrdering,
			SupplementalRemoveInfo removeInfo) throws FederateInternalError {
		ObjectRef details = objectInstances.remove(theObject);
		if (details == null) {
			try {
				throw new ObjectNotKnown("no discovered object instance with handle " + theObject);
			} catch (ObjectNotKnown e) {
				log.error(e);
			}
		}
		String objectName = details.getObjectName();
		removedObjectNames.add(objectName);
		log.info("received notice to remove object instance with handle=" + theObject + " and name=" + objectName);
	}

	public boolean isSynchronizationPointPending(String label) {
		return pendingSynchronizationPoints.contains(label);
	}

	public double getFederateTime() {
		return federateTime;
	}

	public double getFederateLookahead() {
		return federateLookahead;
	}

	public double getLogicalTime() {
		return logicalTime;
	}

	public void setTimeAdvancing() {
		isTimeAdvancing = true;
	}

	public boolean isTimeAdvancing() {
		return isTimeAdvancing;
	}

	public boolean isTimeRegulating() {
		return isTimeRegulating;
	}

	public boolean isTimeConstrained() {
		return isTimeConstrained;
	}

	public InteractionRef nextInteraction() {
		return receivedInteractions.pollFirst(); // destructive read
	}

	public ObjectRef nextObjectReflection() {
		return receivedObjectReflections.pollFirst(); // destructive read
	}

	public String nextRemovedObjectName() {
		return removedObjectNames.pollFirst(); // destructive read
	}

	private double convertTime(LogicalTime logicalTime) {
		// conversion from portico to java types
		return ((DoubleTime) logicalTime).getTime();
	}
}
