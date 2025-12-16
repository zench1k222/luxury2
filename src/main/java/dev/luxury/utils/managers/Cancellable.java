public interface Cancellable {

    boolean isCancelled();

    void cancel();

}