package takamaka.whitelisted.java.util.function;

public interface IntUnaryOperator {
	int applyAsInt(int operand);
	java.util.function.IntUnaryOperator compose(java.util.function.IntUnaryOperator before);
	java.util.function.IntUnaryOperator andThen(java.util.function.IntUnaryOperator before);
	java.util.function.IntUnaryOperator identity();
}