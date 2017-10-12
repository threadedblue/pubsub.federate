/**
 */
package iox.hla.pubsub;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each operation of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see iox.hla.pubsub.PubsubFactory
 * @model kind="package"
 * @generated
 */
public interface PubsubPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "pubsub";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://pubsub";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "iox.hla.pubsub";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	PubsubPackage eINSTANCE = iox.hla.pubsub.impl.PubsubPackageImpl.init();

	/**
	 * The meta object id for the '{@link iox.hla.pubsub.impl.MessageImpl <em>Message</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see iox.hla.pubsub.impl.MessageImpl
	 * @see iox.hla.pubsub.impl.PubsubPackageImpl#getMessage()
	 * @generated
	 */
	int MESSAGE = 0;

	/**
	 * The feature id for the '<em><b>Message</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MESSAGE__MESSAGE = 0;

	/**
	 * The number of structural features of the '<em>Message</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MESSAGE_FEATURE_COUNT = 1;

	/**
	 * The number of operations of the '<em>Message</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MESSAGE_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link iox.hla.pubsub.impl.HLAInteractionImpl <em>HLA Interaction</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see iox.hla.pubsub.impl.HLAInteractionImpl
	 * @see iox.hla.pubsub.impl.PubsubPackageImpl#getHLAInteraction()
	 * @generated
	 */
	int HLA_INTERACTION = 1;

	/**
	 * The number of structural features of the '<em>HLA Interaction</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int HLA_INTERACTION_FEATURE_COUNT = 0;

	/**
	 * The number of operations of the '<em>HLA Interaction</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int HLA_INTERACTION_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link iox.hla.pubsub.impl.HLAObjectImpl <em>HLA Object</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see iox.hla.pubsub.impl.HLAObjectImpl
	 * @see iox.hla.pubsub.impl.PubsubPackageImpl#getHLAObject()
	 * @generated
	 */
	int HLA_OBJECT = 2;

	/**
	 * The number of structural features of the '<em>HLA Object</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int HLA_OBJECT_FEATURE_COUNT = 0;

	/**
	 * The number of operations of the '<em>HLA Object</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int HLA_OBJECT_OPERATION_COUNT = 0;


	/**
	 * Returns the meta object for class '{@link iox.hla.pubsub.Message <em>Message</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Message</em>'.
	 * @see iox.hla.pubsub.Message
	 * @generated
	 */
	EClass getMessage();

	/**
	 * Returns the meta object for the attribute '{@link iox.hla.pubsub.Message#getMessage <em>Message</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Message</em>'.
	 * @see iox.hla.pubsub.Message#getMessage()
	 * @see #getMessage()
	 * @generated
	 */
	EAttribute getMessage_Message();

	/**
	 * Returns the meta object for class '{@link iox.hla.pubsub.HLAInteraction <em>HLA Interaction</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>HLA Interaction</em>'.
	 * @see iox.hla.pubsub.HLAInteraction
	 * @generated
	 */
	EClass getHLAInteraction();

	/**
	 * Returns the meta object for class '{@link iox.hla.pubsub.HLAObject <em>HLA Object</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>HLA Object</em>'.
	 * @see iox.hla.pubsub.HLAObject
	 * @generated
	 */
	EClass getHLAObject();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	PubsubFactory getPubsubFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each operation of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link iox.hla.pubsub.impl.MessageImpl <em>Message</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see iox.hla.pubsub.impl.MessageImpl
		 * @see iox.hla.pubsub.impl.PubsubPackageImpl#getMessage()
		 * @generated
		 */
		EClass MESSAGE = eINSTANCE.getMessage();

		/**
		 * The meta object literal for the '<em><b>Message</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MESSAGE__MESSAGE = eINSTANCE.getMessage_Message();

		/**
		 * The meta object literal for the '{@link iox.hla.pubsub.impl.HLAInteractionImpl <em>HLA Interaction</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see iox.hla.pubsub.impl.HLAInteractionImpl
		 * @see iox.hla.pubsub.impl.PubsubPackageImpl#getHLAInteraction()
		 * @generated
		 */
		EClass HLA_INTERACTION = eINSTANCE.getHLAInteraction();

		/**
		 * The meta object literal for the '{@link iox.hla.pubsub.impl.HLAObjectImpl <em>HLA Object</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see iox.hla.pubsub.impl.HLAObjectImpl
		 * @see iox.hla.pubsub.impl.PubsubPackageImpl#getHLAObject()
		 * @generated
		 */
		EClass HLA_OBJECT = eINSTANCE.getHLAObject();

	}

} //PubsubPackage
