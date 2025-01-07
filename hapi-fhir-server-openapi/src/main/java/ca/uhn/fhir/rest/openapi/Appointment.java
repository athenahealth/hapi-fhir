package ca.uhn.fhir.rest.openapi;

import com.fasterxml.jackson.annotation.JsonSetter;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;

public class Appointment extends org.hl7.fhir.r4.model.Appointment {
  @Override
  @JsonSetter
  public Reference setReferenceElement(StringType value) {
    return super.setReferenceElement(value);
  }

}
