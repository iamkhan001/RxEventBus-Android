# RxEventBus-Android
If you’ve ever needed to communicate between different parts of your application, it can be painful. To alleviate this, you can use an event bus like Otto. But, with the addition of RxJava and RxAndroid to the Android ecosystem you don’t need to rely on Otto anymore. Otto is actually deprecated in favour of these newer libraries, since making your own event bus with them is actually quite easy.

I came up with my own solution that works well for my purposes. You can use it as is if you want, or tweak it to fit your needs.

# Create Event Bus 

```sh
public final class RxBus {

    private static SparseArray<PublishSubject<Object>> sSubjectMap = new SparseArray<>();
    private static Map<Object, CompositeDisposable> sSubscriptionsMap = new HashMap<>();

    public static final int SUBJECT_MY_SUBJECT = 0;
    public static final int SUBJECT_ANOTHER_SUBJECT = 1;

    @Retention(SOURCE)
    @IntDef({SUBJECT_MY_SUBJECT, SUBJECT_ANOTHER_SUBJECT})
    @interface Subject {
    }

    private RxBus() {
        // hidden constructor
    }

    /**
     * Get the subject or create it if it's not already in memory.
     */
    @NonNull
    private static PublishSubject<Object> getSubject(@Subject int subjectCode) {
        PublishSubject<Object> subject = sSubjectMap.get(subjectCode);
        if (subject == null) {
            subject = PublishSubject.create();
            subject.subscribeOn(AndroidSchedulers.mainThread());
            sSubjectMap.put(subjectCode, subject);
        }

        return subject;
    }

    /**
     * Get the CompositeDisposable or create it if it's not already in memory.
     */
    @NonNull
    private static CompositeDisposable getCompositeDisposable(@NonNull Object object) {
        CompositeDisposable compositeDisposable = sSubscriptionsMap.get(object);
        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
            sSubscriptionsMap.put(object, compositeDisposable);
        }

        return compositeDisposable;
    }

    /**
     * Subscribe to the specified subject and listen for updates on that subject. Pass in an object to associate
     * your registration with, so that you can unsubscribe later.
     * <br/><br/>
     * <b>Note:</b> Make sure to call {@link RxBus#unregister(Object)} to avoid memory leaks.
     */
    public static void subscribe(@Subject int subject, @NonNull Object lifecycle, @NonNull Consumer<Object> action) {
        Disposable disposable = getSubject(subject).subscribe(action);
        getCompositeDisposable(lifecycle).add(disposable);
    }

    /**
     * Unregisters this object from the bus, removing all subscriptions.
     * This should be called when the object is going to go out of memory.
     */
    public static void unregister(@NonNull Object lifecycle) {
        //We have to remove the composition from the map, because once you dispose it can't be used anymore
        CompositeDisposable compositeDisposable = sSubscriptionsMap.remove(lifecycle);
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
    }

    /**
     * Publish an object to the specified subject for all subscribers of that subject.
     */
    public static void publish(@Subject int subject, @NonNull Object message) {
        getSubject(subject).onNext(message);
    }
}
```
# Subscribe and UnSubscribe 
Also, to simplify management of unsubscribing and keeping a reference to those subscriptions, I created a BaseActivity and BaseFragment.

###Subscribe  
```sh

  RxBus.subscribe(RxBus.SUBJECT_MY_SUBJECT, this, new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {

                        Data data = (Data) o;
                        Toast.makeText(MainActivity.this,"Event Received\n"+data.getMessage(),Toast.LENGTH_SHORT).show();
                        Log.v("Testing", data.getMessage());
                    }
                }
        );
```

### Unsubscribe 
####For Activity
```sh
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.unregister(this);
    }
}

```

#### for Fragment 
```sh
public abstract class BaseFragment extends Fragment {

    @Override
    public void onDestroy() {
        super.onDestroy();
        RxBus.unregister(this);
    }
}
```
###Send Event 
```sh

    RxBus.publish(RxBus.SUBJECT_MY_SUBJECT,new Data("Hello World!"));

```



# Potential issues
While working on this, I made a list of problems I had with the implementation. Some of which I believe can be addressed, others I’m not sure.

I’m still passing around objects which have to be cast to the correct type. I’m not sure if there’s a way around this, because the subject publishes Objects. So, the subscriber will only receive Objects.

You can pass in any object to associate your subscription with, so there’s no guarantee that you’ve actually unsubscribed. I tried to address this with my comments, saying that you must call unregister. But there’s no guarantee that it gets called, which will cause memory leaks.

The BaseActivity and BaseFragment unregister from the bus in onDestroy(). This means that if you start a new activity, the old activity will still be subscribed. So if you publish an event that the previous activity is subscribed to, it may end up causing your app to crash with java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState. I didn’t want to call unregister in onStop() because if you go back to the previous activity, it won’t be subscribed anymore. If you are careful with how you manage your subjects, this won't be an issue. Ideally the subscriptions would pause and resume with the lifecycle, and finally get destroyed with the lifecycle.

Lastly, I’m using static members instead of the singleton pattern. Technically I believe that using the singleton pattern is more memory efficient. Since it will only create the class when it needs to. But, in my case since I’m using RxBus in onCreate() for most of my activities, it won’t really save anything. Plus, the amount of memory it uses is negligible. Some people also think that static variables are evil.

#### Originally developed by
[Pierce Zaifman](https://gist.github.com/PierceZ)
