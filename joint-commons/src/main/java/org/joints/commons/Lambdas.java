package org.joints.commons;

import java.util.function.*;

/**
 * Lambda helper
 *
 * @author fan
 */
public class Lambdas {

	public interface ExBiConsumer<T, U> {
		void accept(T t, U u) throws Exception;
	}

	public static class ExBiConsumerWrapper<T, U> implements BiConsumer<T, U> {
		public final ExBiConsumer<T, U> ebc;
		public final Consumer<Exception> exceptionHandler;

		public ExBiConsumerWrapper(ExBiConsumer<T, U> ebc, Consumer<Exception> exceptionHandler) {
			super();
			this.ebc = ebc;
			this.exceptionHandler = exceptionHandler;
		}

		public void accept(T t, U u) {
			try {
				ebc.accept(t, u);
			} catch (Exception e) {
				exceptionHandler.accept(e);
			}
		}
	}

	public interface ExBiFunc<T, U, R> {
		R apply(T t, U u) throws Exception;
	}

	public static class ExBiFuncWrapper<T, U, R> implements BiFunction<T, U, R> {
		public final ExBiFunc<T, U, R> ebc;
		public final Consumer<Exception> exceptionHandler;
		public final Supplier<R> defaultValue;

		public ExBiFuncWrapper(ExBiFunc<T, U, R> ebc, Consumer<Exception> exceptionHandler, Supplier<R> defaultValue) {
			super();
			this.ebc = ebc;
			this.exceptionHandler = exceptionHandler;
			this.defaultValue = defaultValue;
		}

		public R apply(T t, U u) {
			try {
				return ebc.apply(t, u);
			} catch (Exception e) {
				if (exceptionHandler != null)
					exceptionHandler.accept(e);
			}
			return defaultValue.get();
		}
	}

	@FunctionalInterface
	public interface ExConsumer<T> {
		void accept(T t) throws Exception;
	}

	public static class ExConsumerWrapper<T> implements Consumer<T> {
		public final ExConsumer<T> ec;
		public final Consumer<Exception> exceptionHandler;

		public ExConsumerWrapper(ExConsumer<T> ec, Consumer<Exception> exceptionHandler) {
			super();
			this.ec = ec;
			this.exceptionHandler = exceptionHandler;
		}

		public void accept(T t) {
			try {
				ec.accept(t);
			} catch (Exception e) {
				exceptionHandler.accept(e);
			}
		}
	}

	public interface ExFunc<T, R> {
		R apply(T t) throws Exception;
	}

	public static class ExFuncWrapper<T, R> implements Function<T, R> {
		public final ExFunc<T, R> ef;
		public final Consumer<Exception> exceptionHandler;
		public final Supplier<R> defaultValue;

		public ExFuncWrapper(ExFunc<T, R> ebc, Consumer<Exception> exceptionHandler, Supplier<R> defaultValue) {
			super();
			this.ef = ebc;
			this.exceptionHandler = exceptionHandler;
			this.defaultValue = defaultValue;
		}

		public R apply(T t) {
			try {
				return ef.apply(t);
			} catch (Exception e) {
				exceptionHandler.accept(e);
			}
			return defaultValue.get();
		}
	}

	public interface ExSupplier<T> {
		T get() throws Exception;
	}

	public static class ExSupplierWrapper<T> implements Supplier<T> {
		public final ExSupplier<T> sup;
		public final Consumer<Exception> exceptionHandler;
		public final Supplier<T> defaultValue;

		public ExSupplierWrapper(ExSupplier<T> ebc, Consumer<Exception> exceptionHandler, Supplier<T> defaultValue) {
			super();
			this.sup = ebc;
			this.exceptionHandler = exceptionHandler;
			this.defaultValue = defaultValue;
		}

		public T get() {
			try {
				return sup.get();
			} catch (Exception e) {
				exceptionHandler.accept(e);
			}
			return defaultValue.get();
		}
	}

	public interface IExVarArgConsumer<T> {
		@SuppressWarnings("unchecked")
		void accept(T... args) throws Exception;
	}

	public static <T, U> ExBiConsumerWrapper<T, U> wbc(ExBiConsumer<T, U> ebc, Consumer<Exception> exceptionHandler) {
		return new ExBiConsumerWrapper<T, U>(ebc, exceptionHandler);
	}

