package iox.hla.ii;

import java.io.File;
import java.io.IOException;
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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.ieee.standards.ieee1516._2010.AttributeType1;
import org.ieee.standards.ieee1516._2010.DocumentRoot;
import org.ieee.standards.ieee1516._2010.InteractionClassType;
import org.ieee.standards.ieee1516._2010.ObjectClassType;
import org.ieee.standards.ieee1516._2010.ObjectModelType;
import org.ieee.standards.ieee1516._2010.SharingEnumerations;
import org.ieee.standards.ieee1516._2010._2010Package;
import org.ieee.standards.ieee1516._2010.util._2010ResourceFactoryImpl;
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
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.AlreadyConnected;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.CallNotAllowedFromWithinCallback;
import hla.rti1516e.exceptions.ConnectionFailed;
import hla.rti1516e.exceptions.CouldNotCreateLogicalTimeFactory;
import hla.rti1516e.exceptions.CouldNotOpenFDD;
import hla.rti1516e.exceptions.ErrorReadingFDD;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateServiceInvocationsAreBeingReportedViaMOM;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
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
import hla.rti1516e.exceptions.TimeConstrainedAlreadyEnabled;
import hla.rti1516e.exceptions.TimeRegulationAlreadyEnabled;
import hla.rti1516e.exceptions.UnsupportedCallbackModel;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import iox.hla.ii.config.InteractionPublicationConfig;
import iox.hla.ii.exception.RTIAmbassadorException;
import iox.sds4emf.Deserialize;
import pubsub.Message;
import pubsub.PubsubFactory;

public class PubSubFederate implements Runnable {
	
	private static final Logger log = LogManager.getLogger(PubSubFederate.class);

	private static final int MAX_JOIN_ATTEMPTS = 6;
	private static final int REJOIN_DELAY_MS = 10000;

	public static final String INTERACTION_NAME_ROOT = "HLAinteractionRoot";
	public static final String OBJECT_NAME_ROOT = "HLAobjectRoot";

	public enum State {
		CONSTRUCTED, INITIALIZED, JOINED, TERMINATING;
	};

	public enum SYNCH_POINTS {
		readyToPopulate, readyToRun, readyToResign
	};

	private State state = State.CONSTRUCTED;
	private Double logicalTime;
	private InteractionPublicationConfig iiConfig;
	private Map<String, ObjectInstanceHandle> registeredObjects = new HashMap<String, ObjectInstanceHandle>();
	private HLAfloat64TimeFactory timeFactory; // set when we join
	protected EncoderFactory encoderFactory; // set when we join

	RTIambassador getRtiAmb() {
		return rtiAmb;
	}

	FederateAmbassador getFedAmb() {
		return fedAmb;
	}

	private RTIambassador rtiAmb;
	private FederateAmbassador fedAmb;
	private ObjectModelType fom;
	private URL[] foms;
	// set of object names that have been created as injectable entities
	private HashSet<String> discoveredObjects = new HashSet<String>();

	private String federateName;
	private String federationName;
	private static String fomFilePath;

	private Publish interObjectInjection;

	private Subscribe interObjectReception;

	private TimeStepHook timeStepHook;

	private AtomicBoolean advancing = new AtomicBoolean(false);

	public String getFomFilePath() {
		return fomFilePath;
	}

	private double lookahead;

	public State getState() {
		return state;
	}

	public ObjectModelType getFom() {
		return fom;
	}

	public String getFederateName() {
		return federateName;
	}

	public String getFederationName() {
		return federationName;
	}

	public double getStepsize() {
		return stepsize;
	}

	private double stepsize;

	public PubSubFederate() throws RTIAmbassadorException, ParserConfigurationException {

		try {
			rtiAmb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
			encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
			fedAmb = new FederateAmbassador(rtiAmb);
			rtiAmb.connect(fedAmb, CallbackModel.HLA_EVOKED);
		} catch (RTIinternalError | ConnectionFailed | InvalidLocalSettingsDesignator | UnsupportedCallbackModel
				| AlreadyConnected | CallNotAllowedFromWithinCallback e) {
			throw new RTIAmbassadorException(e);
		}
	}

