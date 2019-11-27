package io.takamaka.code.instrumentation.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;

import io.takamaka.code.instrumentation.Constants;
import io.takamaka.code.instrumentation.GasCostModel;
import io.takamaka.code.instrumentation.InstrumentedClass;
import io.takamaka.code.instrumentation.internal.instrumentationsOfClass.AddAccessorMethods;
import io.takamaka.code.instrumentation.internal.instrumentationsOfClass.AddConstructorForDeserializationFromBlockchain;
import io.takamaka.code.instrumentation.internal.instrumentationsOfClass.AddEnsureLoadedMethods;
import io.takamaka.code.instrumentation.internal.instrumentationsOfClass.AddExtractUpdates;
import io.takamaka.code.instrumentation.internal.instrumentationsOfClass.AddOldAndIfAlreadyLoadedFields;
import io.takamaka.code.instrumentation.internal.instrumentationsOfClass.DesugarBootstrapsInvokingEntries;
import io.takamaka.code.instrumentation.internal.instrumentationsOfClass.SwapSuperclassOfSpecialClasses;
import io.takamaka.code.instrumentation.internal.instrumentationsOfMethod.AddContractToCallsToEntries;
import io.takamaka.code.instrumentation.internal.instrumentationsOfMethod.AddGasUpdates;
import io.takamaka.code.instrumentation.internal.instrumentationsOfMethod.AddRuntimeChecksForWhiteListingProofObligations;
import io.takamaka.code.instrumentation.internal.instrumentationsOfMethod.InstrumentMethodsOfSupportClasses;
import io.takamaka.code.instrumentation.internal.instrumentationsOfMethod.ReplaceFieldAccessesWithAccessors;
import io.takamaka.code.instrumentation.internal.instrumentationsOfMethod.SetCallerAndBalanceAtTheBeginningOfEntries;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.ThrowIncompleteClasspathError;
import io.takamaka.code.verification.VerifiedClass;
import it.univr.bcel.StackMapReplacer;

/**
 * An instrumenter of a single class file. For instance, it instruments storage
 * classes, by adding the serialization support, and contracts, to deal with entries.
 */
public class InstrumentedClassImpl implements InstrumentedClass {

	/**
	 * The order used for generating the parameters of the instrumented constructors.
	 */
	private final static Comparator<Field> fieldOrder = Comparator.comparing(Field::getName)
		.thenComparing(field -> field.getType().toString());

	/**
	 * The Java class of this instrumented class.
	 */
	private final JavaClass javaClass;

	/**
	 * Performs the instrumentation of a single class file.
	 * 
	 * @param clazz the class to instrument
	 * @param gasCostModel the gas cost model used for the instrumentation
	 * @param instrumentedJar the jar where the instrumented class will be added
	 */
	public InstrumentedClassImpl(VerifiedClass clazz, GasCostModel gasCostModel) {
		this.javaClass = new Builder(clazz, gasCostModel).classGen.getJavaClass();
	}

	@Override
	public String getClassName() {
		return javaClass.getClassName();
	}

	@Override
	public JavaClass toJavaClass() {
		return javaClass;
	}

	/**
	 * Local scope for the instrumentation of a single class.
	 */
	public class Builder {

		/**
		 * The class that is being instrumented.
		 */
		private final VerifiedClass verifiedClass;

		/**
		 * The class generator of the verified class.
		 */
		private final ClassGen classGen;

		/**
		 * The methods of the instrumented class, in editable version.
		 */
		private final List<MethodGen> methods;

		/**
		 * The gas cost model used for the instrumentation.
		 */
		private final GasCostModel gasCostModel;

		/**
		 * The constant pool of the class being instrumented.
		 */
		private final ConstantPoolGen cpg;

		/**
		 * The object that can be used to build complex instructions.
		 */
		private final InstructionFactory factory;

		/**
		 * True if and only if the class being instrumented is a storage class.
		 */
		private final boolean isStorage;

		/**
		 * True if and only if the class being instrumented is a contract class.
		 */
		private final boolean isContract;

