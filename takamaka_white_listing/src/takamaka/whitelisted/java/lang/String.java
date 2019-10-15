package takamaka.whitelisted.java.lang;

import takamaka.whitelisted.MustRedefineHashCodeOrToString;

public abstract class String {
	public String(java.lang.String original) {};
	public abstract int length();
	public abstract java.lang.String valueOf(int i);
	public abstract java.lang.String valueOf(@MustRedefineHashCodeOrToString java.lang.Object obj);
	public abstract java.lang.String concat(java.lang.String other);
}