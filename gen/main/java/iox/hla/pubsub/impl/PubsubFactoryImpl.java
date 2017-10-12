/**
 */
package iox.hla.pubsub.impl;

import iox.hla.pubsub.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class PubsubFactoryImpl extends EFactoryImpl implements PubsubFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static PubsubFactory init() {
		try {
			PubsubFactory thePubsubFactory = (PubsubFactory)EPackage.Registry.INSTANCE.getEFactory(PubsubPackage.eNS_URI);
			if (thePubsubFactory != null) {
				return thePubsubFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new PubsubFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PubsubFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case PubsubPackage.MESSAGE: return createMessage();
			case PubsubPackage.HLA_INTERACTION: return createHLAInteraction();
			case PubsubPackage.HLA_OBJECT: return createHLAObject();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Message createMessage() {
		MessageImpl message = new MessageImpl();
		return message;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public HLAInteraction createHLAInteraction() {
		HLAInteractionImpl hlaInteraction = new HLAInteractionImpl();
		return hlaInteraction;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public HLAObject createHLAObject() {
		HLAObjectImpl hlaObject = new HLAObjectImpl();
		return hlaObject;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PubsubPackage getPubsubPackage() {
		return (PubsubPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static PubsubPackage getPackage() {
		return PubsubPackage.eINSTANCE;
	}

} //PubsubFactoryImpl
