package iox.hla.ii;

import org.portico.impl.hla1516e.types.HLA1516eParameterHandleValueMap;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;

public class Interaction {

	private InteractionClassHandle handle;
	private final String name;
	private ParameterHandleValueMap parameters;

	public Interaction(InteractionClassHandle handle, String name, ParameterHandleValueMap theInteraction) {
		this.handle = handle;
		this.name = name;
		this.parameters = theInteraction;
	}

	public InteractionClassHandle getHandle() {
		return handle;
	}

	public String getName() {
		return name;
	}

	public int getParameterCount() {
		return parameters.size();
	}

	public byte[] getParameterValue(ParameterHandle index) {
		return parameters.get(index);
	}

	public ParameterHandleValueMap getParameters() {
		return parameters;
	}

	public void setParameters(HLA1516eParameterHandleValueMap parameters) {
		this.parameters = parameters;
	}
}
