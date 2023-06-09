/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

package io.hotmoka.network.updates;

import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfBigInteger;
import io.hotmoka.beans.updates.UpdateOfBoolean;
import io.hotmoka.beans.updates.UpdateOfByte;
import io.hotmoka.beans.updates.UpdateOfChar;
import io.hotmoka.beans.updates.UpdateOfDouble;
import io.hotmoka.beans.updates.UpdateOfEnumEager;
import io.hotmoka.beans.updates.UpdateOfEnumLazy;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.updates.UpdateOfFloat;
import io.hotmoka.beans.updates.UpdateOfInt;
import io.hotmoka.beans.updates.UpdateOfLong;
import io.hotmoka.beans.updates.UpdateOfShort;
import io.hotmoka.beans.updates.UpdateOfStorage;
import io.hotmoka.beans.updates.UpdateOfString;
import io.hotmoka.beans.updates.UpdateToNullEager;
import io.hotmoka.beans.updates.UpdateToNullLazy;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.CharValue;
import io.hotmoka.beans.values.DoubleValue;
import io.hotmoka.beans.values.EnumValue;
import io.hotmoka.beans.values.FloatValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.ShortValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.network.signatures.FieldSignatureModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.StorageValueModel;
import io.hotmoka.network.values.TransactionReferenceModel;

/**
 * The model of an update of an object.
 */
public class UpdateModel {

	/**
	 * The field that is updated. This is {@code null} for class tags.
	 */
	public FieldSignatureModel field;

	/**
	 * The value assigned to the updated field. This is {@code null} for class tags.
	 */
	public StorageValueModel value;

	/**
	 * The name of the class of the object. This is non-{@code null} for class tags only.
	 */
	public String className;

	/**
	 * The transaction that installed the jar from where the class has been loaded.
	 * This is non-{@code null} for class tags only.
	 */
	public TransactionReferenceModel jar;

	/**
	 * The object whose field is modified.
	 */
	public StorageReferenceModel object;

	/**
	 * Builds the model of an update of an object.
	 * 
	 * @param update the update
	 */
	public UpdateModel(Update update) {
		this.object = new StorageReferenceModel(update.object);

		if (update instanceof ClassTag) {
			ClassTag classTag = (ClassTag) update;

			this.field = null;
			this.value = null;
			this.className = classTag.clazz.name;
			this.jar = new TransactionReferenceModel(classTag.jar);
		}
		else {
			UpdateOfField updateOfField = (UpdateOfField) update;

			this.field = new FieldSignatureModel(updateOfField.getField());
			this.value = new StorageValueModel(updateOfField.getValue());
			this.className = null;
			this.jar = null;
		}
	}

	public UpdateModel() {}

	/**
	 * Yields the update having this model.
	 * 
	 * @return the update
	 */
	public Update toBean() {
		if (object == null)
			throw new RuntimeException("unexpected null update object");
		else if (className != null)
			return new ClassTag(object.toBean(), className, jar.toBean());
		else {
			FieldSignature field = this.field.toBean();
			StorageValue value = this.value.toBean();
			StorageReference object = this.object.toBean();

			if (value == NullValue.INSTANCE)
				if (field.type.isEager())
					return new UpdateToNullEager(object, field);
				else
					return new UpdateToNullLazy(object, field);
			else if (value instanceof EnumValue)
				if (field.type.isEager())
					return new UpdateOfEnumEager(object, field, ((EnumValue) value).enumClassName, ((EnumValue) value).name);
				else
					return new UpdateOfEnumLazy(object, field, ((EnumValue) value).enumClassName, ((EnumValue) value).name);
			else if (value instanceof BigIntegerValue)
				return new UpdateOfBigInteger(object, field, ((BigIntegerValue) value).value);
			else if (value instanceof StringValue)
				return new UpdateOfString(object, field, ((StringValue) value).value);
			else if (value instanceof StorageReference)
				return new UpdateOfStorage(object, field, ((StorageReference) value));
			else if (value instanceof BooleanValue)
				return new UpdateOfBoolean(object, field, ((BooleanValue) value).value);
			else if (value instanceof ByteValue)
				return new UpdateOfByte(object, field, ((ByteValue) value).value);
			else if (value instanceof CharValue)
				return new UpdateOfChar(object, field, ((CharValue) value).value);
			else if (value instanceof DoubleValue)
				return new UpdateOfDouble(object, field, ((DoubleValue) value).value);
			else if (value instanceof FloatValue)
				return new UpdateOfFloat(object, field, ((FloatValue) value).value);
			else if (value instanceof IntValue)
				return new UpdateOfInt(object, field, ((IntValue) value).value);
			else if (value instanceof LongValue)
				return new UpdateOfLong(object, field, ((LongValue) value).value);
			else if (value instanceof ShortValue)
				return new UpdateOfShort(object, field, ((ShortValue) value).value);
			else
				throw new RuntimeException("unexpected update value of class " + value.getClass().getName());
		}
	}
}