package iox.hla.ii;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.ieee.standards.ieee1516._2010.AttributeType1;
import org.ieee.standards.ieee1516._2010.InteractionClassType;
import org.ieee.standards.ieee1516._2010.ObjectClassType;
import org.ieee.standards.ieee1516._2010.ObjectModelType;
import org.ieee.standards.ieee1516._2010.SharingEnumerations;
import org.portico.impl.hla1516e.types.HLA1516eParameterHandleValueMap;
import org.portico.impl.hla1516e.types.time.DoubleTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.ResignAction;
import hla.rti1516e.exceptions.AlreadyConnected;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.CallNotAllowedFromWithinCallback;
import hla.rti1516e.exceptions.ConnectionFailed;
import hla.rti1516e.exceptions.CouldNotCreateLogicalTimeFactory;
import hla.rti1516e.exceptions.CouldNotOpenFDD;
import hla.rti1516e.exceptions.ErrorReadingFDD;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateServiceInvocationsAreBeingReportedViaMOM;
import hla.rti1516e.exceptions.InconsistentFDD;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.InvalidLocalSettingsDesignator;
import hla.rti1516e.exceptions.InvalidLogicalTime;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.ObjectClassNotDefined;
import hla.rti1516e.exceptions.ObjectInstanceNotKnown;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;
import hla.rti1516e.exceptions.SynchronizationPointLabelNotAnnounced;
import hla.rti1516e.exceptions.UnsupportedCallbackModel;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import iox.hla.core.AbstractFederate;
import iox.hla.core.FederateAmbassador;
import iox.hla.core.InteractionRef;
import iox.hla.core.ObjectRef;
import iox.hla.core.RTIAmbassadorException;
import iox.hla.core.federatecore.FederatecoreFactory;
import iox.hla.core.federatecore.JoinInteraction;
import iox.hla.core.federatecore.Message;
import iox.hla.core.federatecore.ResignInteraction;
import iox.hla.fom2emf.FOM;
import iox.hla.ii.config.InteractionPublicationConfig;

public class PubSubFederate extends AbstractFederate {

	private static final Logger log = LogManager.getLogger(PubSubFederate.class);

	private State state = State.CONSTRUCTED;
	private InteractionPublicationConfig iiConfig;
	private Map<String, ObjectInstanceHandle> registeredObjects = new HashMap<String, ObjectInstanceHandle>();

	// set of object names that have been created as injectable entities
	private HashSet<String> discoveredObjects = new HashSet<String>();

	private static String fomFilePath;

	private Publish interObjectInjection;

	private Subscribe interObjectReception;

	private TimeStepHook timeStepHook;

	private AtomicBoolean advancing = new AtomicBoolean(false);

	private URL[] foms;

	public String getFomFilePath() {
		return fomFilePath;
	}

	public PubSubFederate() throws RTIAmbassadorException, ParserConfigurationException {
		super();
	}