		/**
		 * The non-transient instance fields of primitive type or of special reference
		 * types that are allowed in storage objects (such as {@link java.lang.String}
		 * and {@link java.math.BigInteger}). They are defined in the class being
		 * instrumented or in its superclasses up to {@link io.takamaka.code.lang.Storage}.
		 * This list is non-empty for storage classes only. The first set in
		 * the list are the fields of {@link io.takamaka.code.lang.Storage};
		 * the last are the fields of the class being instrumented.
		 */
		private final LinkedList<SortedSet<Field>> eagerNonTransientInstanceFields = new LinkedList<>();

		/**
		 * The non-transient instance fields of reference type,
		 * defined in the class being instrumented (superclasses are not
		 * considered). This set is non-empty for storage classes only.
		 */
		private final SortedSet<Field> lazyNonTransientInstanceFields = new TreeSet<>(fieldOrder);

		/**
		 * The class loader that loaded the class under instrumentation and those of the program it belongs to.
		 */
		private final TakamakaClassLoader classLoader;

		/**
		 * The bootstrap methods that have been instrumented since they must receive an
		 * extra parameter, since they call an entry and need the calling contract for that.
		 */
		private final Set<BootstrapMethod> bootstrapMethodsThatWillRequireExtraThis = new HashSet<>();

		/**
		 * A map from a description of invoke instructions that lead into a white-listed method
		 * with proof obligations into the replacement instruction
		 * that has been already computed for them. This is used to avoid recomputing
		 * the replacement for invoke instructions that occur more times inside the same
		 * class. This is not just an optimization, since, for invokedynamic, their bootstrap
		 * might be modified, hence the repeated construction of their checking method
		 * would lead into exception.
		 */
		private final Map<String, InvokeInstruction> whiteListingCache = new HashMap<>();

		/**
		 * Performs the instrumentation of a single class.
		 * 
		 * @param clazz the class to instrument
		 * @param gasCostModel the gas cost model used for the instrumentation
		 */
		private Builder(VerifiedClass clazz, GasCostModel gasCostModel) {
			this.verifiedClass = clazz;
			this.classGen = new ClassGen(clazz.toJavaClass());
			this.gasCostModel = gasCostModel;
			this.classLoader = clazz.getJar().getClassLoader();
			this.cpg = classGen.getConstantPool();
			String className = classGen.getClassName();
			this.methods = Stream.of(classGen.getMethods())
				.map(method -> new MethodGen(method, className, cpg))
				.collect(Collectors.toList());
			this.factory = new InstructionFactory(cpg);
			this.isStorage = classLoader.isStorage(className);
			this.isContract = classLoader.isContract(className);

			partitionFieldsOfStorageClasses();
			methodLevelInstrumentations();
			classLevelInstrumentations();
			replaceMethods();
		}

