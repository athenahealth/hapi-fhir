package ca.uhn.fhir.jpa.subscription;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.dao.data.IResourceModifiedDao;
import ca.uhn.fhir.jpa.provider.BaseResourceProviderR4Test;
import ca.uhn.fhir.jpa.subscription.channel.impl.LinkedBlockingChannel;
import ca.uhn.fhir.jpa.subscription.model.ResourceModifiedMessage;
import ca.uhn.fhir.jpa.subscription.submit.svc.ResourceModifiedSubmitterSvc;
import ca.uhn.fhir.jpa.test.util.SubscriptionTestUtil;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.subscription.api.IResourceModifiedMessagePersistenceSvc;
import ca.uhn.fhir.test.utilities.server.HashMapResourceProviderExtension;
import ca.uhn.fhir.test.utilities.server.RestfulServerExtension;
import ca.uhn.fhir.test.utilities.server.TransactionCapturingProviderExtension;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.test.concurrency.PointcutLatch;
import com.apicatalog.jsonld.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.listener.SingleQueryCountHolder;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Subscription;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class BaseSubscriptionsR4Test extends BaseResourceProviderR4Test {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(BaseSubscriptionsR4Test.class);
	protected static int ourListenerPort;

	@Order(0)
	@RegisterExtension
	protected static RestfulServerExtension ourRestfulServer = new RestfulServerExtension(FhirContext.forR4Cached());
	@Order(1)
	@RegisterExtension
	protected static HashMapResourceProviderExtension<Patient> ourPatientProvider = new HashMapResourceProviderExtension<>(ourRestfulServer, Patient.class);
	@Order(1)
	@RegisterExtension
	protected static HashMapResourceProviderExtension<Observation> ourObservationProvider = new HashMapResourceProviderExtension<>(ourRestfulServer, Observation.class);
	@Order(1)
	@RegisterExtension
	protected static TransactionCapturingProviderExtension<Bundle> ourTransactionProvider = new TransactionCapturingProviderExtension<>(ourRestfulServer, Bundle.class);
	protected static SingleQueryCountHolder ourCountHolder;
	@Order(1)
	@RegisterExtension
	protected static HashMapResourceProviderExtension<Organization> ourOrganizationProvider = new HashMapResourceProviderExtension<>(ourRestfulServer, Organization.class);
	@Autowired
	protected SubscriptionTestUtil mySubscriptionTestUtil;
	@Autowired
	protected ResourceModifiedSubmitterSvc myResourceModifiedSubmitterSvc;
	@Autowired
	protected IResourceModifiedMessagePersistenceSvc myResourceModifiedMessagePersistenceSvc;
	@Autowired
	protected IResourceModifiedDao myResourceModifiedDao;
	protected CountingInterceptor myCountingInterceptor;
	protected List<IIdType> mySubscriptionIds = Collections.synchronizedList(new ArrayList<>());
	@Autowired
	private SingleQueryCountHolder myCountHolder;

	protected final PointcutLatch mySubscriptionTopicsCheckedLatch = new PointcutLatch(Pointcut.SUBSCRIPTION_TOPIC_AFTER_PERSISTED_RESOURCE_CHECKED);
	protected final PointcutLatch mySubscriptionDeliveredLatch = new PointcutLatch(Pointcut.SUBSCRIPTION_AFTER_REST_HOOK_DELIVERY);

	@AfterEach
	public void afterUnregisterRestHookListener() {
		for (IIdType next : mySubscriptionIds) {
			IIdType nextId = next.toUnqualifiedVersionless();
			ourLog.info("Deleting: {}", nextId);
			myClient.delete().resourceById(nextId).execute();
		}
		mySubscriptionIds.clear();

		myStorageSettings.setAllowMultipleDelete(true);
		ourLog.info("Deleting all subscriptions");
		myClient.delete().resourceConditionalByUrl("Subscription?status=active").execute();
		myClient.delete().resourceConditionalByUrl("Observation?code:missing=false").execute();
		ourLog.info("Done deleting all subscriptions");
		myStorageSettings.setAllowMultipleDelete(new JpaStorageSettings().isAllowMultipleDelete());

		mySubscriptionTestUtil.unregisterSubscriptionInterceptor();
		myResourceModifiedDao.deleteAll();
	}

	@BeforeEach
	public void beforeRegisterRestHookListener() {
		mySubscriptionTestUtil.registerRestHookInterceptor();
	}

	@BeforeEach
	public void beforeReset() throws Exception {
		// Delete all Subscriptions
		if (myClient != null) {
			Bundle allSubscriptions = myClient.search().forResource(Subscription.class).returnBundle(Bundle.class).execute();
			for (IBaseResource next : BundleUtil.toListOfResources(myFhirContext, allSubscriptions)) {
				myClient.delete().resource(next).execute();
			}
			waitForActivatedSubscriptionCount(0);
		}

		LinkedBlockingChannel matchingChannel = mySubscriptionTestUtil.getMatchingChannel();
		if (matchingChannel != null) {
			matchingChannel.clearInterceptorsForUnitTest();
		}
		myCountingInterceptor = new CountingInterceptor();
		if (matchingChannel != null) {
			matchingChannel.addInterceptor(myCountingInterceptor);
		}
	}


	protected Subscription createSubscription(String theCriteria, String thePayload) {
		return createSubscription(theCriteria, thePayload, null);
	}

	protected Subscription createSubscription(String theCriteria, String thePayload, Extension theExtension) {
		String id = null;

		return createSubscription(theCriteria, thePayload, theExtension, id);
	}

	@Nonnull
	protected Subscription createSubscription(String theCriteria, String thePayload, Extension theExtension, String id) {
		Subscription subscription = newSubscription(theCriteria, thePayload);
		if (theExtension != null) {
			subscription.getChannel().addExtension(theExtension);
		}
		if (id != null) {
			subscription.setId(id);
		}

		subscription = postOrPutSubscription(subscription);
		return subscription;
	}

	protected Subscription postOrPutSubscription(IBaseResource theSubscription) {
		MethodOutcome methodOutcome;
		if (theSubscription.getIdElement().isEmpty()) {
			 methodOutcome = myClient.create().resource(theSubscription).execute();
		} else {
			 methodOutcome =  myClient.update().resource(theSubscription).execute();
		}
		theSubscription.setId(methodOutcome.getId().getIdPart());
		mySubscriptionIds.add(methodOutcome.getId());
		return (Subscription) theSubscription;
	}

	protected Subscription newSubscription(String theCriteria, String thePayload) {
		Subscription subscription = new Subscription();
		subscription.setReason("Monitor new neonatal function (note, age will be determined by the monitor)");
		subscription.setStatus(Subscription.SubscriptionStatus.REQUESTED);
		subscription.setCriteria(theCriteria);

		Subscription.SubscriptionChannelComponent channel = subscription.getChannel();
		channel.setType(Subscription.SubscriptionChannelType.RESTHOOK);
		channel.setPayload(thePayload);
		channel.setEndpoint(ourRestfulServer.getBaseUrl());
		return subscription;
	}


	protected void waitForQueueToDrain() throws InterruptedException {
		mySubscriptionTestUtil.waitForQueueToDrain();
	}

	@PostConstruct
	public void initializeOurCountHolder() {
		ourCountHolder = myCountHolder;
	}


	protected Observation sendObservation(String theCode, String theSystem) {
		return sendObservation(theCode, theSystem, null, null);
	}

	protected Observation sendObservation(String theCode, String theSystem, String theSource, String theRequestId) {
		Observation observation = buildBaseObservation(theCode, theSystem);
		if (StringUtils.isNotBlank(theSource)) {
			observation.getMeta().setSource(theSource);
		}

		SystemRequestDetails systemRequestDetails = new SystemRequestDetails();
		if (StringUtils.isNotBlank(theRequestId)) {
			systemRequestDetails.setRequestId(theRequestId);
		}
		IIdType id = myObservationDao.create(observation, systemRequestDetails).getId();
		observation.setId(id);
		return observation;
	}

	protected Observation buildBaseObservation(String theCode, String theSystem) {
		Observation observation = new Observation();
		CodeableConcept codeableConcept = new CodeableConcept();
		observation.setCode(codeableConcept);
		observation.getIdentifierFirstRep().setSystem("foo").setValue("1");
		Coding coding = codeableConcept.addCoding();
		coding.setCode(theCode);
		coding.setSystem(theSystem);

		observation.setStatus(Observation.ObservationStatus.FINAL);
		return observation;
	}

	protected Patient sendPatient() {
		Patient patient = new Patient();
		patient.setActive(true);

		IIdType id = myPatientDao.create(patient).getId();
		patient.setId(id);

		return patient;
	}

	protected Organization sendOrganization() {
		Organization org = new Organization();
		org.setName("ORG");

		IIdType id = myOrganizationDao.create(org).getId();
		org.setId(id);

		return org;
	}

	@AfterAll
	public static void reportTotalSelects() {
		ourLog.info("Total database select queries: {}", getQueryCount().getSelect());
	}

	private static QueryCount getQueryCount() {
		return ourCountHolder.getQueryCountMap().get("");
	}

	protected IIdType createResource(IBaseResource theResource, boolean theExpectDelivery) throws InterruptedException {
		return createResource(theResource, theExpectDelivery, 1);
	}

	// TODO KHS consolidate with lambda
	protected IIdType createResource(IBaseResource theResource, boolean theExpectDelivery, int theCount) throws InterruptedException {
		IFhirResourceDao dao = myDaoRegistry.getResourceDao(theResource.getClass());
		if (theExpectDelivery) {
			mySubscriptionDeliveredLatch.setExpectedCount(theCount);
		}
		mySubscriptionTopicsCheckedLatch.setExpectedCount(1);
		IIdType id = dao.create(theResource, mySrd).getId();
		mySubscriptionTopicsCheckedLatch.awaitExpected();
		if (theExpectDelivery) {
			mySubscriptionDeliveredLatch.awaitExpected();
		}
		theResource.setId(id);
		return id;
	}

	protected DaoMethodOutcome updateResource(IBaseResource theResource, boolean theExpectDelivery) throws InterruptedException {
		IFhirResourceDao dao = myDaoRegistry.getResourceDao(theResource.getClass());
		if (theExpectDelivery) {
			mySubscriptionDeliveredLatch.setExpectedCount(1);
		}
		mySubscriptionTopicsCheckedLatch.setExpectedCount(1);
		DaoMethodOutcome retval = dao.update(theResource, mySrd);

		List<HookParams> hookParams = mySubscriptionTopicsCheckedLatch.awaitExpected();
		ResourceModifiedMessage lastMessage = PointcutLatch.getInvocationParameterOfType(hookParams, ResourceModifiedMessage.class);
		assertEquals(theResource.getIdElement().toVersionless().toString(), lastMessage.getPayloadId());

		if (theExpectDelivery) {
			mySubscriptionDeliveredLatch.awaitExpected();
		}
		return retval;
	}


}