	public static <T, U> ExBiConsumerWrapper<T, U[]> wbca1(ExBiConsumer<T, U[]> ebc, Consumer<Exception> exceptionHandler) {
		return new ExBiConsumerWrapper<T, U[]>(ebc, exceptionHandler);
	}

	public static <T, U> ExBiConsumerWrapper<T[], U> wbca2(ExBiConsumer<T[], U> ebc, Consumer<Exception> exceptionHandler) {
		return new ExBiConsumerWrapper<T[], U>(ebc, exceptionHandler);
	}

	public static <T, U> ExBiConsumerWrapper<T[], U[]> wbc3(ExBiConsumer<T[], U[]> ebc, Consumer<Exception> exceptionHandler) {
		return new ExBiConsumerWrapper<T[], U[]>(ebc, exceptionHandler);
	}

	public static <T, U> ExBiConsumerWrapper<T, U> wbc(ExBiConsumer<T, U> ebc) {
		return new ExBiConsumerWrapper<T, U>(ebc, Lambdas::defaultExceptionConsumer);
	}

	public static <T, U> ExBiConsumerWrapper<T, U[]> wbca1(ExBiConsumer<T, U[]> ebc) {
		return new ExBiConsumerWrapper<T, U[]>(ebc, Lambdas::defaultExceptionConsumer);
	}

	public static <T, U> ExBiConsumerWrapper<T[], U> wbca2(ExBiConsumer<T[], U> ebc) {
		return new ExBiConsumerWrapper<T[], U>(ebc, Lambdas::defaultExceptionConsumer);
	}

	public static <T, U> ExBiConsumerWrapper<T[], U[]> wbca3(ExBiConsumer<T[], U[]> ebc) {
		return new ExBiConsumerWrapper<T[], U[]>(ebc, Lambdas::defaultExceptionConsumer);
	}

	public static <T, U, R> ExBiFuncWrapper<T, U, R> wbf(ExBiFunc<T, U, R> ebc, Supplier<R> df, Consumer<Exception> exceptionHandler) {
		return new ExBiFuncWrapper<T, U, R>(ebc, exceptionHandler, df);
	}

	public static <T, U, R> ExBiFuncWrapper<T[], U[], R> wbfa5(ExBiFunc<T[], U[], R> ebc, Supplier<R> df, Consumer<Exception> exceptionHandler) {
		return new ExBiFuncWrapper<T[], U[], R>(ebc, exceptionHandler, df);
	}

	public static <T, U, R> ExBiFuncWrapper<T, U[], R> wbfa6(ExBiFunc<T, U[], R> ebc, Supplier<R> df, Consumer<Exception> exceptionHandler) {
		return new ExBiFuncWrapper<T, U[], R>(ebc, exceptionHandler, df);
	}

	public static <T, U, R> ExBiFuncWrapper<T[], U, R> wbfa7(ExBiFunc<T[], U, R> ebc, Supplier<R> df, Consumer<Exception> exceptionHandler) {
		return new ExBiFuncWrapper<T[], U, R>(ebc, exceptionHandler, df);
	}

	public static <T, U, R> ExBiFuncWrapper<T, U, R[]> wbfa1(ExBiFunc<T, U, R[]> ebc, Supplier<R[]> df, Consumer<Exception> exceptionHandler) {
		return new ExBiFuncWrapper<T, U, R[]>(ebc, exceptionHandler, df);
	}

	public static <T, U, R> ExBiFuncWrapper<T, U[], R[]> wbfa2(ExBiFunc<T, U[], R[]> ebc, Supplier<R[]> df, Consumer<Exception> exceptionHandler) {
		return new ExBiFuncWrapper<T, U[], R[]>(ebc, exceptionHandler, df);
	}

	public static <T, U, R> ExBiFuncWrapper<T[], U, R[]> wbfa3(ExBiFunc<T[], U, R[]> ebc, Supplier<R[]> df, Consumer<Exception> exceptionHandler) {
		return new ExBiFuncWrapper<T[], U, R[]>(ebc, exceptionHandler, df);
	}

	public static <T, U, R> ExBiFuncWrapper<T[], U[], R[]> wbfa4(ExBiFunc<T[], U[], R[]> ebc, Supplier<R[]> df, Consumer<Exception> exceptionHandler) {
		return new ExBiFuncWrapper<T[], U[], R[]>(ebc, exceptionHandler, df);
	}