		/**
		 * Partitions the fields of a storage class into eager and lazy.
		 * If the class is not storage, it does not do anything.
		 */
		private void partitionFieldsOfStorageClasses() {
			if (isStorage)
				ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
					collectNonTransientInstanceFieldsOf(classLoader.loadClass(classGen.getClassName()), true);
				});
		}

		/**
		 * Replaces the methods of the class under instrumentation,
		 * putting the instrumented ones instead of the original ones.
		 */
		private void replaceMethods() {
			classGen.setMethods(new Method[0]);
			for (MethodGen method: methods) {
				method.setMaxLocals();
				method.setMaxStack();
				if (!method.isAbstract()) {
					method.getInstructionList().setPositions();
					StackMapReplacer.of(method);
				}
				classGen.addMethod(method.getMethod());
			}
		}

		public abstract class ClassLevelInstrumentation {

			/**
			 * The verified class for which instrumentation is performed.
			 */
			protected final VerifiedClass verifiedClass = Builder.this.verifiedClass;

			/**
			 * The gas cost model used for the instrumentation.
			 */
			protected final GasCostModel gasCostModel = Builder.this.gasCostModel;

			/**
			 * The name of the class being instrumented.
			 */
			protected final String className = classGen.getClassName();

			/**
			 * The constant pool of the class being instrumented.
			 */
			protected final ConstantPoolGen cpg = Builder.this.cpg;

			/**
			 * The object that can be used to build complex instructions.
			 */
			protected final InstructionFactory factory = Builder.this.factory;

			/**
			 * True if and only if the class being instrumented is a storage class.
			 */
			protected final boolean isStorage = Builder.this.isStorage;

			/**
			 * True if and only if the class being instrumented is a contract class.
			 */
			protected final boolean isContract = Builder.this.isContract;

			/**
			 * The non-transient instance fields of primitive type or of special reference
			 * types that are allowed in storage objects (such as {@link java.lang.String}
			 * and {@link java.math.BigInteger}). They are defined in the class being
			 * instrumented or in its superclasses up to {@link io.takamaka.code.lang.Storage}
			 * (excluded). This list is non-empty for storage classes only. The first set in
			 * the list are the fields of the topmost class; the last are the fields of the
			 * class being considered.
			 */
			protected final LinkedList<SortedSet<Field>> eagerNonTransientInstanceFields = Builder.this.eagerNonTransientInstanceFields;

			/**
			 * The non-transient instance fields of reference type,
			 * defined in the class being instrumented (superclasses are not
			 * considered). This set is non-empty for storage classes only.
			 */
			protected final SortedSet<Field> lazyNonTransientInstanceFields = Builder.this.lazyNonTransientInstanceFields;

			/**
			 * The bootstrap methods that have been instrumented since they must receive an
			 * extra parameter, since they call an entry and need the calling contract for that.
			 */
			protected final Set<BootstrapMethod> bootstrapMethodsThatWillRequireExtraThis = Builder.this.bootstrapMethodsThatWillRequireExtraThis;

			/**
			 * The class loader that loaded the class under instrumentation and those of the program it belongs to.
			 */
			protected final TakamakaClassLoader classLoader = Builder.this.classLoader;

			/**
			 * A map from a description of invoke instructions that lead into a white-listed method
			 * with proof obligations into the replacement instruction
			 * that has been already computed for them. This is used to avoid recomputing
			 * the replacement for invoke instructions that occur more times inside the same
			 * class. This is not just an optimization, since, for invokedynamic, their bootstrap
			 * might be modified, hence the repeated construction of their checking method
			 * would lead into exception.
			 */
			protected final Map<String, InvokeInstruction> whiteListingCache = Builder.this.whiteListingCache;

			protected final void addMethod(MethodGen methodGen, boolean needsStackMap) {
				methodGen.getInstructionList().setPositions();
				methodGen.setMaxLocals();
				methodGen.setMaxStack();
				if (needsStackMap)
					StackMapReplacer.of(methodGen);
				methods.add(methodGen);
			}

			protected final String getterNameFor(String className, String fieldName) {
				// we use the class name as well, in order to disambiguate fields with the same name in sub and superclass
				return Constants.GETTER_PREFIX + className.replace('.', '_') + '_' + fieldName;
			}

			protected final String setterNameFor(String className, String fieldName) {
				// we use the class name as well, in order to disambiguate fields with the same name in sub and superclass
				return Constants.SETTER_PREFIX + className.replace('.', '_') + '_' + fieldName;
			}

			protected final short invokeCorrespondingToBootstrapInvocationType(int invokeKind) {
				switch (invokeKind) {
				case Const.REF_invokeVirtual:
					return Const.INVOKEVIRTUAL;
				case Const.REF_invokeSpecial:
				case Const.REF_newInvokeSpecial:
					return Const.INVOKESPECIAL;
				case Const.REF_invokeInterface:
					return Const.INVOKEINTERFACE;
				case Const.REF_invokeStatic:
					return Const.INVOKESTATIC;
				default:
					throw new IllegalStateException("Unexpected lambda invocation kind " + invokeKind);
				}
			}

			/**
			 * BCEL does not (yet?) provide a method to add a method handle constant into a
			 * constant pool. Hence we have to rely to a trick: first we add a new integer
			 * constant to the constant pool; then we replace it with the method handle
			 * constant. Ugly, but it currently seems to be the only way.
			 * 
			 * @param mh the constant to add
			 * @return the index at which the constant has been added
			 */
			protected final int addMethodHandleToConstantPool(ConstantMethodHandle mh) {
				// first we check if an equal constant method handle was already in the constant pool
				int size = cpg.getSize(), index;
				for (index = 0; index < size; index++)
					if (cpg.getConstant(index) instanceof ConstantMethodHandle) {
						ConstantMethodHandle c = (ConstantMethodHandle) cpg.getConstant(index);
						if (c.getReferenceIndex() == mh.getReferenceIndex() && c.getReferenceKind() == mh.getReferenceKind())
							return index; // found
					}

				// otherwise, we first add an integer that was not already there
				int counter = 0;
				do {
					index = cpg.addInteger(counter++);
				}
				while (cpg.getSize() == size);

				// and then replace the integer constant with the method handle constant
				cpg.setConstant(index, mh);

				return index;
			}

			/**
			 * Yields the methods in this class.
			 * 
			 * @return the methods
			 */
			protected final Stream<MethodGen> getMethods() {
				return methods.stream();
			}

			/**
			 * Yields the fields in this class.
			 * 
			 * @return the fields
			 */
			protected final Stream<org.apache.bcel.classfile.Field> getFields() {
				return Stream.of(classGen.getFields());
			}

			/**
			 * Yields the name for a method that starts with the given prefix, followed
			 * by a numerical index. It guarantees that the name is not yet used for
			 * existing methods.
			 *
			 * @param prefix the prefix
			 * @return the name
			 */
			protected final String getNewNameForPrivateMethod(String prefix) {
				int counter = 0;
				String newName;

				do {
					newName = prefix + counter++;
				}
				while (getMethods().map(MethodGen::getName).anyMatch(newName::equals));

				return newName;
			}

			/**
			 * Finds the closest instructions whose stack height, at their beginning, is
			 * equal to the height of the stack at {@code ih} minus {@code slots}.
			 * 
			 * @param ih the start instruction of the look up
			 * @param slots the difference in stack height
			 */
			protected final void forEachPusher(InstructionHandle ih, int slots, Consumer<InstructionHandle> what, Runnable ifCannotFollow) {
				Set<HeightAtBytecode> seen = new HashSet<>();
				List<HeightAtBytecode> workingSet = new ArrayList<>();
				HeightAtBytecode start = new HeightAtBytecode(ih, slots);
				workingSet.add(start);
				seen.add(start);

				do {
					HeightAtBytecode current = workingSet.remove(workingSet.size() - 1);
					InstructionHandle currentIh = current.ih;
					if (current.stackHeightBeforeBytecode <= 0)
						what.accept(currentIh);
					else {
						InstructionHandle previous = currentIh.getPrev();
						if (previous != null) {
							Instruction previousIns = previous.getInstruction();
							if (!(previousIns instanceof ReturnInstruction) && !(previousIns instanceof ATHROW)
									&& !(previousIns instanceof GotoInstruction)) {
								// we proceed with previous
								int stackHeightBefore = current.stackHeightBeforeBytecode;
								stackHeightBefore -= previousIns.produceStack(cpg);
								stackHeightBefore += previousIns.consumeStack(cpg);

								HeightAtBytecode added = new HeightAtBytecode(previous, stackHeightBefore);
								if (seen.add(added))
									workingSet.add(added);
							}
						}

						// we proceed with the instructions that jump at currentIh
						InstructionTargeter[] targeters = currentIh.getTargeters();
						if (Stream.of(targeters).anyMatch(targeter -> targeter instanceof CodeExceptionGen))
							ifCannotFollow.run();

						Stream.of(targeters).filter(targeter -> targeter instanceof BranchInstruction)
							.map(targeter -> (BranchInstruction) targeter).forEach(branch -> {
								int stackHeightBefore = current.stackHeightBeforeBytecode;
								stackHeightBefore -= branch.produceStack(cpg);
								stackHeightBefore += branch.consumeStack(cpg);

								HeightAtBytecode added = new HeightAtBytecode(previous, stackHeightBefore);
								if (seen.add(added))
									workingSet.add(added);
							});
					}
				}
				while (!workingSet.isEmpty());
			}

			/**
			 * Sets the name of the superclass of this class.
			 * 
			 * @param name the new name of the superclass of this clas
			 */
			protected final void setSuperclassName(String name) {
				classGen.setSuperclassName(name);
			}

			/**
			 * Adds the given field to this class.
			 * 
			 * @param field the field to add
			 */
			protected final void addField(org.apache.bcel.classfile.Field field) {
				classGen.addField(field);
			}

			/**
			 * Replaces a field of this class with another.
			 * 
			 * @param old the old field to replace
			 * @param _new the new field to put at its place
			 */
			protected final void replaceField(org.apache.bcel.classfile.Field old, org.apache.bcel.classfile.Field _new) {
				classGen.replaceField(old, _new);
			}

			/**
			 * Yields the name of the superclass of this class, if any.
			 * 
			 * @return the name
			 */
			protected final String getSuperclassName() {
				return classGen.getSuperclassName();
			}
		}

		public abstract class MethodLevelInstrumentation extends ClassLevelInstrumentation {
			protected final MethodGen method;

			protected MethodLevelInstrumentation(MethodGen method) {
				this.method = method;
			}
		}

		/**
		 * Performs class-level instrumentations.
		 */
		private void classLevelInstrumentations() {
			new SwapSuperclassOfSpecialClasses(this);
			new AddConstructorForDeserializationFromBlockchain(this);
			new AddOldAndIfAlreadyLoadedFields(this);
			new AddAccessorMethods(this);
			new AddEnsureLoadedMethods(this);
			new AddExtractUpdates(this);
		}

		/**
		 * Performs method-level instrumentations.
		 */
		private void methodLevelInstrumentations() {
			new ArrayList<>(methods).forEach(this::preProcess);
			new DesugarBootstrapsInvokingEntries(this);
			new ArrayList<>(methods).forEach(this::postProcess);
		}

		/**
		 * Pre-processing instrumentation of a single method of the class. This is
		 * performed before instrumentation of the bootstraps.
		 * 
		 * @param method the method to instrument
		 */
		private void preProcess(MethodGen method) {
			new AddRuntimeChecksForWhiteListingProofObligations(this, method);
		}

		/**
		 * Post-processing instrumentation of a single method of the class. This is
		 * performed after instrumentation of the bootstraps.
		 * 
		 * @param method the method to instrument
		 */
		private void postProcess(MethodGen method) {
			new InstrumentMethodsOfSupportClasses(this, method);
			new ReplaceFieldAccessesWithAccessors(this, method);
			new AddContractToCallsToEntries(this, method);
			new SetCallerAndBalanceAtTheBeginningOfEntries(this, method);
			new AddGasUpdates(this, method);
		}

		private boolean isStaticOrTransient(Field field) {
			int modifiers = field.getModifiers();
			return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers);
		}

		private void collectNonTransientInstanceFieldsOf(Class<?> clazz, boolean firstCall) {
			if (clazz != classLoader.getStorage())
				// we put at the beginning the fields of the superclasses
				collectNonTransientInstanceFieldsOf(clazz.getSuperclass(), false);

			Field[] fields = clazz.getDeclaredFields();

			// then the eager fields of className, in order
			eagerNonTransientInstanceFields.add(Stream.of(fields)
				.filter(field -> !isStaticOrTransient(field) && classLoader.isEagerlyLoaded(field.getType()))
				.collect(Collectors.toCollection(() -> new TreeSet<>(fieldOrder))));

			// we collect lazy fields as well, but only for the class being instrumented
			if (firstCall)
				Stream.of(fields)
					.filter(field -> !isStaticOrTransient(field) && classLoader.isLazilyLoaded(field.getType()))
					.forEach(lazyNonTransientInstanceFields::add);
		}
	}
}