package com.affymetrix.genometryImpl.color;

/**
 *
 * @author hiralv
 */
@SuppressWarnings("unchecked")
public class Parameter<E> {
	private final E default_value;
	private E e;

	public Parameter(Object default_value){
		this.e = (E)default_value;
		this.default_value = (E)default_value;
	}
	
	public E get(){
		return e;
	}
	
	public void set(Object e){
		this.e = (E)e;
	}
	
	public void reset(){
		e = default_value;
	}
}