	public static <T, U, R> ExBiFuncWrapper<T, U, R> wbf(ExBiFunc<T, U, R> ebc) {
		return new ExBiFuncWrapper<T, U, R>(ebc, Lambdas::defaultExceptionConsumer, Lambdas::defaultValue);
	}


	public static <T, R> ExFuncWrapper<T, R> wf(ExFunc<T, R> ebc, Supplier<R> df, Consumer<Exception> exceptionHandler) {
		return new ExFuncWrapper<T, R>(ebc, exceptionHandler, df);
	}

	public static <T, R> ExFuncWrapper<T, R[]> wfa1(ExFunc<T, R[]> ebc, Supplier<R[]> df, Consumer<Exception> exceptionHandler) {
		return new ExFuncWrapper<T, R[]>(ebc, exceptionHandler, df);
	}

	public static <T, R> ExFuncWrapper<T[], R> wfa2(ExFunc<T[], R> ebc, Supplier<R> df, Consumer<Exception> exceptionHandler) {
		return new ExFuncWrapper<T[], R>(ebc, exceptionHandler, df);
	}

	public static <T, R> ExFuncWrapper<T[], R[]> wfa3(ExFunc<T[], R[]> ebc, Supplier<R[]> df, Consumer<Exception> exceptionHandler) {
		return new ExFuncWrapper<T[], R[]>(ebc, exceptionHandler, df);
	}

	public static <T, R> ExFuncWrapper<T, R[]> wfa1(ExFunc<T, R[]> ebc) {
		return new ExFuncWrapper<T, R[]>(ebc, Lambdas::defaultExceptionConsumer, Lambdas::defaultValue);
	}

	public static <T, R> ExFuncWrapper<T[], R> wfa2(ExFunc<T[], R> ebc) {
		return new ExFuncWrapper<T[], R>(ebc, Lambdas::defaultExceptionConsumer, Lambdas::defaultValue);
	}

	public static <T, R> ExFuncWrapper<T[], R[]> wfa3(ExFunc<T[], R[]> ebc) {
		return new ExFuncWrapper<T[], R[]>(ebc, Lambdas::defaultExceptionConsumer, Lambdas::defaultValue);
	}

	public static <T, R> ExFuncWrapper<T, R> wf(ExFunc<T, R> ebc) {
		return new ExFuncWrapper<T, R>(ebc, Lambdas::defaultExceptionConsumer, Lambdas::defaultValue);
	}


	public static <T> ExConsumerWrapper<T> wc(ExConsumer<T> ebc, Consumer<Exception> exceptionHandler) {
		return new ExConsumerWrapper<T>(ebc, exceptionHandler);
	}

	public static <T> ExConsumerWrapper<T[]> wca(ExConsumer<T[]> ebc, Consumer<Exception> exceptionHandler) {
		return new ExConsumerWrapper<T[]>(ebc, exceptionHandler);
	}

	public static <T> ExConsumerWrapper<T> wc(ExConsumer<T> ebc) {
		return new ExConsumerWrapper<T>(ebc, Lambdas::defaultExceptionConsumer);
	}

	public static <T> ExConsumerWrapper<T[]> wca(ExConsumer<T[]> ebc) {
		return new ExConsumerWrapper<T[]>(ebc, Lambdas::defaultExceptionConsumer);
	}


	public static <T> ExSupplierWrapper<T> ws(ExSupplier<T> ebc, Supplier<T> df, Consumer<Exception> exceptionHandler) {
		return new ExSupplierWrapper<T>(ebc, exceptionHandler, df);
	}

	public static <T> ExSupplierWrapper<T[]> wsa(ExSupplier<T[]> ebc, Supplier<T[]> df, Consumer<Exception> exceptionHandler) {
		return new ExSupplierWrapper<T[]>(ebc, exceptionHandler, df);
	}

	public static <T> ExSupplierWrapper<T> ws(ExSupplier<T> ebc) {
		return new ExSupplierWrapper<T>(ebc, Lambdas::defaultExceptionConsumer, Lambdas::defaultValue);
	}

	public static <T> ExSupplierWrapper<T[]> wsa(ExSupplier<T[]> ebc) {
		return new ExSupplierWrapper<T[]>(ebc, Lambdas::defaultExceptionConsumer, Lambdas::defaultValue);
	}

	public static void defaultExceptionConsumer(Throwable e) {
		e.printStackTrace();
	}

	public static <T> T defaultValue() {
		return null;
	}
}
