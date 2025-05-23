/*-
 * #%L
 * HAPI FHIR Storage api
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.dao.expunge;

import ca.uhn.fhir.jpa.dao.tx.HapiTransactionService;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.Validate;

import java.util.function.Supplier;

/**
 * Utility class wrapping a supplier in a transaction with the purpose of performing the supply operation with a
 * partitioned aware context.
 */
public class PartitionAwareSupplier {
	private final HapiTransactionService myTransactionService;
	private final RequestDetails myRequestDetails;

	public PartitionAwareSupplier(HapiTransactionService theTxService, RequestDetails theRequestDetails) {
		myTransactionService = theTxService;
		myRequestDetails = theRequestDetails;
	}

	@Nonnull
	public <T> T supplyInPartitionedContext(Supplier<T> theResourcePersistentIdSupplier) {
		T retVal =
				myTransactionService.withRequest(myRequestDetails).execute(tx -> theResourcePersistentIdSupplier.get());
		Validate.notNull(retVal, "No resource persistent id supplied by supplier %s", theResourcePersistentIdSupplier);
		return retVal;
	}
}
