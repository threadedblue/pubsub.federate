package iox.hla.ii;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.ecore.EClass;
import org.ieee.standards.ieee1516._2010.InteractionClassType;
import org.ieee.standards.ieee1516._2010.ObjectClassType;
import org.junit.BeforeClass;
import org.junit.Test;

import iox.hla.ii.exception.RTIAmbassadorException;
import iox.sds4emf.Registrar;
import littleints.theseints.TheseintsPackage;
import littleints.thoseInts.ThoseIntsPackage;

// Tests herein require a fom file populated to a specific state.  The file in question is fom/som.xml.
public class PubSubFederateTest {

	static PubSubFederate sut;
	static Properties props;
	static final String CONFIG_FILE = "pubsub.yml";

	@BeforeClass
	public static void beforeClass() {
		try {
			Registrar.registerPackage(TheseintsPackage.eNS_URI, TheseintsPackage.eINSTANCE);
			Registrar.registerPackage(ThoseIntsPackage.eNS_URI, ThoseIntsPackage.eINSTANCE);
			sut = new PubSubFederate();
		} catch (RTIAmbassadorException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testLoadConfiguration() {
		try {
			sut.loadConfiguration("pubsub.ynl");
			assertEquals(props.getProperty("fomFile"), sut.getFomFilePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetInteractionSubscribe() {
		try {
			sut.loadConfiguration("pubsub.ynl");
			Set<InteractionClassType> set = sut.getInteractionSubscribe();
			for (InteractionClassType oct : set) {
				System.out.println(oct.getName().getValue());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetInteractionPublish() {
		try {
			sut.loadConfiguration("pubsub.ynl");
			Set<InteractionClassType> set = sut.getInteractionPublish();
			for (InteractionClassType oct : set) {
				System.out.println(oct.getName().getValue());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetObjectSubscribe() {
		try {
			sut.loadConfiguration("pubsub.ynl");
			Set<ObjectClassType> set = sut.getObjectSubscribe();
			for (ObjectClassType oct : set) {
				System.out.println(oct.getName().getValue());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetObjectPublish() {
		try {
			sut.loadConfiguration("pubsub.ynl");
			Set<ObjectClassType> set = sut.getObjectPublish();
			for (ObjectClassType oct : set) {
				System.out.println(oct.getName().getValue());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void test() {
		EClass eClass  = sut.findEClass("Int1");
		assertNotNull(eClass);
		eClass  = sut.findEClass("Int4");
		assertNotNull(eClass);
		eClass  = sut.findEClass("Int999");
		assertNull(eClass);
	}
	
//	@Test
//	public void testEMF() {
//		Int1 int1 = TheseintsFactory.eINSTANCE.createInt1();
//		int1.setBoolVal(true);
//		int1.setDoubVal(123.321D);
//		int1.setStrVal("ABC");
//		String name = int1.eClass().getName();
//		sut.publishInteraction(interactionName)
//	}
}
