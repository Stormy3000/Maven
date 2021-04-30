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

package io.takamaka.code.whitelisting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * States that an argument of a method or constructor of a white-listed
 * method must be false, for the method to be white-listed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ ElementType.PARAMETER })
@Inherited
@Documented
@WhiteListingProofObligation(check = MustBeFalse.Check.class)
public @interface MustBeFalse {
	
	class Check implements WhiteListingPredicate {

		@Override
		public boolean test(Object value, WhiteListingWizard wizard) {
			return value.equals(Boolean.FALSE);
		}

		@Override
		public String messageIfFailed(String methodName) {
			return "the actual parameter of " + methodName + " must be false";
		}
	}
}