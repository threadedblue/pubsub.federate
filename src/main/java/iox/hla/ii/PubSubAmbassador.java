package iox.hla.ii;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import hla.rti.ObjectNotKnown;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.exceptions.FederateInternalError;
import iox.hla.core.FederateAmbassador;
import iox.hla.core.InteractionRef;
import iox.hla.core.ObjectRef;

// We assume single threaded environment
public class PubSubAmbassador extends FederateAmbassador {
	private static final Logger log = LogManager.getLogger();

	public PubSubAmbassador() {
		super();
	}

	private Map<ObjectInstanceHandle, ObjectRef> objectInstances = new HashMap<ObjectInstanceHandle, ObjectRef>();

	private LinkedList<String> removedObjectNames = new LinkedList<String>();

	private LinkedList<InteractionRef> receivedInteractions = new LinkedList<InteractionRef>();
	private LinkedList<ObjectRef> receivedObjectReflections = new LinkedList<ObjectRef>();

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
		log.info("received interaction: handle=" + interactionClassHandle);
		receivedInteractions.add(new InteractionRef(interactionClassHandle, parameters));
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

	public InteractionRef nextInteraction() {
		return receivedInteractions.pollFirst(); // destructive read
	}

	public ObjectRef nextObjectReflection() {
		return receivedObjectReflections.pollFirst(); // destructive read
	}

	public String nextRemovedObjectName() {
		return removedObjectNames.pollFirst(); // destructive read
	}
}
