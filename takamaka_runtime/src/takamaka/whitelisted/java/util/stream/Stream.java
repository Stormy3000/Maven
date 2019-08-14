package takamaka.whitelisted.java.util.stream;

public interface Stream<T> {
	<R> java.util.stream.Stream<R> of(R t);
	<R> java.util.stream.Stream<R> of(@SuppressWarnings("unchecked") R... values);
	<R> java.util.stream.Stream<R> ofNullable(R t);
	<R> java.util.stream.Stream<R> map(java.util.function.Function<? super T, ? extends R> mapper);
	java.lang.Object[] toArray();
	<A> A[] toArray(java.util.function.IntFunction<A[]> generator);
	java.util.stream.IntStream mapToInt(java.util.function.ToIntFunction<? super T> mapper);
	void forEachOrdered(java.util.function.Consumer<? super T> action);
	<R, A> R collect(java.util.stream.Collector<? super T, A, R> collector);
	boolean noneMatch(java.util.function.Predicate<? super T> predicate);
	boolean anyMatch(java.util.function.Predicate<? super T> predicate);
	boolean allMatch(java.util.function.Predicate<? super T> predicate);
	java.util.stream.Stream<T> filter(java.util.function.Predicate<? super T> predicate);
	java.util.stream.Stream<T> skip(long n);
	java.util.stream.Stream<T> limit(long maxSize);
	long count();
}