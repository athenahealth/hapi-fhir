package ca.uhn.fhir.parser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.test.utilities.UuidUtils;
import ca.uhn.fhir.util.ClasspathUtil;
import ca.uhn.fhir.util.TestUtil;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.dstu2.model.Address;
import org.hl7.fhir.dstu2.model.Address.AddressUse;
import org.hl7.fhir.dstu2.model.Address.AddressUseEnumFactory;
import org.hl7.fhir.dstu2.model.Binary;
import org.hl7.fhir.dstu2.model.Bundle;
import org.hl7.fhir.dstu2.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu2.model.CodeableConcept;
import org.hl7.fhir.dstu2.model.Conformance;
import org.hl7.fhir.dstu2.model.Conformance.UnknownContentCode;
import org.hl7.fhir.dstu2.model.DateTimeType;
import org.hl7.fhir.dstu2.model.DateType;
import org.hl7.fhir.dstu2.model.DecimalType;
import org.hl7.fhir.dstu2.model.DiagnosticReport;
import org.hl7.fhir.dstu2.model.EnumFactory;
import org.hl7.fhir.dstu2.model.Enumeration;
import org.hl7.fhir.dstu2.model.Extension;
import org.hl7.fhir.dstu2.model.HumanName;
import org.hl7.fhir.dstu2.model.Identifier.IdentifierUse;
import org.hl7.fhir.dstu2.model.InstantType;
import org.hl7.fhir.dstu2.model.List_;
import org.hl7.fhir.dstu2.model.Narrative.NarrativeStatus;
import org.hl7.fhir.dstu2.model.Observation;
import org.hl7.fhir.dstu2.model.Organization;
import org.hl7.fhir.dstu2.model.Patient;
import org.hl7.fhir.dstu2.model.Patient.ContactComponent;
import org.hl7.fhir.dstu2.model.PrimitiveType;
import org.hl7.fhir.dstu2.model.QuestionnaireResponse;
import org.hl7.fhir.dstu2.model.QuestionnaireResponse.QuestionAnswerComponent;
import org.hl7.fhir.dstu2.model.Reference;
import org.hl7.fhir.dstu2.model.Specimen;
import org.hl7.fhir.dstu2.model.StringType;
import org.hl7.fhir.dstu2.model.ValueSet;
import org.hl7.fhir.dstu2.model.ValueSet.ConceptDefinitionComponent;
import org.hl7.fhir.dstu2.model.ValueSet.ValueSetCodeSystemComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static ca.uhn.fhir.test.utilities.UuidUtils.UUID_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonParserHl7OrgDstu2Test {
  private static FhirContext ourCtx = FhirContext.forDstu2Hl7Org();
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(JsonParserHl7OrgDstu2Test.class);

  @AfterEach
  public void after() {
    ourCtx.setNarrativeGenerator(null);
  }

  @AfterAll
  public static void afterClassClearContext() {
    TestUtil.randomizeLocaleAndTimezone();
  }
  
	@Test
	public void testOverrideResourceIdWithBundleEntryFullUrlEnabled() {
		try {
			String tmp = "{\"resourceType\":\"Bundle\",\"entry\":[{\"fullUrl\":\"http://lalaland.org/patient/pat1\",\"resource\":{\"resourceType\":\"Patient\",\"id\":\"patxuzos\"}}]}";
			Bundle bundle = (Bundle) ourCtx.newJsonParser().parseResource(tmp);
			assertThat(bundle.getEntry()).hasSize(1);
			{
				Patient o1 = (Patient) bundle.getEntry().get(0).getResource();
				IIdType o1Id = o1.getIdElement();
				assertEquals("http://lalaland.org", o1Id.getBaseUrl());
				assertEquals("patient", o1Id.getResourceType());
				assertEquals("pat1", o1Id.getIdPart());
				assertFalse(o1Id.hasVersionIdPart());
			}
		} finally {
			// ensure we cleanup ourCtx so other tests continue to work
			ourCtx = FhirContext.forDstu2Hl7Org();
		}
	}

	@Test
	public void testOverrideResourceIdWithBundleEntryFullUrlDisabled_ConfiguredOnFhirContext() {
		try {
			String tmp = "{\"resourceType\":\"Bundle\",\"entry\":[{\"fullUrl\":\"http://lalaland.org/patient/pat1\",\"resource\":{\"resourceType\":\"Patient\",\"id\":\"patxuzos\"}}]}";
			ourCtx.getParserOptions().setOverrideResourceIdWithBundleEntryFullUrl(false);
			Bundle bundle = (Bundle) ourCtx.newJsonParser().parseResource(tmp);
			assertThat(bundle.getEntry()).hasSize(1);
			{
				Patient o1 = (Patient) bundle.getEntry().get(0).getResource();
				IIdType o1Id = o1.getIdElement();
				assertFalse(o1Id.hasBaseUrl());
				assertEquals("Patient", o1Id.getResourceType());
				assertEquals("patxuzos", o1Id.getIdPart());
				assertFalse(o1Id.hasVersionIdPart());
			}
		} finally {
			// ensure we cleanup ourCtx so other tests continue to work
			ourCtx = FhirContext.forDstu2Hl7Org();
		}
	}

	@Test
	public void testOverrideResourceIdWithBundleEntryFullUrlDisabled_ConfiguredOnParser() {
		try {
			String tmp = "{\"resourceType\":\"Bundle\",\"entry\":[{\"fullUrl\":\"http://lalaland.org/patient/pat1\",\"resource\":{\"resourceType\":\"Patient\",\"id\":\"patxuzos\"}}]}";
			Bundle bundle = (Bundle) ourCtx.newJsonParser().setOverrideResourceIdWithBundleEntryFullUrl(false).parseResource(tmp);
			assertThat(bundle.getEntry()).hasSize(1);
			{
				Patient o1 = (Patient) bundle.getEntry().get(0).getResource();
				IIdType o1Id = o1.getIdElement();
				assertFalse(o1Id.hasBaseUrl());
				assertEquals("Patient", o1Id.getResourceType());
				assertEquals("patxuzos", o1Id.getIdPart());
				assertFalse(o1Id.hasVersionIdPart());
			}
		} finally {
			// ensure we cleanup ourCtx so other tests continue to work
			ourCtx = FhirContext.forDstu2Hl7Org();
		}
	}

  @Test
  public void testEncodeUndeclaredExtensionWithEnumerationContent() {
    IParser parser = ourCtx.newJsonParser();

    Patient patient = new Patient();
    patient.addAddress().setUse(AddressUse.HOME);
    EnumFactory<AddressUse> fact = new AddressUseEnumFactory();
    PrimitiveType<AddressUse> enumeration = new Enumeration<>(fact).setValue(AddressUse.HOME);
    patient.addExtension().setUrl("urn:foo").setValue(enumeration);

    String val = parser.encodeResourceToString(patient);
    ourLog.info(val);
    assertThat(val).contains("\"extension\":[{\"url\":\"urn:foo\",\"valueCode\":\"home\"}]");

    MyPatientWithOneDeclaredEnumerationExtension actual = parser.parseResource(MyPatientWithOneDeclaredEnumerationExtension.class, val);
		assertEquals(AddressUse.HOME, patient.getAddress().get(0).getUse());
    Enumeration<AddressUse> ref = actual.getFoo();
		assertEquals("home", ref.getValue().toCode());

  }

  @Test
  public void testEncodeNarrativeSuppressed() throws Exception {
    Patient patient = new Patient();
    patient.setId("Patient/1/_history/1");
    patient.getText().setDivAsString("<div>THE DIV</div>");
    patient.addName().addFamily("FAMILY");
    patient.getMaritalStatus().addCoding().setCode("D");

    String encoded = ourCtx.newJsonParser().setPrettyPrint(true).setSuppressNarratives(true).encodeResourceToString(patient);
    ourLog.info(encoded);

		assertThat(encoded).contains("Patient");
    assertThat(encoded).containsSubsequence(Constants.TAG_SUBSETTED_SYSTEM_DSTU3, Constants.TAG_SUBSETTED_CODE);
		assertThat(encoded).doesNotContain("text");
		assertThat(encoded).doesNotContain("THE DIV");
		assertThat(encoded).contains("family");
		assertThat(encoded).contains("maritalStatus");
  }

  @Test
  public void testEncodeAndParseExtensions() throws Exception {

    Patient patient = new Patient();
    patient.addIdentifier().setUse(IdentifierUse.OFFICIAL).setSystem("urn:example").setValue("7000135");

    Extension ext = new Extension();
    ext.setUrl("http://example.com/extensions#someext");
    ext.setValue(new DateTimeType("2011-01-02T11:13:15"));
    patient.getExtension().add(ext);

    Extension parent = new Extension().setUrl("http://example.com#parent");
    patient.getExtension().add(parent);
    Extension child1 = new Extension().setUrl("http://example.com#child").setValue(new StringType("value1"));
    parent.getExtension().add(child1);
    Extension child2 = new Extension().setUrl("http://example.com#child").setValue(new StringType("value2"));
    parent.getExtension().add(child2);

    Extension modExt = new Extension();
    modExt.setUrl("http://example.com/extensions#modext");
    modExt.setValue(new DateType("1995-01-02"));
    patient.getModifierExtension().add(modExt);

    HumanName name = patient.addName();
    name.addFamily("Blah");
    StringType given = name.addGivenElement();
    given.setValue("Joe");
    Extension ext2 = new Extension().setUrl("http://examples.com#givenext").setValue(new StringType("given"));
    given.getExtension().add(ext2);

    StringType given2 = name.addGivenElement();
    given2.setValue("Shmoe");
    Extension given2ext = new Extension().setUrl("http://examples.com#givenext_parent");
    given2.getExtension().add(given2ext);
    given2ext.addExtension().setUrl("http://examples.com#givenext_child").setValue(new StringType("CHILD"));

    String output = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
    ourLog.info(output);

    String enc = ourCtx.newJsonParser().encodeResourceToString(patient);
    assertThat(enc).containsSubsequence("{\"resourceType\":\"Patient\",", "\"extension\":[{\"url\":\"http://example.com/extensions#someext\",\"valueDateTime\":\"2011-01-02T11:13:15\"}",
            "{\"url\":\"http://example.com#parent\",\"extension\":[{\"url\":\"http://example.com#child\",\"valueString\":\"value1\"},{\"url\":\"http://example.com#child\",\"valueString\":\"value2\"}]}");
    assertThat(enc).containsSubsequence("\"modifierExtension\":[" + "{" + "\"url\":\"http://example.com/extensions#modext\"," + "\"valueDate\":\"1995-01-02\"" + "}" + "],");
		assertThat(enc).contains("\"_given\":[" + "{" + "\"extension\":[" + "{" + "\"url\":\"http://examples.com#givenext\"," + "\"valueString\":\"given\"" + "}" + "]" + "}," + "{"
			+ "\"extension\":[" + "{" + "\"url\":\"http://examples.com#givenext_parent\"," + "\"extension\":[" + "{"
			+ "\"url\":\"http://examples.com#givenext_child\"," + "\"valueString\":\"CHILD\"" + "}" + "]" + "}" + "]" + "}");

    /*
     * Now parse this back
     */

    Patient parsed = ourCtx.newJsonParser().parseResource(Patient.class, enc);
    ext = parsed.getExtension().get(0);
		assertEquals("http://example.com/extensions#someext", ext.getUrl());
		assertEquals("2011-01-02T11:13:15", ((DateTimeType) ext.getValue()).getValueAsString());

    parent = patient.getExtension().get(1);
		assertEquals("http://example.com#parent", parent.getUrl());
		assertNull(parent.getValue());
    child1 = parent.getExtension().get(0);
		assertEquals("http://example.com#child", child1.getUrl());
		assertEquals("value1", ((StringType) child1.getValue()).getValueAsString());
    child2 = parent.getExtension().get(1);
		assertEquals("http://example.com#child", child2.getUrl());
		assertEquals("value2", ((StringType) child2.getValue()).getValueAsString());

    modExt = parsed.getModifierExtension().get(0);
		assertEquals("http://example.com/extensions#modext", modExt.getUrl());
		assertEquals("1995-01-02", ((DateType) modExt.getValue()).getValueAsString());

    name = parsed.getName().get(0);

    ext2 = name.getGiven().get(0).getExtension().get(0);
		assertEquals("http://examples.com#givenext", ext2.getUrl());
		assertEquals("given", ((StringType) ext2.getValue()).getValueAsString());

    given2ext = name.getGiven().get(1).getExtension().get(0);
		assertEquals("http://examples.com#givenext_parent", given2ext.getUrl());
		assertNull(given2ext.getValue());
    Extension given2ext2 = given2ext.getExtension().get(0);
		assertEquals("http://examples.com#givenext_child", given2ext2.getUrl());
		assertEquals("CHILD", ((StringType) given2ext2.getValue()).getValue());

  }

  @Test
  public void testEncodeBinaryResource() {

    Binary patient = new Binary();
    patient.setContentType("foo");
    patient.setContent(new byte[] { 1, 2, 3, 4 });

    String val = ourCtx.newJsonParser().encodeResourceToString(patient);
		assertEquals("{\"resourceType\":\"Binary\",\"contentType\":\"foo\",\"content\":\"AQIDBA==\"}", val);

  }

  @Test
  public void testEncodeBundle() throws InterruptedException {
    Bundle b = new Bundle();

    InstantType pub = InstantType.now();
    b.getMeta().setLastUpdatedElement(pub);
    Thread.sleep(2);

    Patient p1 = new Patient();
    p1.addName().addFamily("Family1");
    p1.setId("1");
    BundleEntryComponent entry = b.addEntry();
    entry.setResource(p1);

    Patient p2 = new Patient();
    p2.setId("Patient/2");
    p2.addName().addFamily("Family2");
    entry = b.addEntry();
    entry.setResource(p2);

    BundleEntryComponent deletedEntry = b.addEntry();
    Patient dp = new Patient();
    deletedEntry.setResource(dp);

    dp.setId(("3"));
    InstantType nowDt = InstantType.withCurrentTime();
    dp.getMeta().setLastUpdatedElement(nowDt);

    String bundleString = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(b);
    ourLog.info(bundleString);

    // List<String> strings = new ArrayList<String>();
    // strings.addAll(Arrays.asList("\"published\":\"" + pub.getValueAsString() + "\""));
    // strings.addAll(Arrays.asList("\"id\":\"1\""));
    // strings.addAll(Arrays.asList("\"id\":\"2\"", "\"rel\":\"alternate\"", "\"href\":\"http://foo/bar\""));
    // strings.addAll(Arrays.asList("\"deleted\":\"" + nowDt.getValueAsString() + "\"", "\"id\":\"Patient/3\""));

    //@formatter:off
		String[] strings = new String[] {
			"\"resourceType\": \"Bundle\",",
			"\"lastUpdated\": \"" + pub.getValueAsString() + "\"",
			"\"entry\": [",
			"\"resource\": {",
			"\"id\": \"1\"",
			"\"resource\": {",
			"\"id\": \"2\"",
			"\"resource\": {",
			"\"id\": \"3\"",
			"\"meta\": {",
			"\"lastUpdated\": \"" + nowDt.getValueAsString() + "\"" 
		};
		//@formatter:off
		assertThat(bundleString).containsSubsequence(strings);

		b.getEntry().remove(2);
		bundleString = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(b);
		assertThat(bundleString).doesNotContain("deleted");

	}


	@Test
	public void testEncodeBundleCategory() {

		Bundle b = new Bundle();
		BundleEntryComponent e = b.addEntry();
		
		Patient pt = new Patient();
		pt.addIdentifier().setSystem("idsystem");
		e.setResource(pt);
		
		b.getMeta().addTag().setSystem("scheme").setCode("term").setDisplay("label");

		String val = ourCtx.newJsonParser().setPrettyPrint(false).encodeResourceToString(b);
		ourLog.info(val);

		assertThat(val).contains("\"tag\":[{\"system\":\"scheme\",\"code\":\"term\",\"display\":\"label\"}]");
		b = ourCtx.newJsonParser().parseResource(Bundle.class, val);
		assertThat(b.getMeta().getTag()).hasSize(1);
		assertEquals("scheme", b.getMeta().getTag().get(0).getSystem());
		assertEquals("term", b.getMeta().getTag().get(0).getCode());
		assertEquals("label", b.getMeta().getTag().get(0).getDisplay());

		assertNotNull(b.getEntry().get(0).getResource());
		Patient p = (Patient) b.getEntry().get(0).getResource();
		assertEquals("idsystem", p.getIdentifier().get(0).getSystem());

	}


	@Test
	public void testEncodeBundleEntryCategory() {

		Bundle b = new Bundle();
		BundleEntryComponent e = b.addEntry();
		e.setResource(new Patient());
		e.getResource().getMeta().addTag().setSystem("scheme").setCode( "term").setDisplay( "label");

		String val = ourCtx.newJsonParser().setPrettyPrint(false).encodeResourceToString(b);
		ourLog.info(val);

		assertThat(val).contains("{\"resourceType\":\"Bundle\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"meta\":{\"tag\":[{\"system\":\"scheme\",\"code\":\"term\",\"display\":\"label\"}]}}}]}");

		b = ourCtx.newJsonParser().parseResource(Bundle.class, val);
		assertThat(b.getEntry()).hasSize(1);
		assertThat(b.getEntry().get(0).getResource().getMeta().getTag()).hasSize(1);
		assertEquals("scheme", b.getEntry().get(0).getResource().getMeta().getTag().get(0).getSystem());
		assertEquals("term", b.getEntry().get(0).getResource().getMeta().getTag().get(0).getCode());
		assertEquals("label", b.getEntry().get(0).getResource().getMeta().getTag().get(0).getDisplay());

	}
	
	
	@Test
	public void testEncodeContained__() {
		// Create an organization
		Organization org = new Organization();
		org.getNameElement().setValue("Contained Test Organization");

		// Create a patient
		Patient patient = new Patient();
		patient.setId("Patient/1333");
		patient.addIdentifier().setSystem("urn:mrns").setValue( "253345");
		patient.getManagingOrganization().setResource(org);
		
		// Create a bundle with just the patient resource
		Bundle b = new Bundle();
		b.addEntry().setResource(patient);
				
		// Encode the buntdle
		String encoded = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(b);
		ourLog.info(encoded);
		String organizationUuid = UuidUtils.findFirstUUID(encoded);
		assertNotNull(organizationUuid);

		assertThat(encoded).containsSubsequence(Arrays.asList("\"contained\"", "resourceType\": \"Organization", "id\": \"" + organizationUuid + "\""));
		assertThat(encoded).contains("reference\": \"#" + organizationUuid + "\"");
		
		encoded = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
		ourLog.info(encoded);
		assertThat(encoded).containsSubsequence(Arrays.asList("\"contained\"", "resourceType\": \"Organization", "id\": \"" + organizationUuid + "\""));
		assertThat(encoded).contains("reference\": \"#" + organizationUuid + "\"");
	}

	@Test
	public void testEncodeContainedResourcesMore() throws Exception {

		DiagnosticReport rpt = new DiagnosticReport();
		Specimen spm = new Specimen();
		rpt.getText().setDivAsString("AAA");
		rpt.addSpecimen().setResource(spm);

		IParser p = ourCtx.newJsonParser().setPrettyPrint(true);
		String str = p.encodeResourceToString(rpt);

		ourLog.info(str);
		assertThat(str).contains("<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">AAA</div>");
		String substring = "\"reference\": \"#";
		assertThat(str).contains(substring);

		int idx = str.indexOf(substring) + substring.length();
		int idx2 = str.indexOf('"', idx + 1);
		String id = str.substring(idx, idx2);
		assertThat(str).contains("\"id\": \"" + id + "\"");
		assertThat(str).doesNotContain("<?xml version='1.0'?>");

	}

	@Test
	public void testEncodeContainedWithNarrative() throws Exception {
		IParser parser = ourCtx.newJsonParser().setPrettyPrint(true);

		// Create an organization, note that the organization does not have an ID
		Organization org = new Organization();
		org.getNameElement().setValue("Contained Test Organization");
		org.getText().setDivAsString("<div>FOOBAR</div>");

		// Create a patient
		Patient patient = new Patient();
		patient.setId("Patient/1333");
		patient.addIdentifier().setSystem("urn:mrns").setValue( "253345");
		patient.getText().setDivAsString("<div>BARFOO</div>");
		patient.getManagingOrganization().setResource(org);

		String encoded = parser.encodeResourceToString(patient);
		ourLog.info(encoded);
		assertThat(encoded).contains("FOOBAR");
		assertThat(encoded).contains("BARFOO");
		
	}

	

	@Test
	public void testEncodeDeclaredExtensionWithAddressContent() {
		IParser parser = ourCtx.newJsonParser();

		MyPatientWithOneDeclaredAddressExtension patient = new MyPatientWithOneDeclaredAddressExtension();
		patient.addAddress().setUse(AddressUse.HOME);
		patient.setFoo(new Address().addLine("line1"));

		String val = parser.encodeResourceToString(patient);
		ourLog.info(val);
		assertThat(val).contains("\"extension\":[{\"url\":\"urn:foo\",\"valueAddress\":{\"line\":[\"line1\"]}}]");

		MyPatientWithOneDeclaredAddressExtension actual = parser.parseResource(MyPatientWithOneDeclaredAddressExtension.class, val);
		assertEquals(AddressUse.HOME, patient.getAddress().get(0).getUse());
		Address ref = actual.getFoo();
		assertEquals("line1", ref.getLine().get(0).getValue());

	}

	@Test
	public void testEncodeDeclaredExtensionWithResourceContent() {
		IParser parser = ourCtx.newJsonParser();

		MyPatientWithOneDeclaredExtension patient = new MyPatientWithOneDeclaredExtension();
		patient.addAddress().setUse(AddressUse.HOME);
		patient.setFoo(new Reference("Organization/123"));

		String val = parser.encodeResourceToString(patient);
		ourLog.info(val);
		assertThat(val).contains("\"extension\":[{\"url\":\"urn:foo\",\"valueReference\":{\"reference\":\"Organization/123\"}}]");

		MyPatientWithOneDeclaredExtension actual = parser.parseResource(MyPatientWithOneDeclaredExtension.class, val);
		assertEquals(AddressUse.HOME, patient.getAddress().get(0).getUse());
		Reference ref = actual.getFoo();
		assertEquals("Organization/123", ref.getReference());

	}

	@Test
	public void testEncodeExt() throws Exception {

		ValueSet valueSet = new ValueSet();
		valueSet.setId("123456");

		ValueSetCodeSystemComponent define = valueSet.getCodeSystem();
		ConceptDefinitionComponent code = define.addConcept();
		code.setCode("someCode");
		code.setDisplay("someDisplay");
		code.addExtension().setUrl("urn:alt").setValue( new StringType("alt name"));


		String encoded = ourCtx.newJsonParser().encodeResourceToString(valueSet);
		ourLog.info(encoded);

		assertThat(encoded).contains("123456");
		assertEquals("{\"resourceType\":\"ValueSet\",\"id\":\"123456\",\"codeSystem\":{\"concept\":[{\"extension\":[{\"url\":\"urn:alt\",\"valueString\":\"alt name\"}],\"code\":\"someCode\",\"display\":\"someDisplay\"}]}}", encoded);

	}

	@Test
	public void testEncodeExtensionInCompositeElement() {

		Conformance c = new Conformance();
		c.addRest().getSecurity().addExtension().setUrl("http://foo").setValue(new StringType("AAA"));

		String encoded = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(c);
		ourLog.info(encoded);

		encoded = ourCtx.newJsonParser().setPrettyPrint(false).encodeResourceToString(c);
		ourLog.info(encoded);
		assertEquals(encoded, "{\"resourceType\":\"Conformance\",\"rest\":[{\"security\":{\"extension\":[{\"url\":\"http://foo\",\"valueString\":\"AAA\"}]}}]}");

	}



	@Test
	public void testEncodeExtensionInPrimitiveElement() {

		Conformance c = new Conformance();
		c.getAcceptUnknownElement().addExtension().setUrl( "http://foo").setValue( new StringType("AAA"));

		String encoded = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(c);
		ourLog.info(encoded);

		encoded = ourCtx.newJsonParser().setPrettyPrint(false).encodeResourceToString(c);
		ourLog.info(encoded);
		assertEquals(encoded, "{\"resourceType\":\"Conformance\",\"_acceptUnknown\":{\"extension\":[{\"url\":\"http://foo\",\"valueString\":\"AAA\"}]}}");

		// Now with a value
		ourLog.info("---------------");

		c = new Conformance();
		c.getAcceptUnknownElement().setValue(UnknownContentCode.EXTENSIONS);
		c.getAcceptUnknownElement().addExtension().setUrl("http://foo").setValue( new StringType("AAA"));

		encoded = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(c);
		ourLog.info(encoded);

		encoded = ourCtx.newJsonParser().setPrettyPrint(false).encodeResourceToString(c);
		ourLog.info(encoded);
		assertEquals(encoded, "{\"resourceType\":\"Conformance\",\"acceptUnknown\":\"extensions\",\"_acceptUnknown\":{\"extension\":[{\"url\":\"http://foo\",\"valueString\":\"AAA\"}]}}");

	}

	@Test
	public void testEncodeExtensionInResourceElement() {

		Conformance c = new Conformance();
		// c.addRest().getSecurity().addUndeclaredExtension(false, "http://foo", new StringType("AAA"));
		c.addExtension().setUrl("http://foo").setValue( new StringType("AAA"));

		String encoded = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(c);
		ourLog.info(encoded);

		encoded = ourCtx.newJsonParser().setPrettyPrint(false).encodeResourceToString(c);
		ourLog.info(encoded);
		assertEquals(encoded, "{\"resourceType\":\"Conformance\",\"extension\":[{\"url\":\"http://foo\",\"valueString\":\"AAA\"}]}");

	}

	@Test
	public void testEncodeExtensionOnEmptyElement() throws Exception {

		ValueSet valueSet = new ValueSet();
		valueSet.addUseContext().addExtension().setUrl("http://foo").setValue( new StringType("AAA"));

		String encoded = ourCtx.newJsonParser().encodeResourceToString(valueSet);
		assertThat(encoded).contains("\"useContext\":[{\"extension\":[{\"url\":\"http://foo\",\"valueString\":\"AAA\"}]}");

	}
	
	
	@Test
	public void testEncodeExtensionWithResourceContent() {
		IParser parser = ourCtx.newJsonParser();

		Patient patient = new Patient();
		patient.addAddress().setUse(AddressUse.HOME);
		patient.addExtension().setUrl("urn:foo").setValue( new Reference("Organization/123"));

		String val = parser.encodeResourceToString(patient);
		ourLog.info(val);
		assertThat(val).contains("\"extension\":[{\"url\":\"urn:foo\",\"valueReference\":{\"reference\":\"Organization/123\"}}]");

		Patient actual = parser.parseResource(Patient.class, val);
		assertEquals(AddressUse.HOME, patient.getAddress().get(0).getUse());
		List<Extension> ext = actual.getExtension();
		assertThat(ext).hasSize(1);
		Reference ref = (Reference) ext.get(0).getValue();
		assertEquals("Organization/123", ref.getReference());

	}

	@Test
	public void testEncodeIds() {
		Patient pt = new Patient();
		pt.addIdentifier().setSystem("sys").setValue( "val");
		
		List_ list = new List_();
		list.setId("listId");
		list.addEntry().setItem(new Reference(pt)).setDeleted(true);
		
		String enc = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(list);
		ourLog.info(enc);

		assertThat(enc).containsPattern("\"id\": \"" + UUID_PATTERN);
		
		List_ parsed = ourCtx.newJsonParser().parseResource(List_.class,enc);
		assertEquals(Patient.class, parsed.getEntry().get(0).getItem().getResource().getClass());
	}

	@Test
	public void testEncodeInvalidChildGoodException() {
		Observation obs = new Observation();
		obs.setValue(new DecimalType(112.22));

		IParser p = ourCtx.newJsonParser();

		try {
			p.encodeResourceToString(obs);
		} catch (DataFormatException e) {
			assertThat(e.getMessage()).contains("DecimalType");
		}
	}
	
	@Test
	public void testEncodeNarrativeBlockInBundle() throws Exception {
		Patient p = new Patient();
		p.addIdentifier().setSystem("foo").setValue("bar");
		p.getText().setStatus(NarrativeStatus.GENERATED);
		p.getText().setDivAsString("<div>AAA</div>");

		Bundle b = new Bundle();
		b.setTotal(123);
		b.addEntry().setResource(p);

		String str = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(b);
		ourLog.info(str);
		assertThat(str).contains("<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">AAA</div>");

		p.getText().setDivAsString("<xhtml:div xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">hello</xhtml:div>");
		str = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(b);
		ourLog.info(str);
		// Backslashes need to be escaped because they are in a JSON value
		assertThat(str).contains(">hello<");

	}	
	
	@Test
	public void testEncodeNonContained() {
		Organization org = new Organization();
		org.setId("Organization/65546");
		org.getNameElement().setValue("Contained Test Organization");

		Patient patient = new Patient();
		patient.setId("Patient/1333");
		patient.addIdentifier().setSystem("urn:mrns").setValue("253345");
		patient.getManagingOrganization().setResource(org);
		
		Bundle b = new Bundle();
		b.addEntry().setResource(patient);
		
		String encoded = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(b);
		ourLog.info(encoded);
		assertThat(encoded).doesNotContain("contained");
		assertThat(encoded).contains("\"reference\": \"Organization/65546\"");
		
		encoded = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
		ourLog.info(encoded);
		assertThat(encoded).doesNotContain("contained");
		assertThat(encoded).contains("\"reference\": \"Organization/65546\"");
	}
	
	

	
	@Test
	public void testEncodeResourceRef() throws DataFormatException {

		Patient patient = new Patient();
		patient.setManagingOrganization(new Reference());

		IParser p = ourCtx.newJsonParser();
		String str = p.encodeResourceToString(patient);
		assertThat(str).doesNotContain("managingOrganization");

		patient.setManagingOrganization(new Reference("Organization/123"));
		str = p.encodeResourceToString(patient);
		assertThat(str).contains("\"managingOrganization\":{\"reference\":\"Organization/123\"}");

		Organization org = new Organization();
		org.addIdentifier().setSystem("foo").setValue("bar");
		patient.setManagingOrganization(new Reference(org));
		str = p.encodeResourceToString(patient);
		assertThat(str).contains("\"contained\":[{\"resourceType\":\"Organization\"");

	}

	@Test
	public void testEncodeSummary() throws Exception {
		Patient patient = new Patient();
		patient.setId("Patient/1/_history/1");
		patient.getText().setDivAsString("<div>THE DIV</div>");
		patient.addName().addFamily("FAMILY");
		patient.setMaritalStatus(new CodeableConcept().setText("D"));

		String encoded = ourCtx.newJsonParser().setPrettyPrint(true).setSummaryMode(true).encodeResourceToString(patient);
		ourLog.info(encoded);

		assertThat(encoded).contains("Patient");
		assertThat(encoded).containsSubsequence("\"tag\"",
				"\"system\": \"" + Constants.TAG_SUBSETTED_SYSTEM_DSTU3 + "\",", "\"code\": \"" + Constants.TAG_SUBSETTED_CODE+"\",");
		assertThat(encoded).doesNotContain("THE DIV");
		assertThat(encoded).contains("family");
		assertThat(encoded).doesNotContain("maritalStatus");
	}

	@Test
	public void testEncodeSummary2() throws Exception {
		Patient patient = new Patient();
		patient.setId("Patient/1/_history/1");
		patient.getText().setDivAsString("<div>THE DIV</div>");
		patient.addName().addFamily("FAMILY");
		patient.setMaritalStatus(new CodeableConcept().setText("D"));

		patient.getMeta().addTag().setSystem("foo").setCode("bar");
		
		String encoded = ourCtx.newJsonParser().setPrettyPrint(true).setSummaryMode(true).encodeResourceToString(patient);
		ourLog.info(encoded);

		assertThat(encoded).contains("Patient");
		assertThat(encoded).containsSubsequence("\"tag\"",
				"\"system\": \"foo\",", "\"code\": \"bar\"",
				"\"system\": \"" + Constants.TAG_SUBSETTED_SYSTEM_DSTU3 + "\",", "\"code\": \"" + Constants.TAG_SUBSETTED_CODE+"\",");
		assertThat(encoded).doesNotContain("THE DIV");
		assertThat(encoded).contains("family");
		assertThat(encoded).doesNotContain("maritalStatus");
	}

	@Test
	public void testEncodeUndeclaredExtensionWithAddressContent() {
		IParser parser = ourCtx.newJsonParser();

		Patient patient = new Patient();
		patient.addAddress().setUse(AddressUse.HOME);
		patient.addExtension().setUrl("urn:foo").setValue(new Address().addLine("line1"));

		String val = parser.encodeResourceToString(patient);
		ourLog.info(val);
		assertThat(val).contains("\"extension\":[{\"url\":\"urn:foo\",\"valueAddress\":{\"line\":[\"line1\"]}}]");

		MyPatientWithOneDeclaredAddressExtension actual = parser.parseResource(MyPatientWithOneDeclaredAddressExtension.class, val);
		assertEquals(AddressUse.HOME, patient.getAddress().get(0).getUse());
		Address ref = actual.getFoo();
		assertEquals("line1", ref.getLine().get(0).getValue());

	}

	@Test
	public void testEncodingNullExtension() {
		Patient p = new Patient();
		Extension extension = new Extension().setUrl("http://foo#bar");
		p.getExtension().add(extension);
		String str = ourCtx.newJsonParser().encodeResourceToString(p);

		assertEquals("{\"resourceType\":\"Patient\"}", str);

		extension.setValue(new StringType());

		str = ourCtx.newJsonParser().encodeResourceToString(p);
		assertEquals("{\"resourceType\":\"Patient\"}", str);

		extension.setValue(new StringType(""));

		str = ourCtx.newJsonParser().encodeResourceToString(p);
		assertEquals("{\"resourceType\":\"Patient\"}", str);

	}

	@Test
	public void testExtensionOnComposite() throws Exception {

		Patient patient = new Patient();

		HumanName name = patient.addName();
		name.addFamily("Shmoe");
		HumanName given = name.addGiven("Joe");
		Extension ext2 = new Extension().setUrl("http://examples.com#givenext").setValue( new StringType("Hello"));
		given.getExtension().add(ext2);
		String enc = ourCtx.newJsonParser().encodeResourceToString(patient);
		ourLog.info(enc);
		assertEquals("{\"resourceType\":\"Patient\",\"name\":[{\"extension\":[{\"url\":\"http://examples.com#givenext\",\"valueString\":\"Hello\"}],\"family\":[\"Shmoe\"],\"given\":[\"Joe\"]}]}", enc);

		IParser newJsonParser = ourCtx.newJsonParser();
		StringReader reader = new StringReader(enc);
		Patient parsed = newJsonParser.parseResource(Patient.class, reader);

		ourLog.debug(ourCtx.newXmlParser().setPrettyPrint(true).encodeResourceToString(parsed));

		assertThat(parsed.getName().get(0).getExtension()).hasSize(1);
		Extension ext = parsed.getName().get(0).getExtension().get(0);
		assertEquals("Hello", ((IPrimitiveType<?>) ext.getValue()).getValue());

	}

	@Test
	public void testExtensionOnPrimitive() throws Exception {

		Patient patient = new Patient();

		HumanName name = patient.addName();
		StringType family = name.addFamilyElement();
		family.setValue("Shmoe");

		family.addExtension().setUrl("http://examples.com#givenext").setValue( new StringType("Hello"));
		String enc = ourCtx.newJsonParser().encodeResourceToString(patient);
		ourLog.info(enc);
		//@formatter:off
		assertThat(enc).contains(("{\n" +
			"    \"resourceType\":\"Patient\",\n" +
			"    \"name\":[\n" +
			"        {\n" +
			"            \"family\":[\n" +
			"                \"Shmoe\"\n" +
			"            ],\n" +
			"            \"_family\":[\n" +
			"                {\n" +
			"                    \"extension\":[\n" +
			"                        {\n" +
			"                            \"url\":\"http://examples.com#givenext\",\n" +
			"                            \"valueString\":\"Hello\"\n" +
			"                        }\n" +
			"                    ]\n" +
			"                }\n" +
			"            ]\n" +
			"        }\n" +
			"    ]\n" +
			"}").replace("\n", "").replaceAll(" +", ""));
		//@formatter:on

    Patient parsed = ourCtx.newJsonParser().parseResource(Patient.class, new StringReader(enc));
		assertThat(parsed.getName().get(0).getFamily().get(0).getExtension()).hasSize(1);
    Extension ext = parsed.getName().get(0).getFamily().get(0).getExtension().get(0);
		assertEquals("Hello", ((IPrimitiveType<?>) ext.getValue()).getValue());

  }

  @Test
  public void testMoreExtensions() throws Exception {

    Patient patient = new Patient();
    patient.addIdentifier().setUse(IdentifierUse.OFFICIAL).setSystem("urn:example").setValue("7000135");

    Extension ext = new Extension();
    ext.setUrl("http://example.com/extensions#someext");
    ext.setValue(new DateTimeType("2011-01-02T11:13:15"));

    // Add the extension to the resource
    patient.getExtension().add(ext);
    // END SNIPPET: resourceExtension

    // START SNIPPET: resourceStringExtension
    HumanName name = patient.addName();
    name.addFamily("Shmoe");
    StringType given = name.addGivenElement();
    given.setValue("Joe");
    Extension ext2 = new Extension().setUrl("http://examples.com#givenext").setValue(new StringType("given"));
    given.getExtension().add(ext2);

    StringType given2 = name.addGivenElement();
    given2.setValue("Shmoe");
    Extension given2ext = new Extension().setUrl("http://examples.com#givenext_parent");
    given2.getExtension().add(given2ext);
    given2ext.addExtension().setUrl("http://examples.com#givenext_child").setValue(new StringType("CHILD"));
    // END SNIPPET: resourceStringExtension

    // START SNIPPET: subExtension
    Extension parent = new Extension().setUrl("http://example.com#parent");
    patient.getExtension().add(parent);

    Extension child1 = new Extension().setUrl("http://example.com#child").setValue(new StringType("value1"));
    parent.getExtension().add(child1);

    Extension child2 = new Extension().setUrl("http://example.com#child").setValue(new StringType("value1"));
    parent.getExtension().add(child2);
    // END SNIPPET: subExtension

    String output = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
    ourLog.info(output);

    String enc = ourCtx.newJsonParser().encodeResourceToString(patient);
		//@formatter:off
		assertThat(enc).contains(("{" +
			"\"resourceType\":\"Patient\"," +
			"    \"extension\":[" +
			"        {" +
			"            \"url\":\"http://example.com/extensions#someext\"," +
			"            \"valueDateTime\":\"2011-01-02T11:13:15\"" +
			"        }," +
			"        {" +
			"            \"url\":\"http://example.com#parent\"," +
			"            \"extension\":[" +
			"                {" +
			"                    \"url\":\"http://example.com#child\"," +
			"                    \"valueString\":\"value1\"" +
			"                }," +
			"                {" +
			"                    \"url\":\"http://example.com#child\"," +
			"                    \"valueString\":\"value1\"" +
			"                }" +
			"            ]" +
			"        }" +
			"    ]").replace(" ", ""));
		//@formatter:on

		//@formatter:off
		assertThat(enc).contains((
			"            \"given\":[" +
				"                \"Joe\"," +
				"                \"Shmoe\"" +
				"            ]," +
				"            \"_given\":[" +
				"                {" +
				"                    \"extension\":[" +
				"                        {" +
				"                            \"url\":\"http://examples.com#givenext\"," +
				"                            \"valueString\":\"given\"" +
				"                        }" +
				"                    ]" +
				"                }," +
				"                {" +
				"                    \"extension\":[" +
				"                        {" +
				"                            \"url\":\"http://examples.com#givenext_parent\"," +
				"                            \"extension\":[" +
				"                                {" +
				"                                    \"url\":\"http://examples.com#givenext_child\"," +
				"                                    \"valueString\":\"CHILD\"" +
				"                                }" +
				"                            ]" +
				"                        }" +
				"                    ]" +
				"                }" +
				"").replace(" ", ""));
		//@formatter:on
  }

  @Test
  public void testNestedContainedResources() {

    Observation A = new Observation();
    A.getCode().setText("A");

    Observation B = new Observation();
    B.getCode().setText("B");
    A.addRelated().setTarget(new Reference(B));

    Observation C = new Observation();
    C.getCode().setText("C");
    B.addRelated().setTarget(new Reference(C));

    String str = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(A);
    ourLog.info(str);

    assertThat(str).containsSubsequence(Arrays.asList("\"text\": \"B\"", "\"text\": \"C\"", "\"text\": \"A\""));

    // Only one (outer) contained block
    int idx0 = str.indexOf("\"contained\"");
    int idx1 = str.indexOf("\"contained\"", idx0 + 1);

		assertThat(idx0).isNotEqualTo(-1);
		assertEquals(-1, idx1);

    Observation obs = ourCtx.newJsonParser().parseResource(Observation.class, str);
		assertEquals("A", obs.getCode().getTextElement().getValue());

    Observation obsB = (Observation) obs.getRelated().get(0).getTarget().getResource();
		assertEquals("B", obsB.getCode().getTextElement().getValue());

    Observation obsC = (Observation) obsB.getRelated().get(0).getTarget().getResource();
		assertEquals("C", obsC.getCode().getTextElement().getValue());

  }

  @Test
  public void testParseBinaryResource() {

    Binary val = ourCtx.newJsonParser().parseResource(Binary.class, "{\"resourceType\":\"Binary\",\"contentType\":\"foo\",\"content\":\"AQIDBA==\"}");
		assertEquals("foo", val.getContentType());
		assertThat(val.getContent()).containsExactly(new byte[]{1, 2, 3, 4});

  }

  @Test
  public void testParseEmptyNarrative() throws Exception {
    //@formatter:off
		String text = "{\n" + 
				"    \"resourceType\" : \"Patient\",\n" + 
				"    \"extension\" : [\n" + 
				"      {\n" + 
				"        \"url\" : \"http://clairol.org/colour\",\n" + 
				"        \"valueCode\" : \"B\"\n" + 
				"      }\n" + 
				"    ],\n" + 
				"    \"text\" : {\n" + 
				"      \"div\" : \"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\"\n" + 
				"    }" + 
				"}";
		//@formatter:on

    Patient res = (Patient) ourCtx.newJsonParser().parseResource(text);
    XhtmlNode div = res.getText().getDiv();
    String value = div.getValueAsString();

		assertNull(value);
    List<XhtmlNode> childNodes = div.getChildNodes();
		assertTrue(childNodes == null || childNodes.isEmpty());
  }

  @Test
  public void testParseSimpleBundle() {
    String bundle = "{\"resourceType\":\"Bundle\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"identifier\":[{\"system\":\"idsystem\"}]}}]}";
    Bundle b = ourCtx.newJsonParser().parseResource(Bundle.class, bundle);

		assertNotNull(b.getEntry().get(0).getResource());
    Patient p = (Patient) b.getEntry().get(0).getResource();
		assertEquals("idsystem", p.getIdentifier().get(0).getSystem());
  }

  /**
   * HAPI FHIR < 0.6 incorrectly used "resource" instead of "reference"
   */
  @Test
  @Disabled
  public void testParseWithIncorrectReference() throws IOException {
    String jsonString = IOUtils.toString(JsonParser.class.getResourceAsStream("/example-patient-general-hl7orgdstu2.json"));
    jsonString = jsonString.replace("\"reference\"", "\"resource\"");
    Patient parsed = ourCtx.newJsonParser().parseResource(Patient.class, jsonString);
		assertEquals("Organization/1", parsed.getManagingOrganization().getReference());
  }

  @Test
  public void testSimpleParse() throws DataFormatException, IOException {

    String msg = IOUtils.toString(XmlParser.class.getResourceAsStream("/example-patient-general-hl7orgdstu2.json"));
    IParser p = ourCtx.newJsonParser();
    // ourLog.info("Reading in message: {}", msg);
    Patient res = p.parseResource(Patient.class, msg);

		assertThat(res.getExtension()).hasSize(2);
		assertThat(res.getModifierExtension()).hasSize(1);

    String encoded = ourCtx.newXmlParser().setPrettyPrint(true).encodeResourceToString(res);
    ourLog.info(encoded);

  }

  @Test
  public void testSimpleResourceEncode() throws IOException {

    String xmlString = ClasspathUtil.loadResource("/example-patient-general-hl7orgdstu2.xml");
    Patient obs = ourCtx.newXmlParser().parseResource(Patient.class, xmlString);

    List<Extension> undeclaredExtensions = obs.getContact().get(0).getName().getFamily().get(0).getExtension();
    Extension undeclaredExtension = undeclaredExtensions.get(0);
		assertEquals("http://hl7.org/fhir/Profile/iso-21090#qualifier", undeclaredExtension.getUrl());

    ourLog.debug(ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(obs));

    IParser jsonParser = ourCtx.newJsonParser();
    String encoded = jsonParser.encodeResourceToString(obs);
    ourLog.info(encoded);

    String jsonString = ClasspathUtil.loadResource("/example-patient-general-hl7orgdstu2.json");

    JSON expected = JSONSerializer.toJSON(jsonString);
    JSON actual = JSONSerializer.toJSON(encoded.trim());

    String exp = expected.toString();

    // This shows up when we parse XML
    String act = actual.toString().replace(" xmlns=\\\"http://www.w3.org/1999/xhtml\\\"", "");

    ourLog.info("Expected: {}", exp);
    ourLog.info("Actual  : {}", act);
		assertEquals(exp, act);

  }

  @Test
  public void testParsePrimitiveExtension() {
    //@formatter:off
	  String input = "{\n" + 
	      "    \"resourceType\":\"Patient\",\n" + 
	      "    \"contact\":[\n" + 
	      "        {\n" + 
	      "            \"name\":{\n" + 
	      "                \"family\":[\n" + 
	      "                    \"du\",\n" + 
	      "                    \"Marché\"\n" + 
	      "                ],\n" + 
	      "                \"_family\":[\n" + 
	      "                    {\n" + 
	      "                        \"extension\":[\n" + 
	      "                            {\n" + 
	      "                                \"url\":\"http://hl7.org/fhir/Profile/iso-21090#qualifier\",\n" + 
	      "                                \"valueCode\":\"VV\"\n" + 
	      "                            }\n" + 
	      "                        ]\n" + 
	      "                    },\n" + 
	      "                    null\n" + 
	      "                ]\n" + 
	      "            }\n" + 
	      "        }\n" + 
	      "    ]\n" + 
	      "}";
    //@formatter:off
	  
	  Patient p = ourCtx.newJsonParser().parseResource(Patient.class, input);
	  ContactComponent contact = p.getContact().get(0);
	  StringType family = contact.getName().getFamily().get(0);
		assertEquals("du", family.getValueAsString());
		assertThat(family.getExtension()).hasSize(1);
	}
	
	
	@Test
	public void testSimpleResourceEncodeWithCustomType() throws IOException, SAXException {

		String jsonString = ClasspathUtil.loadResource("/example-patient-general-hl7orgdstu2.json");
		MyObservationWithExtensions obs = ourCtx.newJsonParser().parseResource(MyObservationWithExtensions.class, jsonString);

		{
    ContactComponent contact = obs.getContact().get(0);
    StringType family = contact.getName().getFamily().get(0);
			assertEquals("du", family.getValueAsString());
			assertThat(family.getExtension()).hasSize(1);
		}

		assertThat(obs.getExtension()).isEmpty();
		assertEquals("aaaa", obs.getExtAtt().getContentType());
		assertEquals("str1", obs.getMoreExt().getStr1().getValue());
		assertEquals("2011-01-02", obs.getModExt().getValueAsString());

		List<org.hl7.fhir.dstu2.model.Extension> undeclaredExtensions = obs.getContact().get(0).getName().getFamily().get(0).getExtension();
		org.hl7.fhir.dstu2.model.Extension undeclaredExtension = undeclaredExtensions.get(0);
		assertEquals("http://hl7.org/fhir/Profile/iso-21090#qualifier", undeclaredExtension.getUrl());

		IParser xmlParser = ourCtx.newXmlParser();
		String encoded = xmlParser.encodeResourceToString(obs);
		encoded = encoded.replaceAll("<!--.*-->", "").replace("\n", "").replace("\r", "").replaceAll(">\\s+<", "><");

		String xmlString = ClasspathUtil.loadResource("/example-patient-general-hl7orgdstu2.xml");
		xmlString = xmlString.replaceAll("<!--.*-->", "").replace("\n", "").replace("\r", "").replaceAll(">\\s+<", "><");

		ourLog.info("Expected: " + xmlString);
		ourLog.info("Actual  : " + encoded);

		String expected = (xmlString);
		String actual = (encoded.trim());

		XmlParserHl7OrgDstu2Test.compareXml(expected, actual);
	}


	@Test
	public void testBaseUrlFooResourceCorrectlySerializedInExtensionValueReference() {
		String refVal = "http://my.org/FooBar";

		Patient fhirPat = new Patient();
		fhirPat.addExtension().setUrl("x1").setValue(new Reference(refVal));

		IParser parser = ourCtx.newJsonParser();

		String output = parser.encodeResourceToString(fhirPat);
		ourLog.info("output: " + output);

		// Deserialize then check that valueReference value is still correct
		fhirPat = parser.parseResource(Patient.class, output);

		List<Extension> extlst = fhirPat.getExtension();
		assertThat(extlst).hasSize(1);
		assertEquals(refVal, ((Reference) extlst.get(0).getValue()).getReference());
	}

  @Test
  public void testParseQuestionnaireResponseAnswerWithValueReference() throws FHIRException {
    String response = "{\"resourceType\":\"QuestionnaireResponse\",\"group\":{\"question\":[{\"answer\": [{\"valueReference\": {\"reference\": \"Observation/testid\"}}]}]}}";
    QuestionnaireResponse r = ourCtx.newJsonParser().parseResource(QuestionnaireResponse.class, response);

    QuestionAnswerComponent answer = r.getGroup().getQuestion().get(0).getAnswer().get(0);
		assertNotNull(answer);
		assertNotNull(answer.getValueReference());
		assertEquals("Observation/testid", answer.getValueReference().getReference());
  }

  @ResourceDef(name = "Patient")
  public static class MyPatientWithOneDeclaredAddressExtension extends Patient {

    private static final long serialVersionUID = 1L;

    @Child(order = 0, name = "foo")
    @ca.uhn.fhir.model.api.annotation.Extension(url = "urn:foo", definedLocally = true, isModifier = false)
    private Address myFoo;

    public Address getFoo() {
      return myFoo;
    }

    public void setFoo(Address theFoo) {
      myFoo = theFoo;
    }

  }

  @ResourceDef(name = "Patient")
  public static class MyPatientWithOneDeclaredExtension extends Patient {

    private static final long serialVersionUID = 1L;

    @Child(order = 0, name = "foo")
    @ca.uhn.fhir.model.api.annotation.Extension(url = "urn:foo", definedLocally = true, isModifier = false)
    private Reference myFoo;

    public Reference getFoo() {
      return myFoo;
    }

    public void setFoo(Reference theFoo) {
      myFoo = theFoo;
    }

  }

}
