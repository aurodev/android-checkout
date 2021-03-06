/*
 * Copyright 2014 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.checkout;

import android.app.Activity;
import android.content.Intent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static android.app.Activity.RESULT_OK;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.solovyev.android.checkout.ResponseCodes.ACCOUNT_ERROR;
import static org.solovyev.android.checkout.ResponseCodes.OK;

@RunWith(CheckoutTestRunner.class)
public class PurchaseFlowTest {

	@Nonnull
	private RequestListener listener;

	@Nonnull
	private PurchaseFlow flow;

	@Nonnull
	private PurchaseVerifier verifier;

	@Before
	public void setUp() throws Exception {
		listener = mock(RequestListener.class);
		verifier = mock(PurchaseVerifier.class);
		Tests.mockVerifier(verifier, false);
		flow = new PurchaseFlow(new Activity(), 1, listener, verifier);
	}

	@Test
	public void testShouldErrorIfIntentIsNull() throws Exception {
		flow.onActivityResult(1, 1, null);

		verifyError(ResponseCodes.NULL_INTENT, BillingException.class);
	}

	private void verifyError(int responseCode, Class<? extends Exception> exceptionClass) {
		verify(listener).onError(eq(responseCode), any(exceptionClass));
		verify(listener, never()).onSuccess(anyObject());
	}

	@Test
	public void testShouldErrorIfRequestCodeIsDifferent() throws Exception {
		flow.onActivityResult(2, 1, new Intent());

		verifyError(ResponseCodes.EXCEPTION, RuntimeException.class);
	}

	@Test
	public void testShouldErrorIfResultCodeItNotOk() throws Exception {
		flow.onActivityResult(1, Activity.RESULT_CANCELED, new Intent());

		verifyError(OK, BillingException.class);
	}

	@Test
	public void testShouldErrorIfResponseCodeItNotOk() throws Exception {
		flow.onActivityResult(1, RESULT_OK, newIntent(ResponseCodes.ACCOUNT_ERROR, null, null));

		verifyError(ResponseCodes.ACCOUNT_ERROR, BillingException.class);
	}

	@Test
	public void testShouldErrorWithNoData() throws Exception {
		flow.onActivityResult(1, RESULT_OK, newIntent(OK, null, "signature"));

		verifyError(ResponseCodes.EXCEPTION, RuntimeException.class);
	}

	@Test
	public void testShouldErrorWithNoSignature() throws Exception {
		flow.onActivityResult(1, RESULT_OK, newIntent(OK, "data", null));

		verifyError(ResponseCodes.EXCEPTION, RuntimeException.class);
	}

	@Test
	public void testShouldErrorWithEmptySignature() throws Exception {
		flow.onActivityResult(1, RESULT_OK, newIntent(OK, "{productId:'test', purchaseTime:1000}", ""));

		verifyError(ResponseCodes.WRONG_SIGNATURE, RuntimeException.class);
	}

	@Test
	public void testShouldErrorIfVerificationFailed() throws Exception {
		Tests.mockVerifier(verifier, false);
		flow.onActivityResult(1, RESULT_OK, newIntent(OK, "{productId:'test', purchaseTime:1000}", "signature"));

		verifyError(ResponseCodes.WRONG_SIGNATURE, RuntimeException.class);

	}

	@Test
	public void testShouldFinishSuccessfully() throws Exception {
		Tests.mockVerifier(verifier, true);
		flow.onActivityResult(1, RESULT_OK, newOkIntent());

		verify(listener, never()).onError(anyInt(), any(Exception.class));
		verify(listener).onSuccess(any(Purchase.class));
	}

	@Test
	public void testShouldNotCallListenerIfCancelled() throws Exception {
		Tests.mockVerifier(verifier, true);
		flow.cancel();
		flow.onActivityResult(1, RESULT_OK, newOkIntent());
		flow.onActivityResult(1, RESULT_OK, newIntent(ACCOUNT_ERROR, "{productId:'test', purchaseTime:1000}", "signature"));
		verify(listener, never()).onError(anyInt(), any(Exception.class));
		verify(listener, never()).onSuccess(any(Purchase.class));
	}

	@Nonnull
	static Intent newOkIntent() {
		return newIntent(OK, "{productId:'test', purchaseTime:1000}", "signature");
	}

	@Nonnull
	static Intent newIntent(int responseCode, @Nullable String data, @Nullable String signature) {
		final Intent intent = new Intent();
		intent.putExtra(PurchaseFlow.EXTRA_RESPONSE, responseCode);
		intent.putExtra(PurchaseFlow.EXTRA_PURCHASE_DATA, data);
		intent.putExtra(PurchaseFlow.EXTRA_PURCHASE_SIGNATURE, signature);
		return intent;
	}
}