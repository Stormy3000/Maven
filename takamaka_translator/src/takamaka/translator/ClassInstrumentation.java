package takamaka.translator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import takamaka.blockchain.StorageReference;
import takamaka.lang.Storage;

class ClassInstrumentation {
	private final static Logger LOGGER = Logger.getLogger(ClassInstrumentation.class.getName());
	private final static String OLD_PREFIX = "§old_";
	private final static String IF_ALREADY_LOADED_PREFIX = "§ifAlreadyLoaded_";
	private final static String ENSURE_LOADED_PREFIX = "§ensureLoaded_";
	private final static String GETTER_PREFIX = "§get_";
	private final static String PUTTER_PREFIX = "§put_";
	private final static String EXTRACT_UPDATES = "extractUpdates";
	private final static String RECURSIVE_EXTRACT = "recursiveExtract";
	private final static String ADD_UPDATES_FOR = "addUpdatesFor";
	private final static String IN_STORAGE_NAME = "inStorage";
	private final static String DESERIALIZE_LAST_UPDATE_FOR = "deserializeLastUpdateFor";

	/**
	 * The order used for generating the parameters of the instrumented constructors.
	 */
	private final static Comparator<Field> fieldOrder = Comparator.comparing(Field::getName).thenComparing(field -> field.getType().toString());

	private final static Type[] NO_TYPES = new Type[0];
	private final static String[] NO_STRINGS = new String[0];
	private final static Type[] THREE_STRINGS = new Type[] { ObjectType.STRING, ObjectType.STRING, ObjectType.STRING };
	private final static Type[] ONLY_SET = new Type[] { new ObjectType("java.util.Set") };
	private final static ObjectType STORAGE_REFERENCE_OT = new ObjectType( StorageReference.class.getName());
	private final static Type[] ADD_UPDATES_FOR_ARGS = new Type[] { STORAGE_REFERENCE_OT, ObjectType.STRING, ObjectType.STRING, new ObjectType("java.util.Set") };
	private final static Type[] RECURSIVE_EXTRACT_ARGS = new Type[] { ObjectType.OBJECT, new ObjectType("java.util.Set") };

	public ClassInstrumentation(InputStream input, String className, JarOutputStream instrumentedJar, Program program) throws ClassFormatException, IOException {
		LOGGER.fine(() -> "Instrumenting " + className);
		ClassGen classGen = new ClassGen(new ClassParser(input, className).parse());
		new Initializer(classGen, program);
		classGen.getJavaClass().dump(instrumentedJar);
	}

	private class Initializer {
		/**
		 * The class that is being instrumented.
		 */
		private final ClassGen classGen;

		/**
		 * The name of <code>classGen</code>.
		 */
		private final String className;

		/**
		 * The constant pool of the class being instrumented.
		 */
		private final ConstantPoolGen cpg;

		/**
		 * The object that can be used to build complex instructions.
		 */
		private final InstructionFactory factory;

		/**
		 * True if and only if <code>classGen</code> is a storage class.
		 */
		private final boolean isStorage;

		/**
		 * The non-transient instance fields of primitive type defined in <code>classGen</code>
		 * and in its superclasses up to Storage (excluded). This is non-empty for storage classes only.
		 */
		private final LinkedList<SortedSet<Field>> primitiveNonTransientInstanceFields = new LinkedList<>();

		/**
		 * The non-transient instance fields of reference type defined in <code>classGen</code>
		 * (superclasses are not considered). This is non-empty for storage classes only.
		 */
		private final SortedSet<Field> referenceNonTransientInstanceFields = new TreeSet<>(fieldOrder);

		/**
		 * The program that collects the classes under instrumentation and those of the
		 * supporting libraries.
		 */
		private final Program program;

		private Initializer(ClassGen classGen, Program program) {
			this.classGen = classGen;
			this.className = classGen.getClassName();
			this.cpg = classGen.getConstantPool();
			this.factory = new InstructionFactory(cpg);
			this.program = program;
			this.isStorage = isStorage(className);
			if (isStorage)
				collectPrimitiveNonTransientInstanceFieldsOf(className);

			instrument();
		}

		private void instrument() {
			if (isStorage) {
				addOldAndIfAlreadyLoadedFields();
				addConstructorForDeserializationFromBlockchain();
				addEnsureLoadedMethods();
				addAccessorMethods();
				addExtractUpdates();
			}
			//TODO
		}

