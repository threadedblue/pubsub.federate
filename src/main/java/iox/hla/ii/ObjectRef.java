package iox.hla.ii;

import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;

public class ObjectRef {

	private final ObjectInstanceHandle objectInstanceHandle;
	private final ObjectClassHandle objectClassHandle;
	private final String objectName;
	private final AttributeHandleValueMap attributes;

	public ObjectRef(ObjectInstanceHandle objectInstanceHandle, ObjectClassHandle objectClassHandle, String objectName,
			AttributeHandleValueMap attributes) {
		this.objectInstanceHandle = objectInstanceHandle;
		this.objectClassHandle = objectClassHandle;
		this.objectName = objectName;
		this.attributes = attributes;
	}

	public ObjectInstanceHandle getObjectInstanceHandle() {
		return objectInstanceHandle;
	}

	public ObjectClassHandle getObjectClassHandle() {
		return objectClassHandle;
	}

	public String getObjectName() {
		return objectName;
	}

	public AttributeHandleValueMap getAttributes() {
		return attributes;
	}
}
