package ca.uhn.fhir.rest.openapi;

import com.fasterxml.jackson.annotation.JsonSetter;
import org.hl7.fhir.r4.model.StringType;

public class Reference extends org.hl7.fhir.r4.model.Reference {
  @Override
  @JsonSetter
  public org.hl7.fhir.r4.model.Reference setReferenceElement(StringType value) {
    return super.setReferenceElement(value);
  }

}
