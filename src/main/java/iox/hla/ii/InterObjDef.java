package iox.hla.ii;

import java.util.Map;

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.InteractionClassHandle;

public class InterObjDef {

	public enum TYPE {
		INTERACTION, OBJECT
	};

	public final TYPE type;

	protected final InteractionClassHandle handle;
	protected final String name;
	protected final Map<AttributeHandle, byte[]> parameters;

	public InterObjDef(InteractionClassHandle handle, String name, Map<String, byte[]> parameters, TYPE type) {
		this.handle = handle;
		this.name = name;
		this.parameters = parameters;
		this.type = type;
	}

	public InteractionClassHandle getHandle() {
		return handle;
	}

	public String getName() {
		return name;
	}

	public Map<AttributeHandle, byte[]> getParameters() {
		return parameters;
	}

	public TYPE getType() {
		return type;
	}

	public String toString() {
		return String.format("name=%s parameters=%d type=%s", name,
				parameters.size(), type.name());
	}

	public boolean isInteraction() {
		return type == TYPE.INTERACTION;
	}

	public boolean isObject() {
		return type == TYPE.OBJECT;
	}
}
