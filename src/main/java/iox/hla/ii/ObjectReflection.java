package iox.hla.ii;

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.ObjectClassHandle;

public class ObjectReflection {

	private ObjectClassHandle objectClass;
	private String objectName;
	private AttributeHandleValueMap attributes;

	public ObjectReflection(ObjectClassHandle objectClass, String objectName, AttributeHandleValueMap theAttributes) {
		this.objectClass = objectClass;
		this.objectName = objectName;
		this.attributes = theAttributes;
	}

	public ObjectClassHandle getObjectClass() {
		return objectClass;
	}

	public String getObjectName() {
		return objectName;
	}

	public int getAttributeCount() {
		return attributes.size();
	}

	public byte[] getAttributeHandle(AttributeHandle index) {
		return attributes.get(index);
	}

	public String getAttributeValue(AttributeHandle index) {
		return decodeString(attributes.get(index));
	}

	public AttributeHandleValueMap getAttributes() {
		return attributes;
	}

	public void setAttributes(AttributeHandleValueMap attributes) {
		this.attributes = attributes;
	}

	private String decodeString(byte[] buffer) {
		// ObjectRoot.cpp does not send a c-string so we do not need to check for \0
		// see ObjectRoot::createDatamemberHandleValuePairSet and
		// ObjectRoot::setAttributes
		return new String(buffer);
	}
}
