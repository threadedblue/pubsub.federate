package iox.hla.ii;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.ieee.standards.ieee1516._2010.InteractionClassType;
import org.ieee.standards.ieee1516._2010.ObjectClassType;
import org.junit.BeforeClass;
import org.junit.Test;

import iox.hla.ii.PubSubFederate;
import iox.hla.ii.exception.RTIAmbassadorException;
import littleints.Int1;
import littleints.LittleintsFactory;

// Tests herein require a fom file populated to a specific state.  The file in question is fom/som.xml.
public class InjectionFederateTest {

	static PubSubFederate sut;
	static Properties props;
	static final String CONFIG_FILE = "config.properties";

	@BeforeClass
	public static void beforeClass() {
		try {
			sut = new PubSubFederate();
			props = new Properties();
			InputStream is = InjectionFederateTest.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
			props.load(is);
		} catch (RTIAmbassadorException | ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testLoadConfiguration() {
		try {
			sut.loadConfiguration(CONFIG_FILE);
			assertEquals(props.getProperty("fom-file"), sut.getFomFilePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetInteractionSubscribe() {
		try {
			sut.loadConfiguration(CONFIG_FILE);
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
			sut.loadConfiguration(CONFIG_FILE);
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
			sut.loadConfiguration(CONFIG_FILE);
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
			sut.loadConfiguration(CONFIG_FILE);
			Set<ObjectClassType> set = sut.getObjectPublish();
			for (ObjectClassType oct : set) {
				System.out.println(oct.getName().getValue());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testEMF() {
		Int1 int1 = LittleintsFactory.eINSTANCE.createInt1();
		int1.setBoolVal(true);
		int1.setIntVal(123);
		int1.setStrVal("ABC");
		String name = int1.eClass().getName();
		sut.injectInteraction(int1, 1D);
	}
}