		private void addExtractUpdates() {
			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.ALOAD_1);
			il.append(factory.createInvoke(classGen.getSuperclassName(), EXTRACT_UPDATES, STORAGE_REFERENCE_OT, ONLY_SET, Const.INVOKESPECIAL));
			il.append(InstructionConst.ASTORE_2);

			InstructionHandle end = il.append(InstructionConst.ALOAD_2);
			il.append(InstructionFactory.createReturn(STORAGE_REFERENCE_OT));

			for (Field field: primitiveNonTransientInstanceFields.getLast())
				end = addUpdateExtractionForPrimitiveField(field, il, end);

			for (Field field: referenceNonTransientInstanceFields)
				end = addUpdateExtractionForReferenceField(field, il, end);

			MethodGen extractUpdates = new MethodGen(Const.ACC_PROTECTED | Const.ACC_SYNTHETIC, STORAGE_REFERENCE_OT, ONLY_SET, new String[] { "updates" }, EXTRACT_UPDATES, className, il, cpg);
			classGen.addMethod(extractUpdates.getMethod());
		}

		private InstructionHandle addUpdateExtractionForReferenceField(Field field, InstructionList il, InstructionHandle end) {
			ObjectType type = (ObjectType) field.getType();

			List<Type> args = new ArrayList<>();
			for (Type arg: ADD_UPDATES_FOR_ARGS)
				args.add(arg);
			args.add(ObjectType.STRING);
			args.add(ObjectType.OBJECT);

			InstructionHandle recursiveExtract = il.insert(end, InstructionFactory.createThis());
			il.insert(end, InstructionConst.DUP);
			il.insert(end, factory.createGetField(className, OLD_PREFIX + field.getName(), type));
			il.insert(end, InstructionConst.ALOAD_1);
			il.insert(end, factory.createInvoke(Storage.class.getName(), RECURSIVE_EXTRACT, Type.VOID, RECURSIVE_EXTRACT_ARGS, Const.INVOKESPECIAL));
			
			InstructionHandle addUpdatesFor = il.insert(recursiveExtract, InstructionConst.ALOAD_2);
			il.insert(recursiveExtract, factory.createConstant(className));
			il.insert(recursiveExtract, factory.createConstant(field.getName()));
			il.insert(recursiveExtract, InstructionConst.ALOAD_1);
			il.insert(recursiveExtract, factory.createConstant(type.getClassName()));
			il.insert(recursiveExtract, InstructionFactory.createThis());
			il.insert(recursiveExtract, factory.createGetField(className, field.getName(), type));
			il.insert(recursiveExtract, factory.createInvoke(Storage.class.getName(), ADD_UPDATES_FOR, Type.VOID, args.toArray(NO_TYPES), Const.INVOKESPECIAL));

			InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(Storage.class.getName(), IN_STORAGE_NAME, BasicType.BOOLEAN));
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, InstructionConst.DUP);
			il.insert(addUpdatesFor, factory.createGetField(className, field.getName(), type));
			il.insert(addUpdatesFor, factory.createGetField(className, OLD_PREFIX + field.getName(), type));

			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ACMPEQ, recursiveExtract));
			
			return start;
		}

		private InstructionHandle addUpdateExtractionForPrimitiveField(Field field, InstructionList il, InstructionHandle end) {
			Type type = field.getType();

			List<Type> args = new ArrayList<>();
			for (Type arg: ADD_UPDATES_FOR_ARGS)
				args.add(arg);
			args.add(type);

			InstructionHandle addUpdatesFor = il.insert(end, InstructionConst.ALOAD_2);
			il.insert(end, factory.createConstant(className));
			il.insert(end, factory.createConstant(field.getName()));
			il.insert(end, InstructionConst.ALOAD_1);
			il.insert(end, InstructionFactory.createThis());
			il.insert(end, factory.createGetField(className, field.getName(), type));
			il.insert(end, factory.createInvoke(Storage.class.getName(), ADD_UPDATES_FOR, Type.VOID, args.toArray(NO_TYPES), Const.INVOKESPECIAL));

			InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(Storage.class.getName(), IN_STORAGE_NAME, BasicType.BOOLEAN));
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, InstructionConst.DUP);
			il.insert(addUpdatesFor, factory.createGetField(className, field.getName(), type));
			il.insert(addUpdatesFor, factory.createGetField(className, OLD_PREFIX + field.getName(), type));

			if (field.getType().equals(Type.DOUBLE)) {
				il.insert(addUpdatesFor, InstructionConst.DCMPL);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else if (field.getType().equals(Type.FLOAT)) {
				il.insert(addUpdatesFor, InstructionConst.FCMPL);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else if (field.getType().equals(Type.LONG)) {
				il.insert(addUpdatesFor, InstructionConst.LCMP);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ICMPEQ, end));
			
			return start;
		}

		private void addAccessorMethods() {
			referenceNonTransientInstanceFields.forEach(this::addAccessorMethodsFor);
		}

		private void addAccessorMethodsFor(Field field) {
			addGetterFor(field);
			addPutterFor(field);
		}

		private void addPutterFor(Field field) {
			Type type = field.getType();
			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.DUP);
			il.append(factory.createInvoke(className, ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID, NO_TYPES, Const.INVOKESPECIAL));
			il.append(InstructionConst.ALOAD_1);
			il.append(factory.createPutField(className, field.getName(), type));
			il.append(InstructionConst.RETURN);

			MethodGen putter = new MethodGen(Const.ACC_PRIVATE | Const.ACC_SYNTHETIC, BasicType.VOID, new Type[] { type }, new String[] { field.getName() }, PUTTER_PREFIX + field.getName(), className, il, cpg);
			classGen.addMethod(putter.getMethod());
		}

		private void addGetterFor(Field field) {
			Type type = field.getType();
			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.DUP);
			il.append(factory.createInvoke(className, ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID, NO_TYPES, Const.INVOKESPECIAL));
			il.append(factory.createGetField(className, field.getName(), type));
			il.append(InstructionFactory.createReturn(type));

			MethodGen getter = new MethodGen(Const.ACC_PRIVATE | Const.ACC_SYNTHETIC, type, NO_TYPES, NO_STRINGS, GETTER_PREFIX + field.getName(), className, il, cpg);
			classGen.addMethod(getter.getMethod());
		}

		private void addEnsureLoadedMethods() {
			referenceNonTransientInstanceFields.forEach(this::addEnsureLoadedMethodFor);
		}

		private void addEnsureLoadedMethodFor(Field field) {
			InstructionList il = new InstructionList();
			InstructionHandle _return = il.append(InstructionConst.RETURN);
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, factory.createGetField(Storage.class.getName(), IN_STORAGE_NAME, BasicType.BOOLEAN));
			il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFEQ, _return));
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, factory.createGetField(className, IF_ALREADY_LOADED_PREFIX + field.getName(), BasicType.BOOLEAN));
			il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFNE, _return));
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, InstructionConst.DUP);
			il.insert(_return, InstructionConst.DUP);
			il.insert(_return, InstructionConst.ICONST_1);
			il.insert(_return, factory.createPutField(className, IF_ALREADY_LOADED_PREFIX + field.getName(), BasicType.BOOLEAN));
			il.insert(_return, factory.createConstant(className));
			il.insert(_return, factory.createConstant(field.getName()));
			il.insert(_return, factory.createConstant(((ObjectType) field.getType()).getClassName()));
			il.insert(_return, factory.createInvoke(className, DESERIALIZE_LAST_UPDATE_FOR, ObjectType.OBJECT, THREE_STRINGS, Const.INVOKEVIRTUAL));
			il.insert(_return, factory.createCast(ObjectType.OBJECT, field.getType()));
			il.insert(_return, factory.createPutField(className, OLD_PREFIX + field.getName(), field.getType()));

			MethodGen ensureLoaded = new MethodGen(Const.ACC_PRIVATE | Const.ACC_SYNTHETIC, BasicType.VOID, NO_TYPES, NO_STRINGS, ENSURE_LOADED_PREFIX + field.getName(), className, il, cpg);
			classGen.addMethod(ensureLoaded.getMethod());
		}

		private void addOldAndIfAlreadyLoadedFields() {
			for (Field field: primitiveNonTransientInstanceFields.getLast())
				addOldFieldFor(field);

			for (Field field: referenceNonTransientInstanceFields) {
				addOldFieldFor(field);
				addIfAlreadyLoadedFieldFor(field);
			}
		}

		private void addIfAlreadyLoadedFieldFor(Field field) {
			FieldGen ifAlreadyLoaded = new FieldGen(Const.ACC_PRIVATE | Const.ACC_SYNTHETIC, BasicType.BOOLEAN, IF_ALREADY_LOADED_PREFIX + field.getName(), cpg);
			classGen.addField(ifAlreadyLoaded.getField());
		}

		private void addOldFieldFor(Field field) {
			FieldGen copy = new FieldGen(field, cpg);
			copy.setName(OLD_PREFIX + field.getName());
			copy.setAccessFlags(Const.ACC_PRIVATE | Const.ACC_SYNTHETIC);
			classGen.addField(copy.getField());
		}

		private void addConstructorForDeserializationFromBlockchain() {
			List<Type> args = new ArrayList<>();
			List<String> names = new ArrayList<>();

			// the parameters of the constructor start with a storage reference
			// to the object being deserialized
			args.add(new ObjectType(StorageReference.class.getName()));
			names.add("storageReference");

			// then there are the fields of the class and superclasses, with superclasses first
			primitiveNonTransientInstanceFields.stream()
				.flatMap(SortedSet::stream)
				.map(Field::getType)
				.forEachOrdered(args::add);

			primitiveNonTransientInstanceFields.stream()
				.flatMap(SortedSet::stream)
				.map(Field::getName)
				.forEachOrdered(names::add);

			InstructionList il = new InstructionList();
			int nextLocal = addCallToSuper(il);
			addInitializationOfPrimitiveFields(il, nextLocal);
			il.append(InstructionConst.RETURN);

			MethodGen constructor = new MethodGen(Const.ACC_PUBLIC | Const.ACC_SYNTHETIC, BasicType.VOID, args.toArray(NO_TYPES), names.toArray(NO_STRINGS), Const.CONSTRUCTOR_NAME, className, il, cpg);
			classGen.addMethod(constructor.getMethod());
		}

		private int addCallToSuper(InstructionList il) {
			List<Type> argsForSuperclasses = new ArrayList<>();
			il.append(InstructionFactory.createThis());
			argsForSuperclasses.add(new ObjectType(StorageReference.class.getName()));
		
			// the fields of the superclasses are passed into a call to super(...)
			class PushLoad implements Consumer<Type> {
				private int local = 1;
		
				@Override
				public void accept(Type type) {
					argsForSuperclasses.add(type);
					il.append(InstructionFactory.createLoad(type, local));
					local += type.getSize();
				}
			};
		
			PushLoad pushLoad = new PushLoad();
			primitiveNonTransientInstanceFields.stream()
				.limit(primitiveNonTransientInstanceFields.size() - 1)
				.flatMap(SortedSet::stream)
				.map(Field::getType)
				.forEachOrdered(pushLoad);
		
			il.append(factory.createInvoke(className, Const.CONSTRUCTOR_NAME, BasicType.VOID, argsForSuperclasses.toArray(NO_TYPES), Const.INVOKESPECIAL));
		
			return pushLoad.local;
		}

		private void addInitializationOfPrimitiveFields(InstructionList il, int nextLocal) {
			Consumer<Field> pushField = new Consumer<Field>() {
				private int local = nextLocal;

				@Override
				public void accept(Field field) {
					Type type = field.getType();
					int size = type.getSize();
					il.append(InstructionFactory.createThis());
					il.append(InstructionFactory.createLoad(type, local));
					
					// we reduce the size of the code for the frequent case of one slot values
					if (size == 1)
						il.append(InstructionConst.DUP2);
					il.append(factory.createPutField(className, field.getName(), type));
					if (size != 1) {
						il.append(InstructionFactory.createThis());
						il.append(InstructionFactory.createLoad(type, local));
					}
					il.append(factory.createPutField(className, OLD_PREFIX + field.getName(), type));
					local += size;
				}
			};
			
			primitiveNonTransientInstanceFields.getLast().forEach(pushField);
		}

		private void collectPrimitiveNonTransientInstanceFieldsOf(String className) {
			if (!className.equals(Storage.class.getName())) {
				JavaClass clazz = program.get(className);
				if (clazz != null) {
					// we put at the beginning the fields of the superclasses
					collectPrimitiveNonTransientInstanceFieldsOf(clazz.getSuperclassName());

					// then the fields of className, in order
					primitiveNonTransientInstanceFields.add(Stream.of(clazz.getFields())
						.filter(field -> !field.isStatic() && !field.isTransient() && field.getType() instanceof BasicType)
						.collect(Collectors.toCollection(() -> new TreeSet<>(fieldOrder))));

					// we collect reference fields as well, but only for the class being instrumented
					if (className.equals(this.className))
						Stream.of(clazz.getFields())
							.filter(field -> !field.isStatic() && !field.isTransient() && field.getType() instanceof ReferenceType)
							.forEach(referenceNonTransientInstanceFields::add);
				}
			}
		}

		private boolean isStorage(String className) {
			if (className.equals(Storage.class.getName()))
				return true;
			else {
				JavaClass clazz = program.get(className);
				if (clazz == null)
					return false;
				else {
					String superclassName = clazz.getSuperclassName();
					return superclassName != null && isStorage(superclassName);
				}
			}
		}
	}
}