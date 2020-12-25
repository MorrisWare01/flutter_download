package com.morrisware.flutter.net.core;

import java.util.HashSet;

/**
 * @author mmw
 * @date 2020/3/23
 **/
public class CompositeDisposable implements Disposable {

    private HashSet<Disposable> resources;

    private volatile boolean disposed;

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        HashSet<Disposable> set;
        synchronized (this) {
            if (disposed) {
                return;
            }
            disposed = true;
            set = resources;
            resources = null;
        }

        dispose(set);
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    public boolean add(Disposable disposable) {
        if (disposable == null) {
            return false;
        }
        if (!disposed) {
            synchronized (this) {
                if (!disposed) {
                    HashSet<Disposable> set = resources;
                    if (set == null) {
                        set = new HashSet<>();
                        resources = set;
                    }
                    set.add(disposable);
                    return true;
                }
            }
        }
        disposable.dispose();
        return false;
    }

    public boolean addAll(Disposable... disposables) {
        if (disposables == null) {
            return false;
        }
        if (!disposed) {
            synchronized (this) {
                if (!disposed) {
                    HashSet<Disposable> set = resources;
                    if (set == null) {
                        set = new HashSet<>(disposables.length + 1);
                        resources = set;
                    }
                    for (Disposable d : disposables) {
                        if (d != null) {
                            set.add(d);
                        }

                    }
                    return true;
                }
            }
        }
        for (Disposable d : disposables) {
            d.dispose();
        }
        return false;
    }

    public boolean remove(Disposable d) {
        if (delete(d)) {
            d.dispose();
            return true;
        }
        return false;
    }

    public void clear() {
        if (disposed) {
            return;
        }
        HashSet<Disposable> set;
        synchronized (this) {
            if (disposed) {
                return;
            }

            set = resources;
            resources = null;
        }

        dispose(set);
    }

    private boolean delete(Disposable d) {
        if (d == null) {
            return false;
        }
        if (disposed) {
            return false;
        }
        synchronized (this) {
            if (disposed) {
                return false;
            }

            HashSet<Disposable> set = resources;
            if (set == null || !set.remove(d)) {
                return false;
            }
        }
        return true;
    }

    private void dispose(HashSet<Disposable> set) {
        if (set == null) {
            return;
        }
//        List<Throwable> errors = null;
        Object[] array = set.toArray();
        for (Object o : array) {
            if (o instanceof Disposable) {
                try {
                    ((Disposable) o).dispose();
                } catch (Throwable ex) {
//                    if (errors == null) {
//                        errors = new ArrayList<Throwable>();
//                    }
//                    errors.add(ex);
                }
            }
        }
//        if (errors != null) {
//            if (errors.size() == 1) {
//                throw ExceptionHelper.wrapOrThrow(errors.get(0));
//            }
//            throw new CompositeException(errors);
//        }
    }

}


