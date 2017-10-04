package iox.hla.ii;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandleValueMap;

public class InteractionRef {

	private final InteractionClassHandle interactionClassHandle;
	private final String interactionName;
	private final ParameterHandleValueMap parameters;

	public InteractionRef(InteractionClassHandle interactionClassHandle, String interactionName,
			ParameterHandleValueMap parameters) {
		this.interactionClassHandle = interactionClassHandle;
		this.interactionName = interactionName;
		this.parameters = parameters;
	}

	public InteractionClassHandle getInteractionClassHandle() {
		return interactionClassHandle;
	}

	public String getInteractionName() {
		return interactionName;
	}

	public ParameterHandleValueMap getParameters() {
		return parameters;
	}
}