	public void init() {
		log.trace("init==>");

		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			ObjectName eyeName = new ObjectName("EyePubSubMBean:type=EyePubSub");
			EyePubSub eye = new EyePubSub(this);
			server.registerMBean(eye, eyeName);
		} catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException
				| MalformedObjectNameException e) {
			log.error(e);
		}

		if (state != State.INITIALIZED) {
			throw new IllegalStateException("execute cannot be called in the current state: " + state.name());
		}

		try {
			rtiAmb.connect(fedAmb, CallbackModel.HLA_EVOKED);
			joinFederationExecution();
			changeState(State.JOINED);
			enableAsynchronousDelivery();
			enableTimeConstrained();
			enableTimeRegulation();
			publishAndSubscribe();
		} catch (InterruptedException | RTIAmbassadorException | RTIinternalError | ConnectionFailed
				| InvalidLocalSettingsDesignator | UnsupportedCallbackModel | AlreadyConnected
				| CallNotAllowedFromWithinCallback e) {
			log.error(e);
		}
		log.trace("<==init");
	}

	public Double advanceLogicalTime() throws RTIAmbassadorException {
		advancing.set(true);
		setLogicalTime(getLogicalTime() + getStepSize());
		log.info("advancing logical time to " + getLogicalTime());
		try {
			fedAmb.setTimeAdvancing();
			HLAfloat64Time time = getTimeFactory().makeTime(getLogicalTime());
			rtiAmb.timeAdvanceRequest(time);
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
		while (fedAmb.isTimeAdvancing()) {
			tick();
		}
		log.info("advanced logical time to " + getLogicalTime());
		advancing.set(false);
		return getLogicalTime();
	}

	public void loadConfiguration(String filepath) throws IOException {
		if (state != State.CONSTRUCTED && state != State.INITIALIZED) {
			throw new IllegalStateException("loadConfiguration cannot be called in the current state: " + state.name());
		}

		log.debug("Loading injection federate configuration=" + filepath);
		Path configPath = Paths.get(filepath);
		File configFile = configPath.toFile();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		iiConfig = mapper.readValue(configFile, InteractionPublicationConfig.class);
		setFederateName(iiConfig.getFederateName());
		log.debug("federate-name=" + getFederateName());
		setFederationName(iiConfig.getFederation());
		log.debug("federation=" + getFederationName());
		fomFilePath = iiConfig.getFomFile();
		log.debug("fomFile=" + fomFilePath);
		setLookahead(iiConfig.getLookahead());
		log.debug("lookahead=" + getLookahead());
		setStepSize(iiConfig.getStepsize());
		log.debug("stepsize=" + getStepSize());
		URL[] urls = { new File(fomFilePath).getAbsoluteFile().toURI().toURL() };
		this.foms = urls;
		ObjectModelType fom = FOM.generateFOM(fomFilePath);
		this.fom = fom;
		changeState(State.INITIALIZED);
	}

	private void changeState(State newState) {
		if (newState != state) {
			log.info("state change from " + state.name() + " to " + newState.name());
			state = newState;
		}
	}

	public void run() {
		log.trace("run==>");

		try {
			timeStepHook.beforeReadytoPopulate();
			synchronize(SYNCH_POINTS.readyToPopulate);
			timeStepHook.afterReadytoPopulate();

			timeStepHook.beforeReadytoRun();
			synchronize(SYNCH_POINTS.readyToRun);
			timeStepHook.afterReadytoRun();

			log.info("enter while==>" + state.name());
			while (state != State.TERMINATING) {

				handleMessages();
				processIntObjs(getLogicalTime());

				timeStepHook.beforeAdvanceLogicalTime();
				advanceLogicalTime();
				timeStepHook.afterAdvanceLogicalTime();

			}
		} catch (RTIAmbassadorException | CallNotAllowedFromWithinCallback | RTIinternalError e) {
			log.error(e);
		} finally {
			try {
				switch (state) {
				case TERMINATING:
					synchronize(SYNCH_POINTS.readyToResign);
					resignFederationExecution();
					break;
				case JOINED:
					resignFederationExecution();
					break;
				default:
					break;
				}
			} catch (Exception e) {
				log.warn("failed to resign federation execution", e);
			}
		}
		log.trace("<==run");
	}

	private void processIntObjs(Double logicalTime) {
		log.trace("processIntObjs==> logicalTime=" + logicalTime);
		Queue<EObject> interactions = null;
		if (logicalTime == 0D) {
			interactions = getInterObjectInjection().getPreSynchInteractions();
		} else {
			interactions = getInterObjectInjection().getPublications(logicalTime);
		}

		EObject def = null;
		while ((def = interactions.poll()) != null) {
			sendInteraction(def, logicalTime);
		}
		log.trace("<==processIntObjs");
	}

	private void handleMessages() throws RTIAmbassadorException {

		boolean receivedNothing = true;
		try {
			InteractionRef receivedInteraction;
			while ((receivedInteraction = fedAmb.nextInteraction()) != null) {
				// log.trace("receivedInteraction=" + receivedInteraction);
				receivedNothing = false;
				String interactionClassName = rtiAmb
						.getInteractionClassName(receivedInteraction.getInteractionClassHandle());
				EClass eClass = findEClass(interactionClassName);
				log.debug("interactionClassName=" + interactionClassName + "eClass=" + eClass);
				EObject eObject = EcoreUtil.create(eClass);

				for (Map.Entry<ParameterHandle, byte[]> entry : receivedInteraction.getParameters().entrySet()) {
					String parameterName = rtiAmb.getParameterName(receivedInteraction.getInteractionClassHandle(),
							entry.getKey());
					EAttribute eAttribute = (EAttribute) eClass.getEStructuralFeature(parameterName);
					EDataType eDataType = eAttribute.getEAttributeType();
					Object obj = EcoreUtil.createFromString(eDataType, new String(entry.getValue()));
					eObject.eSet(eAttribute, obj);
				}

				getInterObjectReception().receiveInteraction(getLogicalTime(), eObject);
			}

			ObjectRef receivedObjectReflection;
			while ((receivedObjectReflection = fedAmb.nextObjectReflection()) != null) {
				// log.trace("receivedObjectReflection=" +
				// receivedObjectReflection);
				receivedNothing = false;
				String objectClassName = rtiAmb.getObjectClassName(receivedObjectReflection.getObjectClassHandle());
				EClass eClass = findEClass(objectClassName);
				EObject eObject = EcoreUtil.create(eClass);

				for (Map.Entry<AttributeHandle, byte[]> entry : receivedObjectReflection.getAttributes().entrySet()) {
					String attributeName = rtiAmb.getAttributeName(receivedObjectReflection.getObjectClassHandle(),
							entry.getKey());
					EAttribute eAttribute = (EAttribute) eClass.getEStructuralFeature(attributeName);
					EDataType eDataType = eAttribute.getEAttributeType();
					Object obj = EcoreUtil.createFromString(eDataType, new String(entry.getValue()));
					eObject.eSet(eAttribute, obj);
				}
				getInterObjectReception().receiveObject(getLogicalTime(), eObject);
			}

			String removedObjectName;
			while ((removedObjectName = fedAmb.nextRemovedObjectName()) != null) {
				if (discoveredObjects.remove(removedObjectName) == false) {
					log.warn("tried to delete an unknown object instance with name=" + removedObjectName);
				} else {
					log.info("deleting context broker entity with id=" + removedObjectName);
				}
			}

			if (receivedNothing && !advancing.get()) {
				DoubleTime lt = (DoubleTime) rtiAmb.queryLogicalTime();
				Message msg = FederatecoreFactory.eINSTANCE.createMessage();
				msg.setMessage(String.format("%s %s", lt.toString(), "Nothing received!!"));
				getInterObjectReception().receiveInteraction(getLogicalTime(), msg);
			}
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
	}

	Map<String, byte[]> mapParameters(InteractionRef receivedInteraction) {
		InteractionClassHandle interactionHandle = receivedInteraction.getInteractionClassHandle();
		Map<String, byte[]> parameters = null;
		try {
			parameters = new HashMap<String, byte[]>();
			for (Map.Entry<ParameterHandle, byte[]> entry : receivedInteraction.getParameters().entrySet()) {
				String parameterName = rtiAmb.getParameterName(interactionHandle, entry.getKey());
				byte[] parameterValue = receivedInteraction.getParameters().get(entry.getKey());
				log.debug(parameterName + "=" + parameterValue);
				parameters.put(parameterName, parameterValue);
			}
		} catch (RTIexception e) {
			log.error("", e);
		}
		return parameters;
	}

	Map<String, byte[]> mapAttributes(ObjectClassHandle objectClassHandle, ObjectRef receivedObjectReflection) {
		Map<String, byte[]> attributes = new HashMap<String, byte[]>();
		try {
			for (Map.Entry<AttributeHandle, byte[]> entry : receivedObjectReflection.getAttributes().entrySet()) {
				String attributeName = rtiAmb.getAttributeName(objectClassHandle, entry.getKey());
				byte[] attributeValue = receivedObjectReflection.getAttributes().get(entry.getKey());
				log.debug(attributeName + "=" + attributeValue);
				attributes.put(attributeName, attributeValue);
			}
		} catch (RTIexception e) {
			log.error("", e);
		}
		return attributes;
	}

	private void joinFederationExecution() throws InterruptedException, RTIAmbassadorException {
		boolean joinSuccessful = false;
		log.trace("Trying to join...	");

		for (int i = 0; !joinSuccessful && i < MAX_JOIN_ATTEMPTS; i++) {
			if (i > 0) {
				log.info("next join attempt in " + REJOIN_DELAY_MS + " ms...");
				Thread.sleep(REJOIN_DELAY_MS);
			}

			log.info("joining federation " + getFederationName() + " as " + getFederateName() + " (" + i + ")");
			try {
				synchronized (rtiAmb) {
					rtiAmb.joinFederationExecution(getFederateName(), getFederationName(), foms);
				}
				setTimeFactory((HLAfloat64TimeFactory) rtiAmb.getTimeFactory());
			} catch (SaveInProgress | RestoreInProgress | RTIinternalError | CouldNotCreateLogicalTimeFactory
					| hla.rti1516e.exceptions.FederationExecutionDoesNotExist
					| hla.rti1516e.exceptions.FederateAlreadyExecutionMember | NotConnected
					| CallNotAllowedFromWithinCallback | hla.rti1516e.exceptions.FederateNotExecutionMember
					| InconsistentFDD | ErrorReadingFDD | CouldNotOpenFDD e) {
				log.error("", e);
			}
			joinSuccessful = true;
		}
	}

	private void publishAndSubscribe() {
		// These interactions are required.
		JoinInteraction joinInteraction = FederatecoreFactory.eINSTANCE.createJoinInteraction();
		joinInteraction.setFederateName(getFederateName());
		publishInteraction(joinInteraction.eClass().getName());
		interObjectInjection.addInteraction(joinInteraction);
		ResignInteraction resignInteraction = FederatecoreFactory.eINSTANCE.createResignInteraction();
		resignInteraction.setFederateName(getFederateName());
		publishInteraction(resignInteraction.eClass().getName());
		interObjectInjection.addInteraction(resignInteraction);

		InteractionClassHandle handle = null;
		for (InteractionClassType classType : getInteractionSubscribe()) {
			log.info("Creating HLA subscription for the interaction " + classType.getName().getValue());
			try {
				handle = rtiAmb.getInteractionClassHandle(classType.getName().getValue());
				rtiAmb.subscribeInteractionClass(handle);
			} catch (NameNotFound | FederateNotExecutionMember | RTIinternalError | InteractionClassNotDefined
					| SaveInProgress | RestoreInProgress | FederateServiceInvocationsAreBeingReportedViaMOM
					| NotConnected e) {
				log.error("Continuing", e);
			}
		}
		for (InteractionClassType classType : getInteractionPublish()) {
			log.info("Creating HLA publication for the interaction " + classType.getName().getValue());
			try {
				handle = rtiAmb.getInteractionClassHandle(classType.getName().getValue());
				rtiAmb.publishInteractionClass(handle);
			} catch (NameNotFound | FederateNotExecutionMember | RTIinternalError | InteractionClassNotDefined
					| SaveInProgress | RestoreInProgress | NotConnected e) {
				log.error("Continuing", e);
			}
		}
		for (ObjectClassType classType : getObjectSubscribe()) {
			log.info("Creating HLA subscription for the object " + classType.getName().getValue());
			try {
				String nname = formatObjectName(classType.getName().getValue());
				log.info("Creating HLA subscription for the object1 " + nname);
				ObjectClassHandle objectHandle = rtiAmb.getObjectClassHandle(nname);
				// AttributeHandleSet attributes =
				// RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
				AttributeHandleSet attributes = (AttributeHandleSet) rtiAmb.getAttributeHandleSetFactory().create();
				for (AttributeType1 attribute : classType.getAttribute()) {
					AttributeHandle attributeHandle = rtiAmb.getAttributeHandle(objectHandle,
							attribute.getName().getValue());
					attributes.add(attributeHandle);
				}
				rtiAmb.subscribeObjectClassAttributes(objectHandle, attributes);
			} catch (NameNotFound | FederateNotExecutionMember | RTIinternalError | SaveInProgress | RestoreInProgress
					| ObjectClassNotDefined | AttributeNotDefined | NotConnected | InvalidObjectClassHandle e) {
				log.error("Continuing", e);
			}
		}
		for (ObjectClassType classType : getObjectPublish()) {
			log.info("Creating HLA publication for the object S" + classType.getName().getValue());
			try {
				String className = formatObjectName(classType.getName().getValue());
				ObjectClassHandle classHandle = rtiAmb.getObjectClassHandle(className);
				log.info("Creating HLA publication for the object1 " + className);
				AttributeHandleSet attributes = (AttributeHandleSet) rtiAmb.getAttributeHandleSetFactory().create();
				for (AttributeType1 attribute : classType.getAttribute()) {
					AttributeHandle attributeHandle = rtiAmb.getAttributeHandle(classHandle,
							attribute.getName().getValue());
					attributes.add(attributeHandle);
				}
				rtiAmb.publishObjectClassAttributes(classHandle, attributes);
			} catch (NameNotFound | FederateNotExecutionMember | RTIinternalError | SaveInProgress | RestoreInProgress
					| ObjectClassNotDefined | AttributeNotDefined | NotConnected | InvalidObjectClassHandle e) {
				log.error("Continuing", e);
			}
		}
	}

	public void sendInteraction(EObject eObject) {
		sendInteraction(eObject, getLogicalTime());
	}

	public void sendInteraction(EObject eObject, Double logicalTime) {
		ParameterHandleValueMap parameters = new HLA1516eParameterHandleValueMap();
		EClass eClass = eObject.eClass();
		String interactionName = eClass.getName();
		InteractionClassHandle interactionClassHandle = null;
		try {
			interactionClassHandle = rtiAmb.getInteractionClassHandle(interactionName);
		} catch (hla.rti1516e.exceptions.NameNotFound | hla.rti1516e.exceptions.FederateNotExecutionMember
				| NotConnected | RTIinternalError e) {
			log.error("sendInteraction ", e);
		}
		for (EAttribute eAttribute : eClass.getEAttributes()) {
			EDataType eDataType = eAttribute.getEAttributeType();
			Object value = eObject.eGet(eAttribute);
			String parameterName = eAttribute.getName();
			try {
				ParameterHandle parameterHandle = rtiAmb.getParameterHandle(interactionClassHandle, parameterName);
				parameters.put(parameterHandle, EcoreUtil.convertToString(eDataType, value).getBytes());
			} catch (hla.rti1516e.exceptions.NameNotFound | InvalidInteractionClassHandle
					| hla.rti1516e.exceptions.FederateNotExecutionMember | NotConnected | RTIinternalError e) {
				log.error("getEAllAttributes ", e);
			}
		}
		sendInteraction(interactionClassHandle, parameters, logicalTime);
	}

	void sendInteraction(InteractionClassHandle interactionClassHandle, ParameterHandleValueMap parameters,
			Double logicalTime) {
		try {
			log.trace("interactionClassHandle=" + interactionClassHandle + " parameters=" + parameters.size()
					+ " logicalTime=" + getLogicalTime());
			if (getLogicalTime() == 0D) {
				rtiAmb.sendInteraction(interactionClassHandle, parameters, generateTag());
			} else {
				HLAfloat64Time time = getTimeFactory().makeTime(getLogicalTime() + getLookahead());
				rtiAmb.sendInteraction(interactionClassHandle, parameters, generateTag(), time);
			}
		} catch (RTIinternalError | InteractionClassNotPublished | InteractionParameterNotDefined
				| hla.rti1516e.exceptions.InteractionClassNotDefined | SaveInProgress | RestoreInProgress
				| hla.rti1516e.exceptions.FederateNotExecutionMember | NotConnected | InvalidLogicalTime e) {
			log.error("", e);
		}
	}

	public void updateObject(ObjectClassHandle classHandle, ObjectInstanceHandle objectHandle,
			AttributeHandleValueMap attributes) {
		try {
			log.debug("suppliedAttributes=" + attributes.size());
			rtiAmb.updateAttributeValues(objectHandle, attributes, generateTag());
		} catch (RTIinternalError | hla.rti1516e.exceptions.AttributeNotOwned
				| hla.rti1516e.exceptions.AttributeNotDefined | ObjectInstanceNotKnown | SaveInProgress
				| RestoreInProgress | hla.rti1516e.exceptions.FederateNotExecutionMember | NotConnected e) {
			log.error("", e);
		} finally {
			log.debug("objectHandle=" + objectHandle);
		}
	}

	public InteractionClassHandle publishInteraction(String interactionName) {
		InteractionClassHandle interactionHandle = null;
		try {
			interactionHandle = rtiAmb.getInteractionClassHandle(interactionName);
			log.debug("interactionName=" + interactionName);
			log.debug("interactionHandle=" + interactionHandle);
			rtiAmb.publishInteractionClass(interactionHandle);
		} catch (InteractionClassNotDefined | FederateNotExecutionMember | SaveInProgress | RestoreInProgress
				| RTIinternalError | NameNotFound | NotConnected e) {
			log.error(e);
		}
		return interactionHandle;
	}

	public ObjectClassHandle publishObject(String objectName, Map<AttributeHandle, byte[]> attrMap) {
		String[] attrs = (String[]) attrMap.keySet().toArray(new String[0]);
		return publishObject(objectName, attrs);
	}

	public ObjectClassHandle publishObject(String objectName, String... attributes) {
		ObjectClassHandle classHandle = null;
		try {
			classHandle = rtiAmb.getObjectClassHandle(objectName);
			AttributeHandleSet attributeSet = rtiAmb.getAttributeHandleSetFactory().create();
			for (String attrName : attributes) {
				AttributeHandle attributeHandle = rtiAmb.getAttributeHandle(classHandle, attrName);
				attributeSet.add(attributeHandle);
			}
			rtiAmb.publishObjectClassAttributes(classHandle, attributeSet);
		} catch (RTIinternalError | AttributeNotDefined | ObjectClassNotDefined | SaveInProgress | RestoreInProgress
				| FederateNotExecutionMember | NotConnected | NameNotFound | InvalidObjectClassHandle e) {
			log.error(e);
		}
		return classHandle;
	}

	public void registerObject(String className, ObjectClassHandle classHandle) {
		try {
			ObjectInstanceHandle objectHandle = registeredObjects.get(className);
			if (objectHandle == null) {
				objectHandle = rtiAmb.registerObjectInstance(classHandle);
				registeredObjects.put(className, objectHandle);
				log.debug("registerObject classHandle=" + classHandle);
				log.debug("registerObject className=" + className);
				log.debug("registerObject objectHandle=" + objectHandle);
			}
		} catch (RTIinternalError | hla.rti1516e.exceptions.ObjectClassNotPublished
				| hla.rti1516e.exceptions.ObjectClassNotDefined | SaveInProgress | RestoreInProgress
				| hla.rti1516e.exceptions.FederateNotExecutionMember | NotConnected e) {
			log.error("", e);
		}
	}

	private byte[] generateTag() {
		return ("" + System.currentTimeMillis()).getBytes();
	}

	@Override
	public void readyToPopulate() {
		try {
			synchronize(SYNCH_POINTS.readyToPopulate);
		} catch (CallNotAllowedFromWithinCallback | RTIinternalError | RTIAmbassadorException e) {
			log.error(e);
		}
	}

	@Override
	public void readyToRun() {
		try {
			synchronize(SYNCH_POINTS.readyToRun);
		} catch (CallNotAllowedFromWithinCallback | RTIinternalError | RTIAmbassadorException e) {
			log.error(e);
		}
	}

	private void synchronize(SYNCH_POINTS point)
			throws CallNotAllowedFromWithinCallback, RTIinternalError, RTIAmbassadorException {
		log.info("waiting for announcement of the synchronization point " + point);
		while (!fedAmb.isSynchronizationPointPending(point.name())) {
			tick();
		}

		try {
			synchronized (rtiAmb) {
				rtiAmb.synchronizationPointAchieved(point.name());
			}
		} catch (SynchronizationPointLabelNotAnnounced | SaveInProgress | RestoreInProgress | FederateNotExecutionMember
				| NotConnected | RTIinternalError e) {
			log.error(e);
		}

		log.info("waiting for federation to synchronize on synchronization point " + point.name());
		while (!fedAmb.isSynchronizationPointPending(point.name())) {
			tick();
		}
		log.info("federation synchronized on " + point.name());
	}
	//
	// public Double advanceLogicalTime() throws RTIAmbassadorException {
	// advancing.set(true);
	// setLogicalTime(fedAmb.getLogicalTime() + getStepsize());
	// log.info("advancing logical time to " + getLogicalTime());
	// try {
	// fedAmb.setTimeAdvancing();
	// HLAfloat64Time time = getTimeFactory().makeTime(fedAmb.getFederateTime() +
	// getLogicalTime());
	// rtiAmb.timeAdvanceRequest(time);
	// } catch (RTIexception e) {
	// throw new RTIAmbassadorException(e);
	// }
	// while (fedAmb.isTimeAdvancing() == true) {
	// tick();
	// }
	// log.info("advanced logical time to " + fedAmb.getLogicalTime());
	// advancing.set(false);
	// return getLogicalTime();
	// }

	private void resignFederationExecution() throws RTIAmbassadorException {
		log.info("resigning from the federation execution " + getFederationName());
		try {
			rtiAmb.resignFederationExecution(ResignAction.NO_ACTION);
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
	}

	// public Set<InteractionClassType> getInteractionSubscribe() {
	// Set<InteractionClassType> set = new HashSet<InteractionClassType>();
	// for (InteractionClassType itr :
	// fom.getInteractions().getInteractionClass().getInteractionClass()) {
	// getInteractions(set, itr, SharingEnumerations.SUBSCRIBE);
	// }
	//
	// return set;
	// }

	public Set<InteractionClassType> getInteractionPublish() {
		Set<InteractionClassType> set = new HashSet<InteractionClassType>();
		for (InteractionClassType itr : fom.getInteractions().getInteractionClass().getInteractionClass()) {
			getInteractions(set, itr, SharingEnumerations.PUBLISH);
		}

		return set;
	}

	// public Set<InteractionClassType> getInteractions(Set<InteractionClassType>
	// set, InteractionClassType itr,
	// SharingEnumerations pubsub) {
	// if (itr.getSharing() != null) {
	// if (itr.getSharing().getValue() == pubsub
	// || itr.getSharing().getValue() == SharingEnumerations.PUBLISH_SUBSCRIBE) {
	// set.add(itr);
	// log.trace("added InteractionClassType.name=" + itr.getName().getValue() +
	// "size=" + set.size());
	// }
	// }
	// for (InteractionClassType itr1 : itr.getInteractionClass()) {
	// getInteractions(set, itr1, pubsub);
	// }
	// return set;
	// }

	public Set<ObjectClassType> getObjectSubscribe() {
		Set<ObjectClassType> set = new HashSet<ObjectClassType>();
		for (ObjectClassType oct : fom.getObjects().getObjectClass().getObjectClass()) {
			log.debug("getObjectSubscribe=" + oct.getName().getValue());
			ObjectClassType oct1 = EcoreUtil.copy(oct);
			getObjects(set, oct1, SharingEnumerations.SUBSCRIBE);
		}

		return set;
	}

	public Set<ObjectClassType> getObjectPublish() {
		Set<ObjectClassType> set = new HashSet<ObjectClassType>();
		for (ObjectClassType oct : fom.getObjects().getObjectClass().getObjectClass()) {
			log.debug("getObjectPublish=" + oct.getName().getValue());
			ObjectClassType oct1 = EcoreUtil.copy(oct);
			getObjects(set, oct1, SharingEnumerations.PUBLISH);
		}

		return set;
	}

	public ObjectClassType getObjects(Set<ObjectClassType> set, ObjectClassType oct, final SharingEnumerations pubsub) {
		log.debug("getObjects=" + oct.getName().getValue());
		Iterator<AttributeType1> itr = oct.getAttribute().iterator();
		while (itr.hasNext()) {
			AttributeType1 attr = itr.next();
			log.debug("processing AttributeType1.name==" + attr.getName().getValue());
			if (attr.getSharing() != null) {
				if (attr.getSharing().getValue() != pubsub
						&& attr.getSharing().getValue() != SharingEnumerations.PUBLISH_SUBSCRIBE) {
					itr.remove();
					log.trace("removed AttributeType1.name=" + attr.getName().getValue());
				}
			}
			if (!oct.getAttribute().isEmpty()) {
				set.add(oct);
				log.trace("added ObjectClassType.name=" + oct.getName().getValue() + " size=" + set.size());
			}
		}
		for (ObjectClassType oct1 : oct.getObjectClass()) {
			getObjects(set, oct1, pubsub);
		}
		return oct;
	}

	public Publish getInterObjectInjection() {
		if (interObjectInjection == null) {
			interObjectInjection = new PublishImpl();
		}
		return interObjectInjection;
	}

	public void setInterObjectInjection(Publish interObjectInjection) {
		this.interObjectInjection = interObjectInjection;
	}

	public Subscribe getInterObjectReception() {
		if (interObjectReception == null) {
			interObjectReception = new SubscribeImpl();
		}
		return interObjectReception;
	}

	public void setInterObjectReception(Subscribe interObjectReception) {
		this.interObjectReception = interObjectReception;
	}

	public TimeStepHook getTimeStepHook() {
		return timeStepHook;
	}

	public void setTimeStepHook(TimeStepHook timeStepHook) {
		this.timeStepHook = timeStepHook;
	}

	public AtomicBoolean getAdvancing() {
		log.trace("advancing=" + advancing.get());
		return advancing;
	}

	public String formatInteractionName(String interactionName) {
		return String.format("%s.%s", INTERACTION_NAME_ROOT, interactionName);
	}

	public String formatObjectName(String objectName) {
		return String.format("%s.%s", OBJECT_NAME_ROOT, objectName);
	}

	public double startLogicalTime() {
		advancing.set(true);
		return getLogicalTime();
	}

	public FederateAmbassador getFedAmb() {
		return fedAmb;
	}

	public void setFedAmb(FederateAmbassador fedAmb) {
		this.fedAmb = fedAmb;
	}

}