	public void init() {
		log.trace("init==>");
		if (state != State.INITIALIZED) {
			throw new IllegalStateException("execute cannot be called in the current state: " + state.name());
		}

		try {
			joinFederationExecution();
			changeState(State.JOINED);

			enableAsynchronousDelivery();
			enableTimeConstrained();
			enableTimeRegulation();
			publishAndSubscribe();

		} catch (InterruptedException | RTIAmbassadorException e) {
			log.error(e);
		}
		log.trace("<==init");
	}

	public void loadConfiguration(String filepath) throws IOException {
		if (state != State.CONSTRUCTED && state != State.INITIALIZED) {
			throw new IllegalStateException("loadConfiguration cannot be called in the current state: " + state.name());
		}

		log.debug("loading injection federate configuration");
		Path configPath = Paths.get(filepath);
		File configFile = configPath.toFile();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		iiConfig = mapper.readValue(configFile, InteractionPublicationConfig.class);
		federateName = iiConfig.getFederateName();
		log.debug("federate-name=" + federateName);
		federationName = iiConfig.getFederation();
		log.debug("federation=" + federationName);
		fomFilePath = iiConfig.getFomFile();
		log.debug("fomFile=" + fomFilePath);
		lookahead = iiConfig.getLookahead();
		log.debug("lookahead=" + lookahead);
		stepsize = iiConfig.getStepsize();
		log.debug("stepsize=" + stepsize);
		URL[] urls = { new File(fomFilePath).getAbsoluteFile().toURI().toURL() };
		foms = urls;
		fom = loadFOM();
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
			// rtiAmb.registerFederationSynchronizationPoint(
			// SYNCH_POINTS.readyToPopulate.name(), null );
			synchronize(SYNCH_POINTS.readyToPopulate);
			timeStepHook.afterReadytoPopulate();
		} catch (RTIAmbassadorException | RTIexception e) {
			log.error(e);
		}

		try {
			timeStepHook.beforeReadytoRun();
			// rtiAmb.registerFederationSynchronizationPoint(
			// SYNCH_POINTS.readyToRun.name(), null );
			synchronize(SYNCH_POINTS.readyToRun);
			timeStepHook.afterReadytoRun();
		} catch (RTIAmbassadorException | RTIexception e) {
			log.error(e);
		}

		try {
			log.info("enter while==>" + state.name());
			while (state != State.TERMINATING) {

				handleMessages();
				processIntObjs(logicalTime);

				timeStepHook.beforeAdvanceLogicalTime();
				advanceLogicalTime();
				timeStepHook.afterAdvanceLogicalTime();

			}
		} catch (RTIAmbassadorException e) {
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
		Queue<EObject> interactions = null;
		if (logicalTime == null) {
			interactions = interObjectInjection.getPreSynchInteractions();
		} else {
			interactions = interObjectInjection.getPublications(logicalTime);
		}

		EObject def = null;
		while ((def = interactions.poll()) != null) {
			sendInteraction(def, logicalTime);
		}
	}

