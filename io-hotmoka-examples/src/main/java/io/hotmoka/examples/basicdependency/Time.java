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

package io.hotmoka.examples.basicdependency;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

@Exported
public abstract class Time extends Storage implements Comparable<Time> {
	protected final int secondsFromStartOfDay;

	protected Time(int secondsFromStartOfDay) {
		this.secondsFromStartOfDay = secondsFromStartOfDay;
	}

	public abstract Time after(int minutes);

	public boolean isBeforeOrEqualTo(Time other) {
		return compareTo(other) <= 0;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Time && ((Time) other).secondsFromStartOfDay == secondsFromStartOfDay;
	}

	@Override
	public int hashCode() {
		return secondsFromStartOfDay;
	}

	@Override
	public int compareTo(Time other) {
		return secondsFromStartOfDay - other.secondsFromStartOfDay;
	}

	protected final String twoDigits(int i) {
		if (i < 10)
			return "0" + i;
		else
			return String.valueOf(i);
	}

	@Override @View
	public abstract String toString();
}