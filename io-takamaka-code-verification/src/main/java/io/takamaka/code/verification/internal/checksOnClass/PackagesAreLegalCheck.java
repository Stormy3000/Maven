/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.takamaka.code.verification.internal.checksOnClass;

import io.takamaka.code.verification.internal.CheckOnClasses;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalPackageNameError;

/**
 * A check that class packages in Takamaka code are allowed.
 */
public class PackagesAreLegalCheck extends CheckOnClasses {

	public PackagesAreLegalCheck(VerifiedClassImpl.Verification builder) {
		super(builder);

		if (className.startsWith("java.") || className.startsWith("javax."))
			issue(new IllegalPackageNameError(inferSourceFile()));

		// io.takamaka.code.* is allowed during node initialization, in order to
		// allow the installation of the run-time Takamaka classes such as Contract
		if (!duringInitialization && className.startsWith("io.takamaka.code."))
			issue(new IllegalPackageNameError(inferSourceFile()));
	}
}