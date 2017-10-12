/**
 */
package iox.hla.pubsub;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see iox.hla.pubsub.PubsubPackage
 * @generated
 */
public interface PubsubFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	PubsubFactory eINSTANCE = iox.hla.pubsub.impl.PubsubFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Message</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Message</em>'.
	 * @generated
	 */
	Message createMessage();

	/**
	 * Returns a new object of class '<em>HLA Interaction</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>HLA Interaction</em>'.
	 * @generated
	 */
	HLAInteraction createHLAInteraction();

	/**
	 * Returns a new object of class '<em>HLA Object</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>HLA Object</em>'.
	 * @generated
	 */
	HLAObject createHLAObject();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	PubsubPackage getPubsubPackage();

} //PubsubFactory