	private void handleMessages() throws RTIAmbassadorException {

		boolean receivedNothing = true;
		try {
			InteractionRef receivedInteraction;
			while ((receivedInteraction = fedAmb.nextInteraction()) != null) {
				// log.trace("receivedInteraction=" + receivedInteraction);
				receivedNothing = false;
				EClass eClass = findEClass(receivedInteraction.getInteractionName());
				EObject eObject = EcoreUtil.create(eClass);

				for (Map.Entry<ParameterHandle, byte[]> entry : receivedInteraction.getParameters().entrySet()) {
					String interactionName = rtiAmb.getParameterName(receivedInteraction.getInteractionClassHandle(),
							entry.getKey());
					EAttribute eAttribute = (EAttribute) eClass.getEStructuralFeature(interactionName);
					EDataType eDataType = eAttribute.getEAttributeType();
					Object obj = EcoreUtil.createFromString(eDataType, new String(entry.getValue()));
					eObject.eSet(eAttribute, obj);
				}

				interObjectReception.receiveInteraction(logicalTime, eObject);
			}

			ObjectRef receivedObjectReflection;
			while ((receivedObjectReflection = fedAmb.nextObjectReflection()) != null) {
				// log.trace("receivedObjectReflection=" +
				// receivedObjectReflection);
				receivedNothing = false;
				EClass eClass = findEClass(receivedInteraction.getInteractionName());
				EObject eObject = EcoreUtil.create(eClass);

				for (Map.Entry<ParameterHandle, byte[]> entry : receivedInteraction.getParameters().entrySet()) {
					String interactionName = rtiAmb.getParameterName(receivedInteraction.getInteractionClassHandle(),
							entry.getKey());
					EAttribute eAttribute = (EAttribute) eClass.getEStructuralFeature(interactionName);
					EDataType eDataType = eAttribute.getEAttributeType();
					Object obj = EcoreUtil.createFromString(eDataType, new String(entry.getValue()));
					eObject.eSet(eAttribute, obj);
				}
				interObjectReception.receiveObject(logicalTime, eObject);
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
				Message msg = PubsubFactory.eINSTANCE.createMessage();
				msg.setMessage(String.format("%s %s", lt.toString(), "Nothing received!!"));
				interObjectReception.receiveInteraction(logicalTime, msg);
			}
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
	}

	EClass findEClass(String objectName) {
		EClassifier eClassifier = null;
		for (Map.Entry<String, Object> entry : EPackage.Registry.INSTANCE.entrySet()) {
			String key = entry.getKey();
			EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(key);
			eClassifier = ePackage.getEClassifier(objectName);
			if (eClassifier != null) {
				break;
			}
		}
		return (EClass) eClassifier;
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

	public void tick() throws RTIAmbassadorException, CallNotAllowedFromWithinCallback, RTIinternalError {
		rtiAmb.evokeMultipleCallbacks(0.1, 0.2);
	}

	private void joinFederationExecution() throws InterruptedException, RTIAmbassadorException {
		boolean joinSuccessful = false;
		log.trace("Trying to join...	");

		for (int i = 0; !joinSuccessful && i < MAX_JOIN_ATTEMPTS; i++) {
			if (i > 0) {
				log.info("next join attempt in " + REJOIN_DELAY_MS + " ms...");
				Thread.sleep(REJOIN_DELAY_MS);
			}

			log.info("joining federation " + federationName + " as " + federateName + " (" + i + ")");
			try {
				synchronized (rtiAmb) {
					rtiAmb.createFederationExecution(federationName, foms);
					rtiAmb.joinFederationExecution(federateName, federationName);
				}
				this.timeFactory = (HLAfloat64TimeFactory) rtiAmb.getTimeFactory();
			} catch (SaveInProgress | RestoreInProgress | RTIinternalError | CouldNotCreateLogicalTimeFactory
					| hla.rti1516e.exceptions.FederationExecutionDoesNotExist
					| hla.rti1516e.exceptions.FederateAlreadyExecutionMember | NotConnected
					| CallNotAllowedFromWithinCallback | hla.rti1516e.exceptions.FederateNotExecutionMember
					| InconsistentFDD | ErrorReadingFDD | CouldNotOpenFDD | FederationExecutionAlreadyExists e) {
				log.error("", e);
			}
			joinSuccessful = true;
		}
	}

	// enable Receive Order messages during any tick call
	public void enableAsynchronousDelivery() throws RTIAmbassadorException {
		try {
			log.info("enabling asynchronous delivery of receive order messages");
			rtiAmb.enableAsynchronousDelivery();
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
	}

	private void enableTimeConstrained() throws RTIAmbassadorException {
		try {
			log.info("enabling time constrained");
			rtiAmb.enableTimeConstrained();
			while (fedAmb.isTimeConstrained() == false) {
				tick();
			}
		} catch (TimeConstrainedAlreadyEnabled e) {
			log.info("time constrained already enabled");
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
	}

	private void enableTimeRegulation() throws RTIAmbassadorException {
		try {
			log.info("enabling time regulation");
			HLAfloat64Interval lookahead = timeFactory.makeInterval(fedAmb.getFederateLookahead());
			rtiAmb.enableTimeRegulation(lookahead);
			while (fedAmb.isTimeRegulating() == false) {
				tick();
			}
		} catch (TimeRegulationAlreadyEnabled e) {
			log.info("time regulation already enabled");
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
	}

	ObjectModelType loadFOM() {
		Deserialize.associateExtension("xml", new _2010ResourceFactoryImpl());
		Deserialize.registerPackage(_2010Package.eNS_URI, _2010Package.eINSTANCE);
		DocumentRoot docRoot = (DocumentRoot) Deserialize.it(fomFilePath);
		return docRoot.getObjectModel();
	}

	private void publishAndSubscribe() {
		InteractionClassHandle handle = null;
		for (InteractionClassType classType : getInteractionSubscribe()) {
			log.info("creating HLA subscription for the interaction=" + classType.getName().getValue());
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
			log.info("creating HLA publication for the interaction=" + classType.getName().getValue());
			try {
				handle = rtiAmb.getInteractionClassHandle(classType.getName().getValue());
				rtiAmb.publishInteractionClass(handle);
			} catch (NameNotFound | FederateNotExecutionMember | RTIinternalError | InteractionClassNotDefined
					| SaveInProgress | RestoreInProgress | NotConnected e) {
				log.error("Continuing", e);
			}
		}
		for (ObjectClassType classType : getObjectSubscribe()) {
			log.info("creating HLA subscription for the object=" + classType.getName().getValue());
			try {
				String nname = formatObjectName(classType.getName().getValue());
				log.info("creating HLA subscription for the object1=" + nname);
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
			log.info("creating HLA publication for the object=" + classType.getName().getValue());
			try {
				String className = formatObjectName(classType.getName().getValue());
				ObjectClassHandle classHandle = rtiAmb.getObjectClassHandle(className);
				log.info("creating HLA publication for the object1=" + className);
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

	public void sendInteraction(EObject eObject, Double logicalTime) {
		ParameterHandleValueMap parameters = new HLA1516eParameterHandleValueMap();
		EClass eClass = eObject.eClass();
		String interactionName = eClass.getName();
		InteractionClassHandle interactionHandle = null;
		try {
			interactionHandle = rtiAmb.getInteractionClassHandle(interactionName);
		} catch (hla.rti1516e.exceptions.NameNotFound | hla.rti1516e.exceptions.FederateNotExecutionMember
				| NotConnected | RTIinternalError e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (EAttribute eAttribute : eClass.getEAllAttributes()) {
			EDataType eDataType = eAttribute.getEAttributeType();
			Object value = eObject.eGet(eAttribute);
			String parameterName = eAttribute.getName();
			try {
				ParameterHandle parameterHandle = rtiAmb.getParameterHandle(interactionHandle, parameterName);
				parameters.put(parameterHandle, EcoreUtil.convertToString(eDataType, value).getBytes());
			} catch (hla.rti1516e.exceptions.NameNotFound | InvalidInteractionClassHandle
					| hla.rti1516e.exceptions.FederateNotExecutionMember | NotConnected | RTIinternalError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		sendInteraction(eClass.getName(), parameters, logicalTime);
	}

	// public void injectInteraction(InterObjDef def, Double logicalTime) {
	// injectInteraction(def.getName(), def.getParameters(), logicalTime);
	// }

	public void sendInteraction(String interactionName, ParameterHandleValueMap parameters, Double logicalTime) {
		InteractionClassHandle interactionHandle;
		try {
			interactionHandle = rtiAmb.getInteractionClassHandle(interactionName);
			log.debug("interactionName=" + interactionName);
			log.debug("interactionHandle=" + interactionHandle);
			if (logicalTime == null) {
				rtiAmb.sendInteraction(interactionHandle, parameters, generateTag());
			} else {
				HLAfloat64Time time = timeFactory.makeTime(fedAmb.getFederateTime() + fedAmb.getFederateLookahead());
				rtiAmb.sendInteraction(interactionHandle, parameters, generateTag(), time);
			}
		} catch (RTIinternalError | InteractionClassNotPublished | InteractionParameterNotDefined
				| hla.rti1516e.exceptions.InteractionClassNotDefined | SaveInProgress | RestoreInProgress
				| hla.rti1516e.exceptions.FederateNotExecutionMember | NotConnected | InvalidLogicalTime
				| hla.rti1516e.exceptions.NameNotFound e) {
			log.error("", e);
		}
	}

	// private byte[] convertToByteArray(double value) {
	// ByteBuffer buffer = ByteBuffer.allocate(8);
	// buffer.putDouble(value);
	// return buffer.array();
	//
	// }

	// public SuppliedParameters assembleParameters(InteractionClassHandle
	// interactionHandle, Map<String, String> parameters) {
	// SuppliedParameters suppliedParameters = null;
	// try {
	// suppliedParameters =
	// RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
	// for (Map.Entry<String, String> entry : parameters.entrySet()) {
	// ParameterHandle parameterHandle =
	// rtiAmb.getParameterHandle(interactionHandle, entry.getKey());
	// byte[] parameterValue = entry.getValue().getBytes();
	// suppliedParameters.add(parameterHandle, parameterValue);
	// }
	// } catch (NameNotFound | FederateNotExecutionMember | RTIinternalError e) {
	// log.error("", e);
	// } catch (InteractionClassNotDefined e) {
	// log.error("", e);
	// }
	// return suppliedParameters;
	// }

	// public void updateObject(InterObjDef def) {
	// int classHandle = -1;
	// ObjectClassHandle objectHandle = -1;
	// try {
	// classHandle = getRtiAmb().getObjectClassHandle(def.getName());
	// objectHandle = getRtiAmb().registerObjectInstance(classHandle);
	// updateObject(classHandle, objectHandle, def.getParameters());
	// } catch (NullPointerException | FederateNotExecutionMember | RTIinternalError
	// | NameNotFound
	// | ObjectClassNotDefined | ObjectClassNotPublished | SaveInProgress |
	// RestoreInProgress
	// | ConcurrentAccessAttempted e) {
	// log.debug("registeredObjects=" + registeredObjects);
	// log.debug("def=" + def);
	// log.debug("classHandle=" + classHandle);
	// log.debug("objectHandle=" + objectHandle);
	// log.error(e);
	// }
	// }

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

	// public SuppliedAttributes assembleAttributes(int classHandle, Map<String,
	// String> attributes) {
	// SuppliedAttributes suppliedAttributes = null;
	// try {
	// suppliedAttributes =
	// RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
	// for (Map.Entry<String, String> entry : attributes.entrySet()) {
	// int attributeHandle = rtiAmb.getAttributeHandle(entry.getKey(), classHandle);
	// byte[] attributeValue = entry.getValue().getBytes();
	// suppliedAttributes.add(attributeHandle, attributeValue);
	// }
	// } catch (RTIinternalError e) {
	// log.error("", e);
	// } catch (ObjectClassNotDefined e) {
	// log.error("", e);
	// } catch (NameNotFound e) {
	// log.error("", e);
	// } catch (FederateNotExecutionMember e) {
	// log.error("", e);
	// }
	// return suppliedAttributes;
	// }

	// public int publishInteraction(InterObjDef def) {
	// return publishInteraction(def.getName());
	// }

	public InteractionClassHandle publishInteraction(String interactionName) {
		InteractionClassHandle classHandle = null;
		try {
			classHandle = getRtiAmb().getInteractionClassHandle(interactionName);
			getRtiAmb().publishInteractionClass(classHandle);
		} catch (InteractionClassNotDefined | FederateNotExecutionMember | SaveInProgress | RestoreInProgress
				| RTIinternalError | NameNotFound | NotConnected e) {
			log.error(e);
		}
		return classHandle;
	}

	// public ObjectClassHandle publishObject(InterObjDef def) {
	// return publishObject(def.getName(), def.getParameters());
	// }

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

	private void synchronize(SYNCH_POINTS point) throws RTIAmbassadorException, RTIexception {
		log.info("waiting for announcement of the synchronization point " + point);
		while (!fedAmb.isSynchronizationPointPending(point.name())) {
			tick();
		}

		try {
			synchronized (rtiAmb) {
				rtiAmb.synchronizationPointAchieved(point.name());
			}
		} catch (SynchronizationPointLabelNotAnnounced e) {
			log.error(e);
		} catch (hla.rti1516e.exceptions.SaveInProgress e) {
			log.error(e);
		} catch (hla.rti1516e.exceptions.RestoreInProgress e) {
			log.error(e);
		} catch (hla.rti1516e.exceptions.FederateNotExecutionMember e) {
			log.error(e);
		} catch (NotConnected e) {
			log.error(e);
		} catch (hla.rti1516e.exceptions.RTIinternalError e) {
			log.error(e);
		}

		log.info("waiting for federation to synchronize on synchronization point " + point.name());
		while (!fedAmb.isSynchronizationPointPending(point.name())) {
			tick();
		}
		log.info("federation synchronized on " + point.name());
	}

	public Double advanceLogicalTime() throws RTIAmbassadorException {
		advancing.set(true);
		logicalTime = fedAmb.getLogicalTime() + stepsize;
		log.info("advancing logical time to " + logicalTime);
		try {
			fedAmb.setTimeAdvancing();
			HLAfloat64Time time = timeFactory.makeTime(fedAmb.getFederateTime() + logicalTime);
			rtiAmb.timeAdvanceRequest(time);
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
		while (fedAmb.isTimeAdvancing() == true) {
			try {
				tick();
			} catch (CallNotAllowedFromWithinCallback | RTIinternalError e) {
				log.error(e);
			}
		}
		log.info("advanced logical time to " + fedAmb.getLogicalTime());
		advancing.set(false);
		return logicalTime;
	}

	private void resignFederationExecution() throws RTIAmbassadorException {
		log.info("resigning from the federation execution " + federationName);
		try {
			rtiAmb.resignFederationExecution(ResignAction.NO_ACTION);
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
	}

	public Set<InteractionClassType> getInteractionSubscribe() {
		Set<InteractionClassType> set = new HashSet<InteractionClassType>();
		for (InteractionClassType itr : getFom().getInteractions().getInteractionClass().getInteractionClass()) {
			getInteractions(set, itr, SharingEnumerations.SUBSCRIBE);
		}

		return set;
	}

	public Set<InteractionClassType> getInteractionPublish() {
		Set<InteractionClassType> set = new HashSet<InteractionClassType>();
		for (InteractionClassType itr : getFom().getInteractions().getInteractionClass().getInteractionClass()) {
			getInteractions(set, itr, SharingEnumerations.PUBLISH);
		}

		return set;
	}

	public Set<InteractionClassType> getInteractions(Set<InteractionClassType> set, InteractionClassType itr,
			SharingEnumerations pubsub) {
		if (itr.getSharing() != null) {
			if (itr.getSharing().getValue() == pubsub
					|| itr.getSharing().getValue() == SharingEnumerations.PUBLISH_SUBSCRIBE) {
				set.add(itr);
				log.trace("added InteractionClassType.name=" + itr.getName().getValue() + "size=" + set.size());
			}
		}
		for (InteractionClassType itr1 : itr.getInteractionClass()) {
			getInteractions(set, itr1, pubsub);
		}
		return set;
	}

	public Set<ObjectClassType> getObjectSubscribe() {
		Set<ObjectClassType> set = new HashSet<ObjectClassType>();
		for (ObjectClassType oct : getFom().getObjects().getObjectClass().getObjectClass()) {
			log.debug("getObjectSubscribe=" + oct.getName().getValue());
			ObjectClassType oct1 = EcoreUtil.copy(oct);
			getObjects(set, oct1, SharingEnumerations.SUBSCRIBE);
		}

		return set;
	}

	public Set<ObjectClassType> getObjectPublish() {
		Set<ObjectClassType> set = new HashSet<ObjectClassType>();
		for (ObjectClassType oct : getFom().getObjects().getObjectClass().getObjectClass()) {
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
		return interObjectInjection;
	}

	public void setInterObjectInjection(Publish interObjectInjection) {
		this.interObjectInjection = interObjectInjection;
	}

	public Subscribe getInterObjectReception() {
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
		return logicalTime;
	}

	Double getLogicalTime() {
		return logicalTime;
	}

	void setLogicalTime(Double logicalTime) {
		this.logicalTime = logicalTime;
	}

}
