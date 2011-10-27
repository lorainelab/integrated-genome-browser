package com.affymetrix.common;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public abstract class ExtensionPointHandler<S> {
	public ExtensionPointHandler() {
		super();
	}
	private Class<?> getBaseClass() {
		return getTypeArguments(ExtensionPointHandler.class, getClass()).get(0);
	}
	public String getClassName() {
		return getBaseClass().getName();
	}
	public abstract void addService(S o);
	public abstract void removeService(S o);
	@SuppressWarnings("unchecked")
	public static <Z> void addExtensionPoint(final BundleContext bundleContext, final ExtensionPointHandler<Z> serviceHandler) {
		// register service - an extension point
		try {
			ServiceReference<Z>[] serviceReferences = (ServiceReference<Z>[]) bundleContext.getAllServiceReferences(serviceHandler.getClassName(), null);
			if (serviceReferences != null) {
				for (ServiceReference<Z> serviceReference : serviceReferences) {
					Z extensionPoint = bundleContext.getService(serviceReference);
					ExtensionPointImplHolder<Z> extensionPointImplHolder = (ExtensionPointImplHolder<Z>)ExtensionPointImplHolder.getInstance(serviceHandler.getBaseClass());
					extensionPointImplHolder.addExtensionPointImpl(extensionPoint);
					if (serviceHandler != null) {
						serviceHandler.addService(extensionPoint);
					}
				}
			}
			bundleContext.addServiceListener(
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						ServiceReference<Z> serviceReference = (ServiceReference<Z>) event.getServiceReference();
						Z extensionPoint = bundleContext.getService(serviceReference);
						ExtensionPointImplHolder<Z> extensionPointImplHolder = (ExtensionPointImplHolder<Z>)ExtensionPointImplHolder.getInstance(serviceHandler.getBaseClass());
						if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
							extensionPointImplHolder.removeExtensionPointImpl(extensionPoint);
							if (serviceHandler != null) {
								serviceHandler.removeService(bundleContext.getService(serviceReference));
							}
						}
						if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
							extensionPointImplHolder.addExtensionPointImpl(extensionPoint);
							if (serviceHandler != null) {
								serviceHandler.addService(bundleContext.getService(serviceReference));
							}
						}
					}
				}
			, "(objectClass=" + serviceHandler.getClassName() + ")");
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(ExtensionPointHandler.class.getName()).log(Level.WARNING, "error loading/unloading " + serviceHandler.getClassName(), x.getMessage());
		}
	}

	// Ian Robertson
	// http://www.artima.com/weblogs/viewpost.jsp?thread=208860
	/**
	 * Get the underlying class for a type, or null if the type is a variable type.
	 * @param type the type
	 * @return the underlying class
	 */
	@SuppressWarnings("rawtypes")
	public static Class<?> getClass(Type type) {
	    if (type instanceof Class) {
	      return (Class) type;
	    }
	    else if (type instanceof ParameterizedType) {
	      return getClass(((ParameterizedType) type).getRawType());
	    }
	    else if (type instanceof GenericArrayType) {
	      Type componentType = ((GenericArrayType) type).getGenericComponentType();
	      Class<?> componentClass = getClass(componentType);
	      if (componentClass != null ) {
	        return Array.newInstance(componentClass, 0).getClass();
	      }
	      else {
	        return null;
	      }
	    }
	    else {
	      return null;
	    }
	  }

	  /**
	   * Get the actual type arguments a child class has used to extend a generic base class.
	   *
	   * @param baseClass the base class
	   * @param childClass the child class
	   * @return a list of the raw classes for the actual type arguments.
	   */
	  @SuppressWarnings("rawtypes")
	  public static <T> List<Class<?>> getTypeArguments(
	    Class<T> baseClass, Class<? extends T> childClass) {
	    Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
	    Type type = childClass;
	    // start walking up the inheritance hierarchy until we hit baseClass
	    while (! getClass(type).equals(baseClass)) {
	      if (type instanceof Class) {
	        // there is no useful information for us in raw types, so just keep going.
	        type = ((Class) type).getGenericSuperclass();
	      }
	      else {
	        ParameterizedType parameterizedType = (ParameterizedType) type;
	        Class<?> rawType = (Class) parameterizedType.getRawType();
	  
	        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
	        TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
	        for (int i = 0; i < actualTypeArguments.length; i++) {
	          resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
	        }
	  
	        if (!rawType.equals(baseClass)) {
	          type = rawType.getGenericSuperclass();
	        }
	      }
	    }
	  
	    // finally, for each actual type argument provided to baseClass, determine (if possible)
	    // the raw class for that type argument.
	    Type[] actualTypeArguments;
	    if (type instanceof Class) {
	      actualTypeArguments = ((Class) type).getTypeParameters();
	    }
	    else {
	      actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
	    }
	    List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
	    // resolve types by chasing down type variables.
	    for (Type baseType: actualTypeArguments) {
	      while (resolvedTypes.containsKey(baseType)) {
	        baseType = resolvedTypes.get(baseType);
	      }
	      typeArgumentsAsClasses.add(getClass(baseType));
	    }
	    return typeArgumentsAsClasses;
	  }
}
