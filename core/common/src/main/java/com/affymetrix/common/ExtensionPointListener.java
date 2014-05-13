package com.affymetrix.common;

public abstract class ExtensionPointListener<S> {

    public abstract void addService(S s);

    public abstract void removeService(S s);
}
