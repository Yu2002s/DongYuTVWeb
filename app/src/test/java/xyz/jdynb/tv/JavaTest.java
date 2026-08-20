package xyz.jdynb.tv;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class JavaTest {

    public static void main(String[] args) {
        Result<String> result = runCatching(new Runnable<String>() {
            @Override
            public String run() {
                if (new Random(10).nextInt(10) % 2 == 0) {
                    throw new IllegalStateException("Error");
                }
                return "Success";
            }
        }).onSuccess(new Consumer<String>() {
            @Override
            public void accept(String s) {
                // 成功
                System.out.println(s);
            }
        }).onFailure(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                // 失败
                System.out.println(throwable.getMessage());
            }
        });

        String string = result.getOrNull();
        String string2 = result.getOrThrow();
        String string3 = result.getOrDefault("Default Value");
        String string4 = result.getOrElse(new Supplier<String>() {
            @Override
            public String get() {
                System.out.println("Getting default value");
                return "Default Value";
            }
        });
    }

    public interface Runnable<T> {
        T run();
    }

    public static class Result<T> {

        private T value;

        private Throwable throwable;

        public static <T> Result<T> success(T value) {
            Result<T> result = new Result<>();
            result.value = value;
            return result;
        }

        public static <T> Result<T> error(Throwable throwable) {
            Result<T> result = new Result<>();
            result.throwable = throwable;
            return result;
        }

        public T getOrNull() {
            return value;
        }

        public T getOrThrow() {
            if (throwable != null) {
                throw new RuntimeException(throwable);
            }
            return value;
        }

        public T getOrDefault(T defaultValue) {
            if (throwable != null) {
                return defaultValue;
            }
            return value;
        }

        public T getOrElse(Supplier<T> supplier) {
            if (throwable != null) {
                return supplier.get();
            }
            return value;
        }

        public boolean isSuccessful() {
            return throwable == null;
        }

        public Result<T> onSuccess(Consumer<T> consumer) {
            if (throwable == null) {
                consumer.accept(value);
            }
            return this;
        }

        public Result<T> onFailure(Consumer<Throwable> consumer) {
            if (throwable != null) {
                consumer.accept(throwable);
            }
            return this;
        }
    }

    public static <T> Result<T> runCatching(Runnable<T> runnable) {
        try {
            return Result.success(runnable.run());
        } catch (Exception e) {
            // 全局处理错误
            return Result.error(e);
        }
    }

}
